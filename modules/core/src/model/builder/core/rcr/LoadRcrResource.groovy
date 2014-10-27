package model.builder.core.rcr

import model.builder.core.Activator
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.Util.createColumn
import static model.builder.core.Util.createTable
import static Constant.SDP_UID_COLUMN
import static Constant.SDP_URI_COLUMN
import static Constant.SDP_RCR_RESOURCE_TABLE

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
        if (!api) throw new NullPointerException('api is null')
        if (!id) throw new NullPointerException('id is null')
        this.api = api
        this.id = id
    }

    @Override
    void run(TaskMonitor tm) throws Exception {
        loadRcrToTable(api, id)
    }

    static Map loadRcrToTable(AuthorizedAPI api, String id) {
        WebResponse response = api.rcrResult(id)

        if (response.statusCode != 200) {
            throw new RuntimeException('Error loading SDP RCR')
        }

        String uri = api.uri(resource: 'rcr-results', uid: id)
        RcrResourceTableView rtv = new RcrResourceTableView()
        CyRow row = rtv.addObject(response.data)
        assert row, "CyRow does not exist after adding rcr result to view"
        rtv.getObj(uri)
    }
}
