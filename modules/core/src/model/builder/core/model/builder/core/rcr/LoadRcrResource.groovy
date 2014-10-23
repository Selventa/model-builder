package model.builder.core.model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.Activator.CY
import static model.builder.core.Util.createColumn
import static model.builder.core.Util.createTable
import static model.builder.core.model.builder.core.rcr.Constant.SDP_URI_COLUMN
import static model.builder.core.model.builder.core.rcr.Constant.SDP_RCR_RESOURCE_TABLE

/**
 * Task to load an SDP RCR resource into a Cytoscape table.
 * <br><br>
 * This task does not load subresources (e.g. scores, state changes) from the
 * RCR item.
 */
class LoadRcrResource extends AbstractTask {

    private AuthorizedAPI api
    private String id

    LoadRcrResource(AuthorizedAPI api, String id) {
        if (!api) throw new NullPointerException("api is null")
        if (!id) throw new NullPointerException("id is null")
        this.api = api
        this.id = id
    }

    @Override
    void run(TaskMonitor tm) throws Exception {
        WebResponse response = api.rcrResult(id)

        if (response.statusCode != 200) {
            throw new RuntimeException("Error loading SDP RCR")
        }

        def rcrTable = createTableIdempotently()
        setRow(rcrTable, response.data)
    }

    private static CyTable createTableIdempotently() {
        def rcrTable = createTable(SDP_RCR_RESOURCE_TABLE, SDP_URI_COLUMN,
                SDP_URI_COLUMN.class, true, false,
                CY.cyTableManager, CY.cyTableFactory)
        if (rcrTable.columns.size() == 1) {
            createColumn(rcrTable, "uid", String.class, true, null)
            createColumn(rcrTable, "name", String.class, true, null)
            createColumn(rcrTable, "description", String.class, true, null)
            createColumn(rcrTable, "comparison_name", String.class, true, null)
            createColumn(rcrTable, "comparison_uri", String.class, true, null)
            createColumn(rcrTable, "knowledge_network", String.class, true, null)
            createColumn(rcrTable, "created_by", String.class, true, null)
            createColumn(rcrTable, "created_at", String.class, true, null)
            createColumn(rcrTable, "updated_by", String.class, true, null)
            createColumn(rcrTable, "updated_at", String.class, true, null)
        }
        rcrTable
    }

    private static CyRow setRow(CyTable table, Map rcr) {
        CyRow row = table.getRow(rcr.uri)
        row.set('uid', rcr.uri[rcr.uri.lastIndexOf('/')+1..-1]);
        row.set('name', rcr.name ?: '')
        row.set('description', rcr.description ?: '')

        if (rcr.comparison) {
            row.set('comparison_name', rcr.comparison.name);
            row.set('comparison_uri', rcr.comparison.uri);
        }

        row.set('knowledge_network', rcr.knowledge_network ?: '')
        row.set('created_at', rcr._created_at ?: '')
        row.set('updated_at', rcr._updated_at ?: '')
        row
    }
}
