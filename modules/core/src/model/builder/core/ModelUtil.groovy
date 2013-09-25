package model.builder.core

import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import wslite.json.JSONArray

import static model.builder.core.Util.addMetadata
import static model.builder.core.Util.createColumn
import static org.cytoscape.model.CyNetwork.*
import static org.cytoscape.model.CyEdge.INTERACTION
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
        createColumn(locals, 'revision_number', Integer.class, true, null)
        cyN.getRow(cyN, LOCAL_ATTRS).set(NAME, network.name)
        cyN.getRow(cyN, LOCAL_ATTRS).set('who', revision.who)
        cyN.getRow(cyN, LOCAL_ATTRS).set('when', revision.when)
        cyN.getRow(cyN, LOCAL_ATTRS).set('comment', revision.comment)
        cyN.getRow(cyN, LOCAL_ATTRS).set('revision_number', number)
        addMetadata(network.metadata as Map, cyN, locals)

        def Map<Integer, CyNode> index = [:]
        network.nodes.collect { JSONArray node ->
            def (Integer id, label) = node
            CyNode cyNode = index[id] ?: (index[id] = cyN.addNode())
            cyN.getRow(cyNode).set(NAME, label)
            if (node[4]) addMetadata(node[4].metadata as Map, cyN, locals)
            cyNode
        }
        network.edges.collect { JSONArray edge ->
            def (src, tgt, rel, meta, evidence) = edge
            CyNode cySource = index[src] ?: (index[src] = cyN.addNode())
            CyNode cyTarget = index[tgt] ?: (index[tgt] = cyN.addNode())
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
            CyNode cyNode = index[id]
            cyNv.getNodeView(cyNode).setVisualProperty(NODE_X_LOCATION, x as Double)
            cyNv.getNodeView(cyNode).setVisualProperty(NODE_Y_LOCATION, y as Double)
        }
        cyNv
    }

    static Map from(CyNetworkView cyNv) {
        def cyN = cyNv.model
        def cyNr = cyN.getRow(cyN)

        // TODO convert metadata

        // convert nodes
        def counter = 0
        def Map<CyNode, Integer> index = [:]
        def nodes = cyN.nodeList.collect { cyNode ->
            def name = cyN.getRow(cyNode).get(NAME, String.class)
            Double x = cyNv.getNodeView(cyNode).getVisualProperty(NODE_X_LOCATION)
            Double y = cyNv.getNodeView(cyNode).getVisualProperty(NODE_Y_LOCATION)
            index[cyNode] = counter
            [counter++, name, x as Integer, y as Integer, null]
        }

        // convert edges
        def edges = cyN.edgeList.collect { cyEdge ->
            def rel = cyN.getRow(cyEdge).get(INTERACTION, String.class)
            [index[cyEdge.source], index[cyEdge.target], rel, null, null]
        }

        [
            network: [
                name: cyNr.get(NAME, String.class),
                description: cyNr.get('description', String.class, null),
                species: cyNr.get('species', String.class, null),
                reference_node: cyNr.get('reference_node', String.class, null),
                metadata: null,
                nodes: nodes,
                edges: edges
            ]
        ]
    }
}
