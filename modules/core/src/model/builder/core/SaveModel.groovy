package model.builder.core

import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.AbstractNetworkViewTask
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable

import static model.builder.ui.MessagePopups.successMessage
import static model.builder.core.ModelUtil.from
import static Util.createColumn
import static org.cytoscape.model.CyNetwork.LOCAL_ATTRS

class SaveModel extends AbstractNetworkViewTask {

    // accessed by cytoscape
    @Tunable(description="Revision summary")
    public String summary = ''

    private final APIManager apiManager

    SaveModel(CyNetworkView view, APIManager apiManager) {
        super(view)
        this.apiManager = apiManager
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default)
        Map network = from(view)

        // get current model from revision
        def cyN = view.model

        def uri
        if (!cyN.getRow(cyN).isSet('uri')) {
            uri = newModel(api, summary, network)
            createColumn(cyN.getTable(CyNetwork.class, LOCAL_ATTRS), 'uri',
                    String.class, true, '')
            successMessage("The new model, \"${network.name}\", was " +
                    "successfully saved (${apiManager.default.host}).")
        } else {
            uri = cyN.getRow(cyN).get('uri', String.class)
            def id = api.id([uri: uri - ~/\/revisions\/\d+/])
            uri = newRevision(api, id, summary, network)
            successMessage("A new revision of the \"${network.name}\" model " +
                    "was successfully saved (${apiManager.default.host}).")
        }
        cyN.getRow(cyN).set('uri', uri)
    }

    private static String newModel(AuthorizedAPI api, String comment,
                                 Map network) {
        WebResponse res = api.postModel(comment, network)
        String uri = res.headers['Location']
        res = api.model(api.id([uri: uri]))
        def model = res.data.model
        def createdRevision = (model.revisions.length() - 1 as Integer)
        "$uri/revisions/$createdRevision"
    }

    private static String newRevision(AuthorizedAPI api, String modelId,
                                    String comment, Map network) {
        WebResponse res = api.model(modelId)
        def model = res.data.model
        def nextURI = model.links.find {it.rel == 'next_revision'}.uri
        def path = new URI(nextURI).path
        res = api.putModelRevision(path, network, comment)
        res.statusCode == 200 ? nextURI : null
    }
}
