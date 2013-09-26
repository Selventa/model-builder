package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.APIManager
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator

@TupleConstructor
class SaveModelFactory implements NetworkViewTaskFactory {

    final APIManager apiManager

    @Override
    TaskIterator createTaskIterator(CyNetworkView cyNv) {
        new TaskIterator(new SaveModel(cyNv, apiManager))
    }

    @Override
    boolean isReady(CyNetworkView cyNv) {
        return true
    }
}
