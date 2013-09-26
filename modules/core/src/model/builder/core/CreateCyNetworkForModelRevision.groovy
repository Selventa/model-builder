package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static ModelUtil.from

@TupleConstructor
class CreateCyNetworkForModelRevision extends AbstractTask {

    final Map context
    final Expando cyRef

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def model = context.model as Map
        def revision = context.revision as Map
        def revisionNumber = context.revisionNumber as int

        def network = revision.network
        monitor.title = "Creating network for ${network.name} / $revisionNumber"
        monitor.progress = 0.0d
        monitor.statusMessage = 'Adding network nodes and edges'

        def uri = "${model.uri}/revisions/$revisionNumber"
        CyNetworkView cyNv = from(uri, revision, cyRef)

        monitor.statusMessage = 'Creating view'
        cyRef.cyNetworkManager.addNetwork(cyNv.model)
        cyRef.cyNetworkViewManager.addNetworkView(cyNv)
        cyRef.cyApplicationManager.currentNetwork = cyNv.model
        cyRef.cyApplicationManager.currentNetworkView = cyNv
        cyNv.updateView()
        cyNv.fitContent()
        monitor.progress = 1.0d
    }
}
