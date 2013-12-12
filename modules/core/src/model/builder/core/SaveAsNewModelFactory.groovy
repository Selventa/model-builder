package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.APIManager
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.view.model.CyNetworkView
import org.cytoscape.work.TaskIterator

@TupleConstructor
class SaveAsNewModelFactory implements NetworkViewTaskFactory {

    final Expando cyRef
    final APIManager apiManager

    @Override
    TaskIterator createTaskIterator(CyNetworkView cyNv) {
        new TaskIterator(new SaveAsNewModel(cyNv, apiManager))
    }

    @Override
    boolean isReady(CyNetworkView cyNv) {
        if (!(cyNv && cyNv.model))
            false
        else {
            def cyN = cyNv.model
            cyN.getRow(cyN).isSet('uri')
        }
    }
}
