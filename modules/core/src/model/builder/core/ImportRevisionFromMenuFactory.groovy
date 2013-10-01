package model.builder.core

import model.builder.web.api.APIManager
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory

class ImportRevisionFromMenuFactory implements NetworkTaskFactory {

    final APIManager apiManager
    final Expando cyRef
    final AddBelColumnsToCurrentFactory belFac

    ImportRevisionFromMenuFactory(APIManager apiManager, Expando cyRef,
                                  AddBelColumnsToCurrentFactory belFac) {
        this.apiManager = apiManager
        this.cyRef = cyRef
        this.belFac = belFac
    }

    @Override
    TaskIterator createTaskIterator(CyNetwork cyN) {
        TaskIterator tasks = new TaskIterator(
            new CreateCyNetworkForModelRevisionTunable(cyN, apiManager, cyRef))
        tasks.append(belFac.createTaskIterator())
        tasks
    }

    @Override
    boolean isReady(CyNetwork cyN) {
        def mgr = cyRef.cyTableManager
        cyN && mgr.globalTables?.any{it.title == 'SDP.Revisions'}
    }
}
