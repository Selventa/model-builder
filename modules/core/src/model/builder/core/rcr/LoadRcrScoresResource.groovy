package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyRow
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import org.openbel.ws.api.WsAPI

import static model.builder.core.Util.inferOpenBELWsAPI
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
        tm.progress = 0.0
        tm.title = 'Load RCR Scores'
        loadRcrScoresToTable(api, uid, tm)
    }

    static Expando loadRcrScoresToTable(AuthorizedAPI api, String id, TaskMonitor tm) {
        // load rcr
        def rcr = loadRcrToTable(api, id)

        // check if rcr scores were loaded/resolved
        String uri = api.uri(resource: 'rcr-results', uid: id)
        RcrScoresResourceTableView rstv = new RcrScoresResourceTableView(rcr.name, uri, id)
        List<Map> scores = rstv.getAll()
        if (scores.empty) {
            //...need to fetch rcr scores
            WebResponse res = api.rcrResultScores(id)
            if (res.statusCode != 200) {
                throw new RuntimeException('Error loading SDP RCR scores')
            }
            scores = (List<Map>) res.data

            //...resolve score nodes to rcr knowledge network
            List<String> mechanisms = scores.collect {
                it.get('mechanism').toString() ?: ''
            }
            WsAPI wsAPI = inferOpenBELWsAPI(api.access().host)
            Map<String, Map> mechanismIds = [:]
            Iterator<Map> resolvedMechanisms = wsAPI.resolveNodes(mechanisms, rcr.knowledge_network).iterator()
            if (resolvedMechanisms && resolvedMechanisms.hasNext()) {
                mechanisms.each {
                    if (!resolvedMechanisms.hasNext()) return null
                    def node = resolvedMechanisms.next()
                    if (node) {
                        mechanismIds.put(it, node)
                    }
                }
            }

            //...add to table
            scores = scores.collect {
                Map node = mechanismIds.get(it.get('mechanism') ?: '')
                it.put('kam_id', node?.id)
                CyRow row = rstv.addObject(it)
                assert row, "CyRow does not exist after adding rcr score to view"
                row
            }.collect {
                rstv.getObj(it.get('sdp_mechanism', String.class))
            }
        }
        tm.progress = 0.5

        new Expando(table : rstv.table, scores: scores)
    }
}
