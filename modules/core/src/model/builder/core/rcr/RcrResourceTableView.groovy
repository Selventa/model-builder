package model.builder.core.rcr

import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable

import static model.builder.core.Activator.CY
import static model.builder.core.Util.createColumn
import static model.builder.core.Util.createTable
import static Constant.*

class RcrResourceTableView extends AbstractCyTableView<String, Map> {

    private CyTable table

    @Override
    CyTable getTable() {
        if (table) {
            return table
        }

        table = createTable(SDP_RCR_RESOURCE_TABLE, SDP_URI_COLUMN,
                SDP_URI_COLUMN.class, true, false,
                CY.cyTableManager, CY.cyTableFactory)
        if (table.columns.size() == 1) {
            createColumn(table, SDP_UID_COLUMN,          String.class, true, null)
            createColumn(table, 'sdp_name',              String.class, true, null)
            createColumn(table, 'sdp_description',       String.class, true, null)
            createColumn(table, 'sdp_comparison_name',   String.class, true, null)
            createColumn(table, 'sdp_comparison_uri',    String.class, true, null)
            createColumn(table, 'sdp_knowledge_network', String.class, true, null)
            createColumn(table, 'sdp_created_at',        String.class, true, null)
            createColumn(table, 'sdp_updated_at',        String.class, true, null)
        }
        table
    }

    @Override
    CyRow addObject(Map obj) {
        String uri = obj.uri
        if (!uri) {
            throw new IllegalArgumentException('obj does not contain uri field')
        }

        CyRow row = getTable().getRow(uri)
        row.set(SDP_UID_COLUMN, uri[uri.lastIndexOf('/')+1..-1])
        row.set('sdp_name',         obj.name                          ?: '')
        row.set('sdp_description',  obj.description                   ?: '')

        if (obj.comparison) {
            row.set('sdp_comparison_name', obj.comparison.name ?: '')
            row.set('sdp_comparison_uri',  obj.comparison.uri  ?: '')
        }

        row.set('sdp_knowledge_network', obj.knowledge_network ?: '')
        row.set('sdp_created_at',        obj._created_at       ?: '')
        row.set('sdp_updated_at',        obj._updated_at       ?: '')
        row
    }

    @Override
    Map getObj(String key) {
        CyRow row = getTable().getRow(key)
        if (!row) return null

        [
                "${SDP_URI_COLUMN}" : key,
                "${SDP_UID_COLUMN}" : row.get(SDP_UID_COLUMN,          String.class),
                name                : row.get('sdp_name',              String.class),
                description         : row.get('sdp_description',       String.class),
                comparison_name     : row.get('sdp_comparison_name',   String.class),
                comparison_uri      : row.get('sdp_comparison_uri',    String.class),
                knowledge_network   : row.get('sdp_knowledge_network', String.class),
                created_at          : row.get('sdp_created_at',        String.class),
                updated_at          : row.get('sdp_updated_at',        String.class)
        ]
    }
}
