package model.builder.core

import model.builder.web.api.API
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNetworkTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import wslite.json.JSONArray

import static model.builder.core.Util.createColumn
import static org.cytoscape.model.CyNetwork.*
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_X_LOCATION
import static org.cytoscape.view.presentation.property.BasicVisualLexicon.NODE_Y_LOCATION

class CreateCyNetworkForModelRevisionTunable extends AbstractNetworkTask {

    final API api
    final Expando cyRef

    // tunable state
    private Expando modelRev
    private ListSingleSelection<Expando> revSelection

    CreateCyNetworkForModelRevisionTunable(CyNetwork cyN, API api, Expando cyRef) {
        super(cyN)
        this.api = api
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
