package model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator

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
                new PaintRcrScoresResource(api, id)
        )
    }

    @Override
    boolean isReady() {
        return true
    }
}
