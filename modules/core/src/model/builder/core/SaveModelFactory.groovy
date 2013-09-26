package model.builder.core

import org.cytoscape.model.CyNetwork
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.work.TaskIterator

class SaveModelFactory implements NetworkTaskFactory {

    @Override
    TaskIterator createTaskIterator(CyNetwork cyN) {
        new TaskIterator(new SaveModel(cyN))
    }

    @Override
    boolean isReady(CyNetwork cyN) {
        return true
    }
}
