package model.builder.core

import org.cytoscape.model.CyNetwork
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.work.TaskIterator

class ImportRevisionFromMenuFactory implements NetworkTaskFactory {

    @Override
    TaskIterator createTaskIterator(CyNetwork cyN) {
        return new TaskIterator(0)
    }

    @Override
    boolean isReady(CyNetwork cyN) {
        cyN && cyN.getTable(CyNetwork.class, 'sdp.revisions')
    }
}
