package model.builder.core

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.model.CyNetwork
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory

class ImportRevisionFromMenuFactory implements NetworkTaskFactory {

    final AuthorizedAPI api
    final Expando cyRef
    final AddBelColumnsToCurrentFactory addBelFac

    ImportRevisionFromMenuFactory(AuthorizedAPI api, Expando cyRef, AddBelColumnsToCurrentFactory addBelFac) {
        this.api = api
        this.cyRef = cyRef
        this.addBelFac = addBelFac
    }

    @Override
    TaskIterator createTaskIterator(CyNetwork cyN) {
        TaskIterator tasks = new TaskIterator(
            new CreateCyNetworkForModelRevisionTunable(cyN, api, cyRef))
        tasks.append(addBelFac.createTaskIterator())
        tasks
    }

    @Override
    boolean isReady(CyNetwork cyN) {
        def mgr = cyRef.cyTableManager
        cyN && mgr.globalTables.any{it.title == 'SDP.Revisions'}
    }
}
