package model.builder.core

import wslite.json.JSONArray

import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.*
import groovy.transform.TupleConstructor
import model.builder.web.api.API
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNode

import org.cytoscape.model.CyNetwork
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class CreateCyNetwork extends AbstractTask {

    final Map model
    final API api
    final Expando cyRef

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        monitor.title = "Creating networks for ${model.name}"
        monitor.progress = 0.0d
        monitor.statusMessage = "Retrieving ${model.name}"

        /*

        [
    {
        "revision": {
            "when": "2013-05-17T20:41:14Z",
            "comment": "new model; set to homo sapiens",
            "network": {
                "species": 9606,
                "edges": [
                    [
                        "p(HGNC:CPB2)",
                        "actsIn",
                        "cat(p(HGNC:CPB2))",
                        [
                            69,
                            167
                        ],
                        [
                            185,
                            248
                        ],
                        {
                            "statements": [

                            ]
                        }
                    ]
                ],
                "name": "Network",
                "_created_at": "2013-05-17T20:41:14Z",
                "metadata": {
                    "__layoutAlgorithm": "force-directed"
                }
            },
            "who": "test@sdpdemo.selventa.com"
        }
    }
]

         */
        WebResponse rev = api.modelRevisions(model.id, 'latest').first()
        def revObj = rev.data.revision
        def network = revObj.network

        monitor.progress = 1.0d
        CyNetwork cyN = cyRef.cyNetworkFactory.createNetwork()
        cyN.defaultNetworkTable.getColumn('who') ?:
            cyN.defaultNetworkTable.createColumn('who', String.class, true)
        cyN.defaultNetworkTable.getColumn('when') ?:
            cyN.defaultNetworkTable.createColumn('when', String.class, true)
        cyN.defaultNetworkTable.getColumn('comment') ?:
            cyN.defaultNetworkTable.createColumn('comment', String.class, true)
        cyN.getRow(cyN).set(NAME, network.name)
        cyN.getRow(cyN).set('who', revObj.who)
        cyN.getRow(cyN).set('when', revObj.when)
        cyN.getRow(cyN).set('comment', revObj.comment)

        def Map<String, CyNode> nodes = [:]
        def edgeWithXY = network.edges.collect { JSONArray edge ->
            def (src, rel, tgt) = edge
            CyNode cySrc = nodes[src] ?: (nodes[src] = cyN.addNode())
            cyN.getRow(cySrc).set(NAME, src)
            CyNode cyTgt = nodes[tgt] ?: (nodes[tgt] = cyN.addNode())
            cyN.getRow(cyTgt).set(NAME, tgt)
            nodes[edge] = [cySrc, cyTgt]
            [cyN.addEdge(cySrc, cyTgt, true), edge[3], edge[4]]
        }
        def view = cyRef.cyNetworkViewFactory.createNetworkView(cyN)
        edgeWithXY.each {
            def (cyEdge, srcXY, tgtXY) = it
            view.getNodeView(cyEdge.source).setVisualProperty(NODE_X_LOCATION, srcXY.getInt(0))
            view.getNodeView(cyEdge.source).setVisualProperty(NODE_Y_LOCATION, srcXY.getInt(1))
            view.getNodeView(cyEdge.target).setVisualProperty(NODE_X_LOCATION, tgtXY.getInt(0))
            view.getNodeView(cyEdge.target).setVisualProperty(NODE_Y_LOCATION, tgtXY.getInt(1))
        }
        cyRef.cyNetworkManager.addNetwork(cyN)
        cyRef.cyNetworkViewManager.addNetworkView(view)
        cyRef.cyApplicationManager.currentNetwork = cyN
        cyRef.cyApplicationManager.currentNetworkView = view
        view.updateView()
        view.fitContent()
    }
}
