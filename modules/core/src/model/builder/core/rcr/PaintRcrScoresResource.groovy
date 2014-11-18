package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.cytoscape.work.Tunable
import org.cytoscape.work.util.ListSingleSelection
import org.openbel.ws.api.WsAPI

import java.text.DecimalFormat

import static ScorePaintField.*
import static ScorePaintField.fromField
import static model.builder.core.Tunables.tunableNetworkCategory
import static model.builder.core.Util.copyColumns
import static model.builder.core.Util.createColumn
import static model.builder.core.Util.inferOpenBELWsAPI
import static model.builder.core.rcr.Constant.SDP_RCR_FILL_COLOR_COLUMN
import static model.builder.core.rcr.Constant.SDP_RCR_SIGNIFICANT_COLUMN
import static model.builder.core.rcr.Constant.SDP_RCR_TOOLTIP_COLUMN
import static model.builder.core.rcr.LoadRcrResource.loadRcrToTable
import static model.builder.core.rcr.LoadRcrScoresResource.loadRcrScoresToTable
import static model.builder.core.rcr.Tunables.tunableRcrField
import static org.cytoscape.model.CyNetwork.NAME

/**
 * TODO Separate tunable state from class for functional reuse.
 */
class PaintRcrScoresResource extends AbstractTask {

    private static DecimalFormat decimalFormat = new DecimalFormat('0.##E0')
    private static Map tunableState = null

    private ListSingleSelection<Expando> tNetwork
    private ListSingleSelection<Expando> tRcr
    private ListSingleSelection<String> tPaintBy

    @Tunable(
            gravity = 8.0D,
            groups = ['Advanced Options'],
            description = 'Concordance',
            tooltip = 'Compares the consistency of the downstream state changes predicted by a mechanism with the actual measured changes.',
            params = 'displayState=collapsed',
            format=''
    )
    public Double concordanceCutoff      = 0.1D
    @Tunable(
            gravity = 9.0D,
            groups = ['Advanced Options'],
            description = 'Richness',
            tooltip = 'Determines if there is a high amount of signal in this mechanism relative to the amount of signal in general.',
            params = 'displayState=collapsed',
            format=''
    )
    public Double richnessCutoff         = 0.1D
    @Tunable(
            gravity = 10.0D,
            groups = ['Advanced Options'],
            description = 'Paint Not Scored (Grey)',
            tooltip = 'If selected the Not Scored nodes will be painted grey. If deselected the Not Scored nodes will be white.',
            params = 'displayState=collapsed'
    )
    public boolean paintNotScored        = false
    @Tunable(
            gravity = 10.0D,
            groups = ['Advanced Options'],
            description = 'Outline Not Significant (Red)',
            tooltip = 'If selected a node will be outlined in red if it greater than the richness or concordance cutoff. If deselected the Not Significant nodes will be painted white and will not have an outline.',
            params = 'displayState=collapsed'
    )
    public boolean outlineNotSignificant = false

    private Expando network
    private Expando rcr
    private ScorePaintField paintBy

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

