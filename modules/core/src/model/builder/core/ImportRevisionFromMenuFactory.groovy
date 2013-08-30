package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.API
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory

@TupleConstructor
class ImportRevisionFromMenuFactory implements NetworkTaskFactory {

    final API api
    final Expando cyRef
    final AddBelColumnsToCurrentFactory addBelFac

    @Override
    TaskIterator createTaskIterator(CyNetwork cyN) {
        TaskIterator tasks = new TaskIterator(
            new CreateCyNetworkForModelRevisionTunable(cyN, api, cyRef))
        tasks.append(addBelFac.createTaskIterator())
        tasks
    }

    @Override
    boolean isReady(CyNetwork cyN) {
        cyN && cyN.getTable(CyNetwork.class, 'sdp.revisions')
    }
}
