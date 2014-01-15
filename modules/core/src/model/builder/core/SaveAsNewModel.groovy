package model.builder.core

import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.AbstractNetworkViewTask
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable

import static Util.createColumn
import static model.builder.core.ModelUtil.fromView
import static model.builder.ui.MessagePopups.successMessage
import static org.cytoscape.model.CyNetwork.LOCAL_ATTRS
import static org.cytoscape.model.CyNetwork.NAME

class SaveAsNewModel extends AbstractNetworkViewTask {

    // accessed by cytoscape
    public String newName = null
    public String newDescription = null
    @Tunable(description="Revision summary")
    public String summary = ''

    private final APIManager apiManager

    SaveAsNewModel(CyNetworkView view, APIManager apiManager) {
        super(view)
        this.apiManager = apiManager
    }

    @Tunable(description = 'New network name')
    public String getNewName() {
        def cyN = view.model
        newName ?: cyN.getRow(cyN).get(NAME, String.class)
    }

    // accessed by cytoscape
    public void setNewName(String newName) {
        this.newName = newName
    }

    @Tunable(description = 'New network description')
    public String getNewDescription() {
        def cyN = view.model
        newDescription ?: cyN.getRow(cyN).get('description', String.class)
    }

    // accessed by cytoscape
    public void setNewDescription(String newDescription) {
        this.newDescription = newDescription
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default)

        def cyN = view.model

        // set new name
        newName = newName ?: (cyN.getRow(cyN).get(NAME, String.class))
        cyN.getRow(cyN).set(NAME, newName)

        monitor.title = "Saving new model to SDP"
        monitor.statusMessage = "Saving as new model \"${newName}\"."

        // set new description
        newDescription = newDescription ?: (cyN.getRow(cyN).get('description', String.class))
        cyN.getRow(cyN).set('description', newDescription)

        Map network = fromView(view)
        network.description

        if (cyN.getRow(cyN).isSet('uri'))
            cyN.getRow(cyN).set('uri', null)

        def uri = newModel(api, summary, network)
        createColumn(cyN.getTable(CyNetwork.class, LOCAL_ATTRS), 'uri',
                String.class, true, '')
        successMessage("The new model, \"${newName}\", was " +
                "successfully saved (${apiManager.default.host}).")
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
}
