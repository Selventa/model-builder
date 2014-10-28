package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyRow
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.rcr.LoadRcrResource.loadRcrToTable

/**
 * Task to load an SDP RCR scores resource into a Cytoscape table.
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
        loadRcrScoresToTable(api, uid)
    }

    static Expando loadRcrScoresToTable(AuthorizedAPI api, String id) {
        def rcr = loadRcrToTable(api, id)

        String uri = api.uri(resource: 'rcr-results', uid: id)
        RcrScoresResourceTableView rstv = new RcrScoresResourceTableView(rcr.name, uri, id)
        WebResponse res = api.rcrResultScores(id)
        if (res.statusCode != 200) {
            throw new RuntimeException('Error loading SDP RCR scores')
        }
        List<Map> scores = (List<Map>) res.data
        new Expando(
                table : rstv.table,
                scores:
                        scores.collect {
                            CyRow row = rstv.addObject(it)
                            assert row, "CyRow does not exist after adding rcr score to view"
                            row
                        }.collect {
                            rstv.getObj(it.get('sdp_mechanism', String.class))
                        }
        )
    }
}
