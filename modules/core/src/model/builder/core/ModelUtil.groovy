package model.builder.core

import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView
import wslite.json.JSONObject

import static model.builder.core.Util.*
import static org.cytoscape.model.CyNetwork.*
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION

class ModelUtil {

    static def MODEL_FIELDS = ['name', 'description', 'species', 'reference_node']
    static def NETWORK_INELIGIBLE_FIELDS = ['SUID', 'shared name', 'selected',
                                            'uri', 'who', 'when', 'comment']
    static def NODE_INELIGIBLE_FIELDS = ['SUID', 'name', 'shared name', 'selected',
                                         'kam.id', 'bel.function', 'linked']
    static def EDGE_INELIGIBLE_FIELDS = ['SUID', 'name', 'shared name', 'selected',
                                         'interaction', 'shared interaction', 'kam.id']

    static CyNetworkView from(String uri, Map revision, Expando cyRef) {
        Map network = revision.network

        CyNetwork cyN = cyRef.cyNetworkFactory.createNetwork()
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        createColumn(locals, 'who', String.class, true, null)
        createColumn(locals, 'when', String.class, true, null)
        createColumn(locals, 'comment', String.class, true, null)
        createColumn(locals, 'uri', String.class, true, null)
        cyN.getRow(cyN, LOCAL_ATTRS).set(NAME, network.name)
        cyN.getRow(cyN, LOCAL_ATTRS).set('who', revision.who)
        cyN.getRow(cyN, LOCAL_ATTRS).set('when', revision.when)
        cyN.getRow(cyN, LOCAL_ATTRS).set('comment', revision.comment)
        cyN.getRow(cyN, LOCAL_ATTRS).set('uri', uri)

        network.subMap(MODEL_FIELDS).each {
            addData(it.key as String, it.value, cyN, cyN, locals)
        }

        if (network.metadata && "$network.metadata" != 'null')
            setMetadata([network.metadata].findAll(), [cyN], locals)

        def Map<Integer, CyNode> index = [:]
        def cyNodes = network.nodes.collect { node ->
            CyNode cyNode = index[node.id] ?: (index[node.id] = cyN.addNode())
            cyN.getRow(cyNode).set(NAME, node.label)
            cyNode
        }
        locals = cyN.getTable(CyNode.class, LOCAL_ATTRS)
        setMetadata(network.nodes.collect {it.metadata}.findAll(), cyNodes, locals)

        def cyEdges = network.edges.collect { edge ->
            CyNode cySource = index[edge.src] ?: (index[edge.src] = cyN.addNode())
            CyNode cyTarget = index[edge.tgt] ?: (index[edge.tgt] = cyN.addNode())
            def cyEdge = cyN.addEdge(cySource, cyTarget, true)
            cyN.getRow(cyEdge).set('interaction', edge.rel)
            cyEdge
        }
        locals = cyN.getTable(CyEdge.class, LOCAL_ATTRS)
        setMetadata(network.edges.collect {it.metadata}.findAll(), cyEdges, locals)

        def cyNv = cyRef.cyNetworkViewFactory.createNetworkView(cyN)
        network.nodes.findAll {
            it.xloc && it.yloc
        }.each { node ->
            CyNode cyNode = index[node.id]
            cyNv.getNodeView(cyNode).setVisualProperty(NODE_X_LOCATION,
                                                       node.xloc as Double)
            cyNv.getNodeView(cyNode).setVisualProperty(NODE_Y_LOCATION,
                                                       node.yloc as Double)
        }
        cyNv
    }

    static Map from(CyNetworkView cyNv) {
        def cyN = cyNv.model

        // convert nodes
        def counter = 0
        def Map<CyNode, Integer> index = [:]
        def nodes = cyN.nodeList.collect { cyNode ->
            def node = [
                id: counter,
                label: cyN.getRow(cyNode).get(NAME, String.class),
                xloc: cyNv.getNodeView(cyNode).getVisualProperty(NODE_X_LOCATION),
                yloc: cyNv.getNodeView(cyNode).getVisualProperty(NODE_Y_LOCATION)
            ]

            def locals = cyN.getTable(CyNode.class, LOCAL_ATTRS)
            def metadata = rowData(cyNode, cyN, locals).collectEntries { k, v ->
                [k, v == null ? JSONObject.NULL : v]
            }
            NODE_INELIGIBLE_FIELDS.each(metadata.&remove)
            metadata.findAll {'.' in it.key}.each(metadata.&remove)
            if (metadata)
                node.metadata = metadata

            index[cyNode] = counter++
            node
        }

        // convert edges
        def edges = cyN.edgeList.collect { cyEdge ->
            def edge = [
                src: index[cyEdge.source],
                tgt: index[cyEdge.target],
                rel: cyN.getRow(cyEdge).get(INTERACTION, String.class),
            ]

            def locals = cyN.getTable(CyEdge.class, LOCAL_ATTRS)
            def metadata = rowData(cyEdge, cyN, locals).collectEntries { k, v ->
                [k, v == null ? JSONObject.NULL : v]
            }
            EDGE_INELIGIBLE_FIELDS.each(metadata.&remove)
            metadata.findAll {'.' in it.key}.each(metadata.&remove)
            if (metadata)
                edge.metadata = metadata
            edge
        }

        def result = [nodes: nodes, edges: edges]
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        def networkData = rowData(cyN, cyN, locals).collectEntries { k, v ->
            [k, v == null ? JSONObject.NULL : v]
        }
        NETWORK_INELIGIBLE_FIELDS.each(networkData.&remove)
        def fields = networkData.subMap(MODEL_FIELDS)
        result += fields
        result.metadata = networkData - fields
        result
    }
}
