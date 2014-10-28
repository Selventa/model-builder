package model.builder.core.rcr

import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable

import static model.builder.core.Activator.CY
import static model.builder.core.Util.convertToDouble
import static model.builder.core.Util.convertToInteger
import static model.builder.core.Util.createColumn
import static model.builder.core.Util.createTable
import static model.builder.core.rcr.Constant.*

class RcrScoresResourceTableView extends AbstractCyTableView<String, Map> {

    private final String rcrName, uri, uid
    private CyTable table

    RcrScoresResourceTableView(String rcrName, String uri, String uid) {
        this.rcrName = rcrName
        this.uri = uri
        this.uid = uid
    }

    @Override
    CyTable getTable() {
        if (table) {
            return table
        }

        String tableName = "${SDP_RCR_SCORES_TABLE_PREFIX} $rcrName ($uri)"
        table = createTable(tableName, 'sdp_mechanism', String.class, true, false,
                CY.cyTableManager, CY.cyTableFactory)
        if (table.columns.size() == 1) {
            createColumn(table, SDP_URI_COLUMN,     String.class,  true, null)
            createColumn(table, SDP_UID_COLUMN,     String.class,  true, null)
            createColumn(table, 'sdp_ambiguous',    Integer.class, true, null)
            createColumn(table, 'sdp_concordance',  Double.class,  true, null)
            createColumn(table, 'sdp_contra',       Integer.class, true, null)
            createColumn(table, 'sdp_correct',      Integer.class, true, null)
            createColumn(table, 'sdp_direction',    String.class,  true, null)
            createColumn(table, 'sdp_observed',     Integer.class, true, null)
            createColumn(table, 'sdp_possible',     Integer.class, true, null)
            createColumn(table, 'sdp_richness',     Double.class,  true, null)
        }
        table
    }

    @Override
    CyRow addObject(Map obj) {
        CyRow row = getTable().getRow(obj.mechanism)
        row.set(SDP_URI_COLUMN, uri                                          )
        row.set(SDP_UID_COLUMN, uid                                          )
        row.set('sdp_ambiguous',    convertToInteger(obj.ambiguous   ?: 0   ))
        row.set('sdp_concordance',  convertToDouble (obj.concordance ?: 0.0 ))
        row.set('sdp_contra',       convertToInteger(obj.contra      ?: 0   ))
        row.set('sdp_correct',      convertToInteger(obj.correct     ?: 0   ))
        row.set('sdp_direction',    obj.direction   ?: ''                    )
        row.set('sdp_observed',     convertToInteger(obj.observed    ?: 0   ))
        row.set('sdp_possible',     convertToInteger(obj.possible    ?: 0   ))
        row.set('sdp_richness',     convertToDouble (obj.richness    ?: 0.0 ))
        row
    }

    @Override
    Map getObj(String key) {
        CyRow row = getTable().getRow(key)
        if (!row) return null

        [
                mechanism           : key,
                "${SDP_URI_COLUMN}" : row.get(SDP_URI_COLUMN,      String.class  ),
                "${SDP_UID_COLUMN}" : row.get(SDP_UID_COLUMN,      String.class  ),
                ambiguous           : row.get('sdp_ambiguous',     Integer.class ),
                concordance         : row.get('sdp_concordance',   Double.class  ),
                contra              : row.get('sdp_contra',        Integer.class ),
                correct             : row.get('sdp_correct',       Integer.class ),
                direction           : row.get('sdp_direction',     String.class  ),
                observed            : row.get('sdp_observed',      Integer.class ),
                possible            : row.get('sdp_possible',      Integer.class ),
                richness            : row.get('sdp_richness',      Double.class  ),
        ]
    }
}
