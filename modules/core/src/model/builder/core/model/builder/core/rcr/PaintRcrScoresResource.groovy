package model.builder.core.model.builder.core.rcr

import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import static model.builder.core.Tunables.tunableNetworkCategory
import static model.builder.core.model.builder.core.rcr.MechanismPaintField.fromField
import static model.builder.core.model.builder.core.rcr.Tunables.tunableMechanismPaintField
import static model.builder.core.Activator.CY
import static model.builder.core.model.builder.core.rcr.Constant.SDP_RCR_SCORES_TABLE
import static org.cytoscape.model.CyNetwork.NAME

class PaintRcrScoresResource extends AbstractTask {

    private ListSingleSelection<Expando> tNetwork
    private ListSingleSelection<String> tPaintBy

    private Expando network
    private MechanismPaintField paintBy

    @Override
    void run(TaskMonitor tm) throws Exception {
        CyTable scoresTable = CY.cyTableManager.getAllTables(false).find {
            it.title == SDP_RCR_SCORES_TABLE
        }
        if (scoresTable.rowCount == 0) {
            // TODO Report no scores.
            return
        }

        RCRPaint painter = new SdpWebRCRPaint()
        Collection<CyNetwork> networkCol = network.networks
        networkCol.each { CyNetwork cyN ->
            cyN.defaultNodeTable.addVirtualColumns(scoresTable, NAME, false)
            painter.paintMechanisms(paintBy, cyN)
        }
    }

    /* Tunables */

    // Called by cytoscape
    @Tunable(description = "Network(s) to Paint")
    public ListSingleSelection<Expando> getTNetwork() {
        if (!tNetwork) {
            tNetwork = tunableNetworkCategory()
        }
        tNetwork
    }
    // Called by cytoscape
    public void setTNetwork(ListSingleSelection<Expando> sel) {
        this.network = sel.selectedValue
    }
    // Called by cytoscape
    @Tunable(description = "Paint Mechanism By")
    public ListSingleSelection<String> getTPaintBy() {
        if (!tPaintBy) {
            tPaintBy = tunableMechanismPaintField()
        }
        tPaintBy
    }
    // Called by cytoscape
    public void setTPaintBy(ListSingleSelection<String> sel) {
        this.paintBy = fromField(sel.selectedValue)
    }
}
