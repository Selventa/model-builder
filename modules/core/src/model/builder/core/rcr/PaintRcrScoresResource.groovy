package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.BoundedDouble
import org.cytoscape.work.util.ListSingleSelection
import org.openbel.ws.api.WsAPI

import static MechanismPaintField.fromField
import static model.builder.core.Tunables.tunableNetworkCategory
import static model.builder.core.Util.copyColumns
import static model.builder.core.Util.createColumn
import static model.builder.core.Util.inferOpenBELWsAPI
import static model.builder.core.rcr.Constant.SDP_RCR_FILL_COLOR_COLUMN
import static model.builder.core.rcr.Constant.SDP_RCR_SIGNIFICANT_COLUMN
import static model.builder.core.rcr.Constant.SDP_RCR_TEXT_COLOR_COLUMN
import static model.builder.core.rcr.LoadRcrResource.loadRcrToTable
import static model.builder.core.rcr.LoadRcrScoresResource.loadRcrScoresToTable
import static model.builder.core.rcr.MechanismPaintField.*
import static model.builder.core.rcr.Tunables.tunableRcrField
import static org.cytoscape.model.CyNetwork.NAME

class PaintRcrScoresResource extends AbstractTask {

    private ListSingleSelection<Expando> tNetwork
    private ListSingleSelection<Expando> tRcr
    private ListSingleSelection<String> tPaintBy

    @Tunable(gravity = 8.0D,  groups = ['Significance Cutoffs'], description = 'Concordance', params = 'slider=true' )
    public BoundedDouble concordanceCutoff    = new BoundedDouble(0.0, 0.1, 1.0, true, true)
    @Tunable(gravity = 9.0D,  groups = ['Significance Cutoffs'], description = 'Richness',    params = 'slider=true' )
    public BoundedDouble richnessCutoff       = new BoundedDouble(0.0, 0.1, 1.0, true, true)
    @Tunable(gravity = 10.0D, groups = ['Significance Cutoffs'], description = 'Outline Not Significant Scores'      )
    public boolean outlineNotSignificant = false

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
            rcr = new Expando(rcrLoad)
            rcr.setProperty("toString", {
                "${rcrLoad.name} (${rcrLoad.uri})"
            })
            tRcr.selectedValue = rcr
        }
    }

    @Override
    void run(final TaskMonitor tm) throws Exception {
        tm.title = "Painting RCR Scores for \"${rcr.name}\""
        Expando rcr = loadRcrScoresToTable(api, id, tm)

        CyTable scoresTable = rcr.table
        if (scoresTable.rowCount == 0) {
            // TODO Report no scores.
            return
        }

        if (!network?.networks) {
            throw new IllegalStateException("The selected network(s) do not exist.")
        }

        double richnessControl    = richnessCutoff.getValue()
        double concordanceControl = concordanceCutoff.getValue()

        RCRPaint painter = new SdpWebRCRPaint()
        Collection<CyNetwork> networkCol = network.networks
        if (networkCol) {
            WsAPI wsAPI = inferOpenBELWsAPI(api.access().host)
            int increment = 1.0 / networkCol.size()
            networkCol.each { CyNetwork cyN ->
                tm.statusMessage = "Linking \"${cyN.getRow(cyN).get(NAME, String.class)}\""
                wsAPI.linkNodes(cyN, this.rcr.knowledge_network)

                // copy columns to network nodes based on kam id
                copyColumns(scoresTable, scoresTable.getColumn('kam_id'), cyN.defaultNodeTable,
                        cyN.defaultNodeTable.getColumn('kam.id'), false)

                // create columns if needed; clear fill values
                if (cyN.defaultNodeTable.getColumn(SDP_RCR_FILL_COLOR_COLUMN)) {
                    cyN.defaultNodeTable.deleteColumn(SDP_RCR_FILL_COLOR_COLUMN)
                }
                createColumn(cyN.defaultNodeTable, SDP_RCR_FILL_COLOR_COLUMN, String.class, false, null)
                if (cyN.defaultNodeTable.getColumn(SDP_RCR_TEXT_COLOR_COLUMN)) {
                    cyN.defaultNodeTable.deleteColumn(SDP_RCR_TEXT_COLOR_COLUMN)
                }
                createColumn(cyN.defaultNodeTable, SDP_RCR_TEXT_COLOR_COLUMN, String.class, false, null)
                if (cyN.defaultNodeTable.getColumn(SDP_RCR_SIGNIFICANT_COLUMN)) {
                    cyN.defaultNodeTable.deleteColumn(SDP_RCR_SIGNIFICANT_COLUMN)
                }
                createColumn(cyN.defaultNodeTable, SDP_RCR_SIGNIFICANT_COLUMN, Boolean.class, false, null)

                Map significant = cyN.defaultNodeTable.allRows.groupBy {
                    CyRow row ->
                        Double concordance = row.get('sdp_concordance', Double.class)
                        Double richness = row.get('sdp_richness', Double.class)
                        if (!concordance && !richness) {
                            return null
                        }
                        return (concordance <= concordanceControl) && (richness <= richnessControl)
                }

                // set significant color based on scales
                significant[true].each {
                    CyRow row ->
                        if (paintBy == DIRECTION) {
                            String dir = row.get('sdp_direction', String.class)
                            row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, dir))
                            row.set(SDP_RCR_TEXT_COLOR_COLUMN, painter.textColor(dir, paintBy, dir))
                        } else if (paintBy == CONCORDANCE) {
                            String dir = row.get('sdp_direction', String.class)
                            Double concordance = row.get('sdp_concordance', Double.class)
                            row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, concordance))
                            row.set(SDP_RCR_TEXT_COLOR_COLUMN, painter.textColor(dir, paintBy, concordance))
                        } else if (paintBy == RICHNESS) {
                            String dir = row.get('sdp_direction', String.class)
                            Double richness = row.get('sdp_richness', Double.class)
                            row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, richness))
                            row.set(SDP_RCR_TEXT_COLOR_COLUMN, painter.textColor(dir, paintBy, richness))
                        }
                }

                significant[false].each {
                    CyRow row ->
                        row.set(SDP_RCR_FILL_COLOR_COLUMN,  '#FFFFFF')
                }

                if (outlineNotSignificant) {
                    significant[true].each {
                        CyRow row -> row.set(SDP_RCR_SIGNIFICANT_COLUMN, true)
                    }
                    significant[false].each {
                        CyRow row -> row.set(SDP_RCR_SIGNIFICANT_COLUMN, false)
                    }
                }

                // unmeasured
                significant[null].each {
                    it.set(SDP_RCR_FILL_COLOR_COLUMN, '#AAAAAA')
                }

                tm.progress += increment
            }
            tm.statusMessage = "Painting networks with RCR Scores for \"${rcr.name}\""
            painter.paintMechanisms(paintBy, networkCol)
        }
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