        loadTunableState()
    }

    @Override
    void run(final TaskMonitor tm) throws Exception {
        tm.title = "Painting RCR Scores"
        Expando rcr = loadRcrScoresToTable(api, id, tm)

        CyTable scoresTable = rcr.table
        if (scoresTable.rowCount == 0) {
            // TODO Report no scores.
            return
        }

        RCRPaint painter = new SdpWebRCRPaint()
        Collection<CyNetwork> networkColl = network.networks.call()
        if (networkColl) {
            WsAPI wsAPI = inferOpenBELWsAPI(api.access().host)
            int increment = 1.0 / networkColl.size()
            networkColl.each { CyNetwork cyN ->
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
                if (cyN.defaultNodeTable.getColumn(SDP_RCR_SIGNIFICANT_COLUMN)) {
                    cyN.defaultNodeTable.deleteColumn(SDP_RCR_SIGNIFICANT_COLUMN)
                }
                createColumn(cyN.defaultNodeTable, SDP_RCR_SIGNIFICANT_COLUMN, Boolean.class, false, null)
                if (cyN.defaultNodeTable.getColumn(SDP_RCR_TOOLTIP_COLUMN)) {
                    cyN.defaultNodeTable.deleteColumn(SDP_RCR_TOOLTIP_COLUMN)
                }
                createColumn(cyN.defaultNodeTable, SDP_RCR_TOOLTIP_COLUMN, String.class, false, null)

                Map significant = cyN.defaultNodeTable.allRows.groupBy {
                    CyRow row ->
                        Double concordance = row.get('sdp_concordance', Double.class)
                        Double richness = row.get('sdp_richness', Double.class)
                        if (!concordance && !richness) {
                            return null
                        }
                        return (concordance <= concordanceCutoff) && (richness <= richnessCutoff)
                }

                // set significant color based on scales
                cyN.defaultNodeTable.allRows.each {
                    CyRow row ->
                        String dir = row.get('sdp_direction', String.class)
                        Double concordance = row.get('sdp_concordance', Double.class)
                        Double richness = row.get('sdp_richness', Double.class)

                        if (paintBy == DIRECTION) {
                            row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, dir))
                        } else if (paintBy == CONCORDANCE || paintBy == CONCORDANCE_AND_DIRECTION) {
                            row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, concordance))
                        } else if (paintBy == RICHNESS || paintBy == RICHNESS_AND_DIRECTION) {
                            row.set(SDP_RCR_FILL_COLOR_COLUMN, painter.paintColor(dir, paintBy, richness))
                        }

                        String tooltip = ['sdp_concordance', 'sdp_richness'].collect {
                            String key ->
                                Double value = row.get(key, Double.class)
                                value != null ?
                                        "${key.substring(4)}: ${decimalFormat.format(value)}" :
                                        null
                        }.findAll().join(', ')
                        if (tooltip) {
                            row.set(SDP_RCR_TOOLTIP_COLUMN, tooltip)
                        }
                }

                if (outlineNotSignificant) {
                    significant[true].each {
                        CyRow row -> row.set(SDP_RCR_SIGNIFICANT_COLUMN, true)
                    }
                    significant[false].each {
                        CyRow row -> row.set(SDP_RCR_SIGNIFICANT_COLUMN, false)
                    }
                } else {
                    significant[false].each {
                        CyRow row ->
                            row.set(SDP_RCR_FILL_COLOR_COLUMN,  '#FFFFFF')
                    }
                }

                // unmeasured
                if (paintNotScored) {
                    significant[null].each {
                        it.set(SDP_RCR_FILL_COLOR_COLUMN, '#AAAAAA')
                    }
                }

                tm.statusMessage = "Painting \"${cyN.getRow(cyN).get(NAME, String.class)}\""
                painter.paintNetwork(paintBy, cyN)
                tm.progress += increment
            }
        }

        // save tunable state
        synchronized (getClass()) {
            tunableState = [
                    TRcr                 : tRcr.selectedValue,
                    TNetwork             : tNetwork.selectedValue,
                    TPaintBy             : tPaintBy.selectedValue,
                    concordanceCutoff    : concordanceCutoff,
                    richnessCutoff       : richnessCutoff,
                    paintNotScored       : paintNotScored,
                    outlineNotSignificant: outlineNotSignificant
            ]
        }
    }

    private void loadTunableState() {
        if (tunableState) {
            synchronized (getClass()) {
                tunableState.findAll {
                    this.hasProperty(it.key.toString())
                }.each {
                    def prop = this.getProperty(it.key.toString())
                    if (prop instanceof ListSingleSelection) {
                        ((ListSingleSelection) prop).selectedValue = it.value
                    } else {
                        setProperty(it.key.toString(), it.value)
                    }
                }
            }
        }
    }

    /* Tunables */

    // Called by cytoscape
    @Tunable(
            gravity = 3.0D,
            description = "Network(s) to Paint",
            tooltip = 'This controls which networks will be painted with RCR data.'
    )
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
    @Tunable(
            gravity = 1.0D,
            description = 'RCR to Paint',
            tooltip = 'This controls which RCR will be painted.'
    )
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
    @Tunable(
            gravity = 7.0D,
            groups = ['Advanced Options'],
            description = 'Paint Score By',
            tooltip = 'This controls which RCR score value to paint (e.g. Direction, Concordance, Richness)',
            params = 'displayState=collapsed'
    )
    public ListSingleSelection<String> getTPaintBy() {
        if (!tPaintBy) {
            tPaintBy = Tunables.tunableScorePaintField()
        }
        tPaintBy
    }
    // Called by cytoscape
    public void setTPaintBy(ListSingleSelection<String> sel) {
        this.paintBy = fromField(sel.selectedValue)
    }
}
