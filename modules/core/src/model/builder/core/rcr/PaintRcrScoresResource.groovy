package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import static MechanismPaintField.fromField
import static model.builder.core.Tunables.tunableNetworkCategory
import static model.builder.core.Util.copyColumns
import static model.builder.core.Util.createColumn
import static model.builder.core.rcr.Constant.SDP_RCR_FILL_COLOR_COLUMN
import static model.builder.core.rcr.LoadRcrResource.loadRcrToTable
import static model.builder.core.rcr.LoadRcrScoresResource.loadRcrScoresToTable
import static model.builder.core.rcr.MechanismPaintField.CONCORDANCE
import static model.builder.core.rcr.MechanismPaintField.DIRECTION
import static model.builder.core.rcr.MechanismPaintField.RICHNESS
import static model.builder.core.rcr.Tunables.tunableRcrField
import static org.cytoscape.model.CyNetwork.NAME

class PaintRcrScoresResource extends AbstractTask {

    private ListSingleSelection<Expando> tNetwork
    private ListSingleSelection<Expando> tRcr
    private ListSingleSelection<String> tPaintBy

    @Tunable(gravity = 8.0D,  groups = ['Significance Cutoffs'], description = 'Concordance'           )
    public double concordanceCutoff    = 0.1
    @Tunable(gravity = 9.0D,  groups = ['Significance Cutoffs'], description = 'Richness'              )
    public double richnessCutoff       = 0.1
    @Tunable(gravity = 10.0D, groups = ['Significance Cutoffs'], description = 'Paint Not Significant' )
    public boolean paintNotSignificant = false

    private Expando network
    private Expando rcr
    private MechanismPaintField paintBy

    private final AuthorizedAPI api
    private final String        id

    PaintRcrScoresResource(AuthorizedAPI api, String id) {
        this.api = api
        this.id  = id

        Map rcrLoad = loadRcrToTable(api, id)
        if (rcrLoad) {
            tRcr = tunableRcrField()
            rcr = new Expando(
                    name: rcrLoad.name,
                    uri : rcrLoad.uri ,
                    toString: {
                        "${rcrLoad.name} (${rcrLoad.uri})"
                    }
            )
            tRcr.selectedValue = rcr
        }
    }

    @Override
    void run(TaskMonitor tm) throws Exception {
        Expando rcr = loadRcrScoresToTable(api, id)

        CyTable scoresTable = rcr.table
        if (scoresTable.rowCount == 0) {
            // TODO Report no scores.
            return
        }

        RCRPaint painter = new SdpWebRCRPaint()
        Collection<CyNetwork> networkCol = network.networks
        networkCol.each { CyNetwork cyN ->
            copyColumns(scoresTable, scoresTable.primaryKey, cyN.defaultNodeTable,
                    cyN.defaultNodeTable.getColumn(NAME), false)

            // create column if needed; clear fill values
            if (cyN.defaultNodeTable.getColumn(SDP_RCR_FILL_COLOR_COLUMN)) {
                cyN.defaultNodeTable.deleteColumn(SDP_RCR_FILL_COLOR_COLUMN)
            }
            createColumn(cyN.defaultNodeTable, SDP_RCR_FILL_COLOR_COLUMN, String.class, false, null)

            Map significant = cyN.defaultNodeTable.allRows.groupBy {
                CyRow row ->
                    Double concordance = row.get('sdp_concordance', Double.class)
                    Double richness    = row.get('sdp_richness', Double.class)
                    if (!concordance && !richness) {
                        return null
                    }
                    return (concordance <= concordanceCutoff) && (richness <= richnessCutoff)
            }

            // set significant color based on scales
            significant[true].each {
                CyRow row ->
                    if (paintBy == DIRECTION) {
                        String dir = row.get('sdp_direction', String.class)
                        row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, dir))
                    } else if (paintBy == CONCORDANCE) {
                        String dir = row.get('sdp_direction', String.class)
                        Double concordance = row.get('sdp_concordance', Double.class)
                        row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, concordance))
                    } else if (paintBy == RICHNESS) {
                        String dir = row.get('sdp_direction', String.class)
                        Double richness = row.get('sdp_richness', Double.class)
                        row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, richness))
                    }
            }

            // special color for not significant if desired; otherwise show as unmeasured
            String notSignificantColor = paintNotSignificant ? '#FFC2C2' : '#AAAAAA'
            significant[false].each {
                CyRow row ->
                    row.set(SDP_RCR_FILL_COLOR_COLUMN, notSignificantColor)
            }

            // unmeasured
            significant[null].each {
                it.set(SDP_RCR_FILL_COLOR_COLUMN, '#AAAAAA')
            }

        }
        painter.paintMechanisms(paintBy, networkCol)
    }

    /* Tunables */

    // Called by cytoscape
    @Tunable(gravity = 3.0D, description = "Network(s) to Paint")
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
    @Tunable(gravity = 1.0D, description = "RCR to Paint")
    public ListSingleSelection<Expando> getTRcr() {
        if (!tRcr) {
            tRcr = tunableRcrField()
        }
        tRcr
    }
    // Called by cytoscape
    public void setTRcr(ListSingleSelection<Expando> sel) {
        this.rcr = sel.selectedValue
    }
    // Called by cytoscape
    @Tunable(gravity = 2.0D, description = "Paint Mechanism By")
    public ListSingleSelection<String> getTPaintBy() {
        if (!tPaintBy) {
            tPaintBy = Tunables.tunableMechanismPaintField()
        }
        tPaintBy
    }
    // Called by cytoscape
    public void setTPaintBy(ListSingleSelection<String> sel) {
        this.paintBy = fromField(sel.selectedValue.toLowerCase())
    }
}
