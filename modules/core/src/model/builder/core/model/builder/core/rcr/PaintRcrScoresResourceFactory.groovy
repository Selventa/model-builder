package model.builder.core.model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator
import static model.builder.core.Activator.CY

/**
 * Task factory to paint SDP RCR scores resource onto one or more networks.
 */
class PaintRcrScoresResourceFactory implements TaskFactory {

    private AuthorizedAPI api
    private String id

    PaintRcrScoresResourceFactory(AuthorizedAPI api, String id) {
        if (!api) throw new NullPointerException("api is null")
        if (!id) throw new NullPointerException("id is null")
        this.api = api
        this.id = id
    }

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
                new LoadRcrResource(api, id),
                new LoadRcrScoresResource(api, id),
                new PaintRcrScoresResource()
        )
    }

    @Override
    boolean isReady() {
        return true
    }
}
