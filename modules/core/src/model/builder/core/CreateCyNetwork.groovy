package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.API

import static org.cytoscape.model.CyNetwork.NAME
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class CreateCyNetwork extends AbstractTask {

    final Map model
    final API api
    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        monitor.title = "Creating networks for ${model.name}"
        monitor.progress = 0.0d
        monitor.statusMessage = "Retrieving ${model.name}"

        /*
        {
    "model": {
        "species": 9606,
        "name": "Network",
        "links": [
            {
                "rel": "next_revision",
                "uri": "https://sdpdemo.selventa.com/api/models/519695ea42bc1d34b1757f5a/revisions/1"
            }
        ],
        "_created_at": "2013-05-17T20:41:14Z",
        "revisions": [
            {
                "when": "2013-05-17T20:41:14Z",
                "comment": "new model; set to homo sapiens",
                "network": "https://sdpdemo.selventa.com/api/models/519695ea42bc1d34b1757f5a/revisions/0",
                "who": "test@sdpdemo.selventa.com"
            }
        ],
        "uri": "https://sdpdemo.selventa.com/api/models/519695ea42bc1d34b1757f5a"
    }
}
         */

        monitor.progress = 1.0d
        CyNetwork network = cynFac.createNetwork()
        network.getRow(network).set(NAME, this.model.name)
        cynMgr.addNetwork(network)
        def view = cynvFac.createNetworkView(network)
        cynvMgr.addNetworkView(view)
        appMgr.currentNetwork = network
        appMgr.currentNetworkView = view
    }
}
