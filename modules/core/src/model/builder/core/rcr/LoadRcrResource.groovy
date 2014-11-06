package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyRow
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

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
        String uri = api.uri(resource: 'rcr-results', uid: id)
        RcrResourceTableView rtv = new RcrResourceTableView()

        if (rtv.exists(uri)) {
            rtv.getObj(uri)
        } else {
            WebResponse response = api.rcrResult(id)

            if (response.statusCode != 200) {
                throw new RuntimeException('Error loading SDP RCR')
            }

            CyRow row = rtv.addObject(response.data)
            assert row, "CyRow does not exist after adding rcr result to view"
            rtv.getObj(uri)
        }
    }
}
