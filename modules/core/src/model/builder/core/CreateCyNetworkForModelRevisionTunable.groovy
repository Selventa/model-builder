package model.builder.core

import model.builder.web.api.APIManager
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNetworkTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.json.JSONArray

import static model.builder.core.Util.createColumn
import static org.cytoscape.model.CyNetwork.*
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION

class CreateCyNetworkForModelRevisionTunable extends AbstractNetworkTask {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    final APIManager apiManager
    final Expando cyRef

    // tunable state
    private Expando modelRev
    private ListSingleSelection<Expando> revSelection

    CreateCyNetworkForModelRevisionTunable(CyNetwork cyN, APIManager apiManager, Expando cyRef) {
        super(cyN)
        this.apiManager = apiManager
        this.cyRef = cyRef
    }

    // Called by cytoscape
    @Tunable(description = "Revision")
    public ListSingleSelection<Expando> getRevSelection() {
        def mgr = cyRef.cyTableManager
        def revTable = mgr.globalTables.find {it.title == 'SDP.Revisions'}
        if (!revTable)
            throw new IllegalStateException('table SDP.Revisions does not exist')
        List<Expando> modelRevs = revTable.getAllRows().
        findAll {
            it.getList('networks.SUID', Long.class, []).contains(network.SUID)
        }.
        collect {
            new Expando(
                    uri: it.get('uri', String.class),
                    revision: it.get('revision', Integer.class),
                    when: it.get('when', String.class),
                    comment: it.get('comment', String.class),
                    toString: {
                        "Revision $revision ($when): $comment"
                    })
        }.sort { it.toString() }
        revSelection = revSelection ?: new ListSingleSelection<Expando>(modelRevs)
    }

    // Called by cytoscape
    public void setRevSelection(ListSingleSelection<Expando> sel) {
        this.modelRev = sel.selectedValue
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void run(TaskMonitor monitor) throws Exception {
        def host = new URI(modelRev.uri as String).host
        def api = apiManager.authorizedAPI(host)
        if (!api) {
            msg.error("Missing SDP Configuration for ${host}.  ")
            throw new IllegalStateException("Missing SDP Configuration for ${host}.  " +
                                            "Please configure SDP access (Apps -> SDP -> Configuration).")
        }

        WebResponse res = api.modelRevisions(null, null, modelRev.uri).first()
        def revision = res.data.revision
        def network = revision.network

        monitor.title = "Creating network for ${network.name} / ${modelRev.revision}"
        monitor.progress = 0.0d
        monitor.statusMessage = 'Adding network'

        CyNetwork cyN = cyRef.cyNetworkFactory.createNetwork()
        def locals = cyN.getTable(CyNetwork.class, LOCAL_ATTRS)
        createColumn(locals, 'who', String.class, true, null)
        createColumn(locals, 'when', String.class, true, null)
        createColumn(locals, 'comment', String.class, true, null)
        cyN.getRow(cyN, LOCAL_ATTRS).set(NAME, "${network.name} (Revision ${modelRev.revision})" as String)
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
