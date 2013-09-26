package model.builder.core

import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.task.AbstractNetworkViewTask
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable

import static model.builder.core.ModelUtil.from

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
        def uri = cyN.getRow(cyN).get('uri', String.class)
        def id = api.id([uri: uri -= ~/\/revisions\/\d+/])

        // find next available revision
        WebResponse res = api.model(id)
        def model = res.data.model
        def next = (model.revisions.length() - 1 as Integer) + 1
        uri = "$uri/revisions/$next"
        def path = new URI(uri).path

        // save current network/summary to revision
        res = api.putModelRevision(path, network, summary)
        res.statusCode
        res.statusMessage
    }
}
