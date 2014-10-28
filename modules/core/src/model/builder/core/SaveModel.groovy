package model.builder.core

import groovy.swing.SwingBuilder
import model.builder.ui.MessagePopups
import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.AbstractNetworkViewTask
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import javax.swing.*
import java.util.regex.Pattern

import static Util.createColumn
import static model.builder.core.ModelUtil.*
import static model.builder.ui.MessagePopups.*
import static org.cytoscape.model.CyNetwork.LOCAL_ATTRS

class SaveModel extends AbstractNetworkViewTask {

    private static final Pattern revisionPattern = ~/\/revisions\/(\d+)/
    private static final Pattern uriCheckPattern = ~/\/models\/\w+\/revisions\/\d+/
    private static final Logger msg = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)

    // accessed by cytoscape
    @Tunable(description="Revision comment")
    public String summary = ''

    private final Expando cyRef
    private final APIManager apiManager

    SaveModel(CyNetworkView view, Expando cyRef, APIManager apiManager) {
        super(view)
        this.cyRef = cyRef
        this.apiManager = apiManager
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        AuthorizedAPI api = apiManager.byAccess(apiManager.default)
        if (!api) {
            errorAccessNotSet()
            return
        }
        Map network = fromView(view)

        monitor.title = "Saving model to SDP"
        monitor.statusMessage = "Saving new revision for \"${network.name}\"."

        // get current model from revision
        def cyN = view.model

        if (!cyN.getRow(cyN).isSet('uri')) {
            def uri = newModel(api, summary, network)
            createColumn(cyN.getTable(CyNetwork.class, LOCAL_ATTRS), 'uri',
                    String.class, true, '')
            successMessage("The new model, \"${network.name}\", was " +
                    "successfully saved (${apiManager.default.host}).")
            cyN.getRow(cyN).set('uri', uri)
        } else {
            def uri = cyN.getRow(cyN).get('uri', String.class)
            def (ret, code, str) = newRevision(api, uri, summary, network)

            if (ret) {
                cyN.getRow(cyN).set('uri', str)

                def (success, latestRevision) = latestRevision(api, str)
                if (!success) {
                    errorMessage("Latest revision could not be retrieved.")
                } else {
                    addModelRevisionColumns(cyN)
                    cyN.getRow(cyN).set('who', latestRevision.who)
                    cyN.getRow(cyN).set('comment', latestRevision.comment)
                    cyN.getRow(cyN).set('when', latestRevision.when)
                    successMessage("A new revision of the \"${network.name}\" model " +
                            "was successfully saved (${apiManager.default.host}).")
                }
            } else {
                if (code == 409) {
                    def curRevNumber = revisionNumber(uri)
                    def (success, latest) = latestRevision(api, uri)
                    if (!success) {
                        msg.error("Unable to retrieve latest revision for conflict resolution.")
                    } else {
                        // invoke later let's the Cytoscape task finish
                        new SwingBuilder().doLater {
                            ret = MessagePopups.modelConflict(network.name, curRevNumber, latest.who, latest.comment, latest.when)
                            if (ret == JOptionPane.YES_OPTION) {
                                CyNetworkView cyNv = fromRevision(latest.uri, latest, cyRef)

                                def kamVs = cyRef.visualMappingManager.allVisualStyles.find {
                                    it.title == 'KAM Visualization'
                                }

                                cyRef.cyNetworkManager.addNetwork(cyNv.model)
                                cyRef.cyNetworkViewManager.addNetworkView(cyNv)
                                cyRef.cyApplicationManager.currentNetwork = cyNv.model
                                cyRef.cyApplicationManager.currentNetworkView = cyNv
                                cyRef.visualMappingManager.setVisualStyle(kamVs, cyNv)
                                kamVs.apply(cyNv)
                                cyNv.updateView()
                                cyNv.fitContent()
                            }
                        }
                    }
                } else {
                    errorMessage(str)
                }
            }
        }
    }

    private static String newModel(AuthorizedAPI api, String comment,
                                 Map network) {
        WebResponse res = api.postModel(comment, network)
        String uri = res.headers['Location']
        res = api.model(api.id([uri: uri]))
        def model = res.data
        def createdRevision = (model.revisions.length() - 1 as Integer)
        "$uri/revisions/$createdRevision"
    }

    private static List newRevision(AuthorizedAPI api, String revisionURI,
                                    String comment, Map network) {
        // uri sanity check
        if (! (revisionURI =~ uriCheckPattern).find()) {
            return [false, null, "Revision URI does not identify a model (network.uri column)."]
        }

        if (!modelExists(api, revisionURI)) {
            return [false, null, "Model \"${network.name}\" was not found on SDP (404 Not Found)."]
        }

        def rev = revisionNumber(revisionURI)

        def nextURI = revisionURI.replaceAll(revisionPattern, "/revisions/${rev + 1}")
        def path = new URI(nextURI).path

        WebResponse res = api.putModelRevision(path, network, comment)
        switch(res.statusCode) {
            case 200:
                return [true, res.statusCode, nextURI]
            case 400:
                return [false, res.statusCode, "Update for model \"${network.name}\" was invalid (${res.statusCode} ${res.statusMessage})."]
            case 409:
                return [false, res.statusCode, "Update for model \"${network.name}\" is out of date. A newer revision already exists on the SDP (${res.statusCode} ${res.statusMessage})."]
            default:
                return [false, res.statusCode, "Failure to save update for model \"${network.name}\" (${res.statusCode} ${res.statusMessage})."]
        }
    }

    private static boolean modelExists(AuthorizedAPI api, String revisionURI) {
        def modelId = api.id([uri: revisionURI - revisionPattern])
        WebResponse res = api.model(modelId)
        res.statusCode == 200
    }

    private static List latestRevision(AuthorizedAPI api, String revisionURI) {
        def modelURI = revisionURI - revisionPattern
        def modelId = api.id([uri: modelURI])
        WebResponse res = api.model(modelId)

        if (res.statusCode != 200) return [false, res.statusCode]

        def model = res.data
        def rev = model.revisions.length() - 1 as Integer
        res = api.modelRevisions(modelId, rev, '').first()

        if (res.statusCode != 200) return [false, res.statusCode]

        def revisionMap = res.data.revision as Map
        revisionMap.uri = "$modelURI/revisions/$rev"

        [true, revisionMap]
    }

    private static def revisionNumber(String revisionURI) {
        def matcher = (revisionURI =~ revisionPattern)
        if (!matcher.find()) return null
        Integer.parseInt(matcher[0][1])
    }
}
