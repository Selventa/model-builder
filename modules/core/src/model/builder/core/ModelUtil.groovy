package model.builder.core

import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import wslite.json.JSONArray

import static model.builder.core.Util.addMetadata
import static model.builder.core.Util.createColumn
import static org.cytoscape.model.CyNetwork.*
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION

class ModelUtil {

    static CyNetworkView from(Map revision, int number, Expando cyRef) {
        def network = revision.network

        CyNetwork cyN = cyRef.cyNetworkFactory.createNetwork()
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        createColumn(locals, 'who', String.class, true, null)
        createColumn(locals, 'when', String.class, true, null)
        createColumn(locals, 'comment', String.class, true, null)
        cyN.getRow(cyN, LOCAL_ATTRS).set(NAME, "${network.name} (Revision $number)" as String)
        cyN.getRow(cyN, LOCAL_ATTRS).set('who', revision.who)
        cyN.getRow(cyN, LOCAL_ATTRS).set('when', revision.when)
        cyN.getRow(cyN, LOCAL_ATTRS).set('comment', revision.comment)
        addMetadata(network.metadata as Map, cyN, locals)

        def Map<Integer, CyNode> nodes = [:]
        network.nodes.collect { JSONArray node ->
            def (Integer id, label) = node
            CyNode cyNode = nodes[id] ?: (nodes[id] = cyN.addNode())
            cyN.getRow(cyNode).set(NAME, label)
            if (node[4]) {
                addMetadata(node[4].metadata as Map, cyN, locals)
            }
            cyNode
        }
        network.edges.collect { JSONArray edge ->
            def (src, tgt, rel, meta, evidence) = edge
            CyNode cySource = nodes[src] ?: (nodes[src] = cyN.addNode())
            CyNode cyTarget = nodes[tgt] ?: (nodes[tgt] = cyN.addNode())
            def cyEdge = cyN.addEdge(cySource, cyTarget, true)
            cyN.getRow(cyEdge).set('interaction', rel)
            if (meta) {
                addMetadata(meta.metadata as Map, cyN, locals)
            }
            cyEdge
        }

        def cyNv = cyRef.cyNetworkViewFactory.createNetworkView(cyN)
        network.nodes.each { JSONArray node ->
            def (Integer id, _, x, y) = node
            CyNode cyNode = nodes[id]
            cyNv.getNodeView(cyNode).setVisualProperty(NODE_X_LOCATION, x as Double)
            cyNv.getNodeView(cyNode).setVisualProperty(NODE_Y_LOCATION, y as Double)
        }
        cyNv
    }
}
