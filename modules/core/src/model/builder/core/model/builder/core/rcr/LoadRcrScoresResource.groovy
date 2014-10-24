package model.builder.core.model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.model.SUIDFactory
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.Activator.CY
import static model.builder.core.Util.createColumn
import static model.builder.core.Util.createTable
import static model.builder.core.model.builder.core.rcr.Constant.SDP_RCR_SCORES_TABLE
import static model.builder.core.model.builder.core.rcr.Constant.SDP_UID_COLUMN
import static model.builder.core.model.builder.core.rcr.Constant.SDP_URI_COLUMN
import static org.cytoscape.model.CyIdentifiable.SUID;

/**
 * Task to load an SDP RCR scores resource into a Cytoscape table.
 * <br><br>
 * This task does not load subresources (e.g. scores, state changes) from the
 * RCR item.
 */
class LoadRcrScoresResource extends AbstractTask {

    private AuthorizedAPI api
    private String uid

    LoadRcrScoresResource(AuthorizedAPI api, String uid) {
        if (!api) throw new NullPointerException('api is null')
        if (!uid) throw new NullPointerException('uid is null')
        this.api = api
        this.uid = uid
    }

    @Override
    void run(TaskMonitor tm) throws Exception {
        WebResponse response = api.rcrResultScores(uid)

        if (response.statusCode != 200) {
            throw new RuntimeException('Error loading SDP RCR scores')
        }

        String uri = api.uri(resource: 'rcr-results', uid: uid)

        def rcrTable = createTableIdempotently()
        setRow(rcrTable, (List<Map>) response.data, uri, uid)
    }

    private static CyTable createTableIdempotently() {
        def rcrTable = createTable(SDP_RCR_SCORES_TABLE, SUID,
                Long.class, true, false,
                CY.cyTableManager, CY.cyTableFactory)

        if (rcrTable.columns.size() == 1) {
            createColumn(rcrTable, SDP_URI_COLUMN, String.class,  true, null)
            createColumn(rcrTable, SDP_UID_COLUMN, String.class,  true, null)
            createColumn(rcrTable, 'ambiguous',    Integer.class, true, null)
            createColumn(rcrTable, 'concordance',  Double.class,  true, null)
            createColumn(rcrTable, 'contra',       Integer.class, true, null)
            createColumn(rcrTable, 'correct',      Integer.class, true, null)
            createColumn(rcrTable, 'direction',    String.class,  true, null)
            createColumn(rcrTable, 'mechanism',    String.class,  true, null)
            createColumn(rcrTable, 'observed',     Integer.class, true, null)
            createColumn(rcrTable, 'possible',     Integer.class, true, null)
            createColumn(rcrTable, 'richness',     Double.class,  true, null)
        }
        rcrTable
    }

    private static List<CyRow> setRow(CyTable table, List<Map> scores,
                                      String uri, String uid) {
        scores.collect {
            CyRow row = table.getRow(SUIDFactory.nextSUID)
            row.set(SDP_URI_COLUMN, uri                   )
            row.set(SDP_UID_COLUMN, uid                   )
            row.set('ambiguous',    it.ambiguous   ?: 0   )
            row.set('concordance',  it.concordance ?: 0.0 )
            row.set('contra',       it.contra      ?: 0   )
            row.set('correct',      it.correct     ?: 0   )
            row.set('direction',    it.direction   ?: ''  )
            row.set('mechanism',    it.mechanism   ?: ''  )
            row.set('observed',     it.observed    ?: 0   )
            row.set('possible',     it.possible    ?: 0   )
            row.set('richness',     it.richness    ?: 0.0 )
            row
        }
    }
}
