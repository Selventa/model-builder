package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.common.Model
import model.builder.web.api.API
import model.builder.web.api.WebResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.rest.RESTClientException

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

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    final Model model
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

        try {
            api.model(model.id)
        } catch (RESTClientException e) {
            msg.error("Error retrieving ${model.name}")
            throw new RuntimeException('Failed to create network for model', e)
        }
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
