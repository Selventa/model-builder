package model.builder.core

import model.builder.web.api.APIManager
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.AbstractNetworkTask
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static ModelUtil.fromRevision

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
        def revision = res.data.revision as Map
        def network = revision.network

        monitor.title = "Creating network for ${network.name} / ${modelRev.revision}"
        monitor.progress = 0.0d
        monitor.statusMessage = 'Adding network'

        CyNetworkView cyNv = fromRevision(modelRev.uri, revision, cyRef)

        monitor.statusMessage = 'Creating view'
        cyRef.cyNetworkManager.addNetwork(cyNv.model)
        cyRef.cyNetworkViewManager.addNetworkView(cyNv)
        cyRef.cyApplicationManager.currentNetwork = cyNv.model
        cyRef.cyApplicationManager.currentNetworkView = cyNv
        cyNv.updateView()
        cyNv.fitContent()
        monitor.progress = 1.0d
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
}
