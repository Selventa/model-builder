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

        def Map<Integer, CyNode> nodes = [:]
        network.nodes.collect { JSONArray node ->
            def (Integer id, label) = node
            CyNode cyNode = nodes[id] ?: (nodes[id] = cyN.addNode())
            cyN.getRow(cyNode).set(NAME, label)
            if (node[4]) {
                // handle metadata
            }
            cyNode
        }
        monitor.progress = 0.5d
        network.edges.collect { JSONArray edge ->
            def (src, tgt, rel, meta, evidence) = edge
            CyNode cySource = nodes[src] ?: (nodes[src] = cyN.addNode())
            CyNode cyTarget = nodes[tgt] ?: (nodes[tgt] = cyN.addNode())
            def cyEdge = cyN.addEdge(cySource, cyTarget, true)
            cyN.getRow(cyEdge).set('interaction', rel)
            cyEdge
        }

        def view = cyRef.cyNetworkViewFactory.createNetworkView(cyN)
        network.nodes.each { JSONArray node ->
            def (Integer id, _, x, y) = node
            CyNode cyNode = nodes[id]
            view.getNodeView(cyNode).setVisualProperty(NODE_X_LOCATION, x as Double)
            view.getNodeView(cyNode).setVisualProperty(NODE_Y_LOCATION, y as Double)
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
