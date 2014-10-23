package model.builder.core.model.builder.core.rcr

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator

/**
 * Task factory to load an SDP RCR resource into a Cytoscape table.
 * <br><br>
 * This task does not load subresources (e.g. scores, state changes) from the
 * RCR item.
 */
class LoadRcrResourceFactory implements TaskFactory {

    private AuthorizedAPI api
    private String id

    LoadRcrResourceFactory(AuthorizedAPI api, String id) {
        if (!api) throw new NullPointerException("api is null")
        if (!id) throw new NullPointerException("id is null")
        this.api = api
        this.id = id
    }

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(new LoadRcrResource(api, id))
    }

    @Override
    boolean isReady() {
        return true
    }
}
