package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import wslite.json.JSONArray

import static model.builder.core.Util.createColumn
import static org.cytoscape.model.CyNetwork.*
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION

@TupleConstructor
class CreateCyNetworkForModelRevision extends AbstractTask {

    final int number
    final Map revision
    final Expando cyRef

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def network = revision.network

        monitor.title = "Creating network for ${network.name} / $number"
        monitor.progress = 0.0d
        monitor.statusMessage = 'Adding network'

        CyNetwork cyN = cyRef.cyNetworkFactory.createNetwork()
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        createColumn(locals, 'who', String.class, true, null)
        createColumn(locals, 'when', String.class, true, null)
        createColumn(locals, 'comment', String.class, true, null)
        cyN.getRow(cyN, LOCAL_ATTRS).set(NAME, "${network.name} (Revision $number)" as String)
        cyN.getRow(cyN, LOCAL_ATTRS).set('who', revision.who)
        cyN.getRow(cyN, LOCAL_ATTRS).set('when', revision.when)
        cyN.getRow(cyN, LOCAL_ATTRS).set('comment', revision.comment)

        def Map<String, CyNode> nodes = [:]
        def edgeWithXY = network.edges.collect { JSONArray edge ->
            def (src, rel, tgt) = edge
            CyNode cySrc = nodes[src] ?: (nodes[src] = cyN.addNode())
            cyN.getRow(cySrc).set(NAME, src)
            CyNode cyTgt = nodes[tgt] ?: (nodes[tgt] = cyN.addNode())
            cyN.getRow(cyTgt).set(NAME, tgt)
            nodes[edge] = [cySrc, cyTgt]
            def cyE = cyN.addEdge(cySrc, cyTgt, true)
            cyN.getRow(cyE).set('interaction', rel)
            [cyE, edge[3], edge[4]]
        }
        monitor.progress = 0.5d

        def view = cyRef.cyNetworkViewFactory.createNetworkView(cyN)
        edgeWithXY.each {
            def (cyEdge, srcXY, tgtXY) = it
            view.getNodeView(cyEdge.source).setVisualProperty(NODE_X_LOCATION, srcXY.getDouble(0))
            view.getNodeView(cyEdge.source).setVisualProperty(NODE_Y_LOCATION, srcXY.getDouble(1))
            view.getNodeView(cyEdge.target).setVisualProperty(NODE_X_LOCATION, tgtXY.getDouble(0))
            view.getNodeView(cyEdge.target).setVisualProperty(NODE_Y_LOCATION, tgtXY.getDouble(1))
        }

        monitor.statusMessage = 'Creating view'
        cyRef.cyNetworkManager.addNetwork(cyN)
        cyRef.cyNetworkViewManager.addNetworkView(view)
        cyRef.cyApplicationManager.currentNetwork = cyN
        cyRef.cyApplicationManager.currentNetworkView = view
        view.updateView()
        view.fitContent()
        monitor.progress = 1.0d
    }
}
