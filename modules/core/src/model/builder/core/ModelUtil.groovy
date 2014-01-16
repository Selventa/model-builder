package model.builder.core

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.cytoscape.model.CyEdge
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.view.model.CyNetworkView

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
                                         'interaction', 'shared interaction', 'kam.id',
                                         'evidence', 'linked']

    /**
     * Validates model json format and returns a {@link Tuple} two of Boolean and
     * List.
     *
     * @param network {@link Map}
     * @return {@link Tuple} two; first position is format validation {@link Boolean};
     * second position is format errors {@link List}
     */
    static Tuple validateJsonFormat(Map network) {
        List errors = []
        if (!('name' in network)) errors << "Missing 'name' field"
        if (!('nodes' in network)) errors << "Missing 'nodes' field"
        if (!('edges' in network)) errors << "Missing 'edges' field"

        if (network.nodes) {
            network.nodes.eachWithIndex{ Map node, int idx ->
                if (!(node.containsKey('id'))) errors << "Node $idx is missing an 'id' field"
                if (!(node.containsKey('label'))) errors << "Node $idx is missing a 'label' field"
            }
        }
        if (network.edges) {
            network.edges.eachWithIndex{ Map edge, int idx ->
                if (!(edge.containsKey('src'))) errors << "Edge $idx is missing a 'src' field"
                if (!(edge.containsKey('rel'))) errors << "Edge $idx is missing a 'rel' field"
                if (!(edge.containsKey('tgt'))) errors << "Edge $idx is missing a 'tgt' field"
            }
        }

        new Tuple((errors ? false : true), errors)
    }

    static CyNetworkView fromNetwork(Map network, Expando cyr) {
        def (cyN, index) = makeNetwork(network, cyr)
        makeNetworkView(network, cyN, index, cyr)
    }

    static CyNetworkView fromRevision(String uri, Map revision, Expando cyr) {
        def cyNv = fromNetwork(revision.network, cyr)
        def cyN = cyNv.model

        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        createColumn(locals, 'who', String.class, true, null)
        createColumn(locals, 'when', String.class, true, null)
        createColumn(locals, 'comment', String.class, true, null)
        createColumn(locals, 'uri', String.class, true, null)
        cyN.getRow(cyN, LOCAL_ATTRS).set('who', revision.who)
        cyN.getRow(cyN, LOCAL_ATTRS).set('when', revision.when)
        cyN.getRow(cyN, LOCAL_ATTRS).set('comment', revision.comment)
        cyN.getRow(cyN, LOCAL_ATTRS).set('uri', uri)
        cyNv
    }

    static Map fromView(CyNetwork cyN) {
        // convert nodes
        def counter = 0
        def Map<CyNode, Integer> index = [:]
        def nodes = cyN.nodeList.collect { cyNode ->
            def node = [
                    id: counter,
                    label: cyN.getRow(cyNode).get(NAME, String.class)
            ]

            def locals = cyN.getTable(CyNode.class, LOCAL_ATTRS)
            def metadata = scrubMetadata(rowData(cyNode, cyN, locals), NODE_INELIGIBLE_FIELDS)
            if (metadata) node.metadata = metadata

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
            def metadata = scrubMetadata(rowData(cyEdge, cyN, locals), EDGE_INELIGIBLE_FIELDS)
            if (metadata) edge.metadata = metadata

            if (cyN.getRow(cyEdge).isSet('evidence')) {
                def evTxt = cyN.getRow(cyEdge).get('evidence', String.class)
                def ev = new JsonSlurper().parseText(evTxt)
                if (ev) edge.evidence = ev
            }

            edge
        }

        def result = [nodes: nodes, edges: edges]
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        def networkData = scrubMetadata(rowData(cyN, cyN, locals), NETWORK_INELIGIBLE_FIELDS)
        def fields = networkData.subMap(MODEL_FIELDS)
        result += fields
        result.metadata = networkData - fields
        result
    }

    static Map fromView(CyNetworkView cyNv) {
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
            def metadata = scrubMetadata(rowData(cyNode, cyN, locals), NODE_INELIGIBLE_FIELDS)
            if (metadata) node.metadata = metadata

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
            def metadata = scrubMetadata(rowData(cyEdge, cyN, locals), EDGE_INELIGIBLE_FIELDS)
            if (metadata) edge.metadata = metadata

            if (cyN.getRow(cyEdge).isSet('evidence')) {
                def evTxt = cyN.getRow(cyEdge).get('evidence', String.class)
                def ev = new JsonSlurper().parseText(evTxt)
                if (ev) edge.evidence = ev
            }

            edge
        }

        def result = [nodes: nodes, edges: edges]
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        def networkData = scrubMetadata(rowData(cyN, cyN, locals), NETWORK_INELIGIBLE_FIELDS)
        def fields = networkData.subMap(MODEL_FIELDS)
        result += fields
        result.metadata = networkData - fields
        result
    }

    private static Map scrubMetadata(Map metadata, List ineligibleFields) {
        Map scrubbed = [:] + metadata
        ineligibleFields.each(scrubbed.&remove)
        scrubbed.findAll {!it.key?.contains('.')}
    }

    private static Tuple makeNetwork(Map network, Expando cyr) {
        CyNetwork cyN = cyr.cyNetworkFactory.createNetwork()
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        cyN.getRow(cyN, LOCAL_ATTRS).set(NAME, network.name)

        network.subMap(MODEL_FIELDS).each {
            addData(it.key as String, it.value, cyN, cyN, locals)
        }

        if (network.metadata && "$network.metadata" != 'null')
            setMetadata([network.metadata].findAll(), [cyN], locals)

        // handle nodes
        def Map<Integer, CyNode> index = [:]
        def cyNodes = network.nodes.collect { node ->
            CyNode cyNode = index[node.id] ?: (index[node.id] = cyN.addNode())
            cyN.getRow(cyNode).set(NAME, node.label)
            cyNode
        }
        locals = cyN.getTable(CyNode.class, LOCAL_ATTRS)
        setMetadata(network.nodes.collect {it.metadata}.findAll(), cyNodes, locals)

        // handle edges
        locals = cyN.getTable(CyEdge.class, LOCAL_ATTRS)
        def cyEdges = network.edges.collect { edge ->
            CyNode cySource = index[edge.src] ?: (index[edge.src] = cyN.addNode())
            CyNode cyTarget = index[edge.tgt] ?: (index[edge.tgt] = cyN.addNode())
            def cyEdge = cyN.addEdge(cySource, cyTarget, true)
            cyN.getRow(cyEdge).set('interaction', edge.rel)

            if (edge.evidence) {
                def evTxt = new JsonBuilder(edge.evidence).toString()
                createColumn(locals, 'evidence', String.class, false, null)
                locals.getRow(cyEdge.SUID).set('evidence', evTxt)
            }
            cyEdge
        }
        setMetadata(network.edges.collect {it.metadata}.findAll(), cyEdges, locals)

        new Tuple(cyN, index)
    }

    private static CyNetworkView makeNetworkView(Map network, CyNetwork cyN,
                                         Map<Integer, CyNode> index, Expando cyr) {
        def cyNv = cyr.cyNetworkViewFactory.createNetworkView(cyN)
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
}
