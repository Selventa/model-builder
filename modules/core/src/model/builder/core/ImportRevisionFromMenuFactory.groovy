package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory

@TupleConstructor
class ImportRevisionFromMenuFactory implements NetworkTaskFactory {

    final APIManager apiManager
    final Expando cyRef
    final AddBelColumnsToCurrentFactory addBelFac

    @Override
    TaskIterator createTaskIterator(CyNetwork cyN) {
        TaskIterator tasks = new TaskIterator(
            new CreateCyNetworkForModelRevisionTunable(cyN, apiManager, cyRef))
        tasks.append(addBelFac.createTaskIterator())
        tasks
    }

    @Override
    boolean isReady(CyNetwork cyN) {
        def mgr = cyRef.cyTableManager
        cyN && mgr.globalTables?.any{it.title == 'SDP.Revisions'}
    }
}
