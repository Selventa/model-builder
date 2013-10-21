package model.builder.core

import model.builder.ui.UI
import model.builder.web.api.APIManager
import model.builder.web.api.WebResponse
import org.cytoscape.work.Task

import static org.cytoscape.model.CyTableUtil.getNodesInState
import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.task.AbstractNodeViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.view.model.View
import org.cytoscape.work.TaskIterator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import static org.cytoscape.model.CyNetwork.*

@TupleConstructor
class CreateSetFactory extends AbstractNodeViewTaskFactory {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages");
    final APIManager apiManager

    /**
     * {@inheritDoc}
     */
    @Override
    TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView cyNv) {
        def nodes = getNodesInState(cyNv.model, 'selected', true).collect {
            cyNv.model.getRow(it).get(NAME, String.class)
        }
        UI.createSet(nodes, { createDialog, name, desc, newItems ->
            def api = apiManager.authorizedAPI(apiManager.default)
            WebResponse res = api.postSet(name, desc, newItems)
            println res.statusCode
            res.statusCode == 201
        })
        return new TaskIterator({
            run: {}
            cancel: {}
        } as Task)
    }
}
