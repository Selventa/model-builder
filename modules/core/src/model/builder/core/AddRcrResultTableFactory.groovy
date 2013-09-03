package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator

@TupleConstructor
class AddRcrResultTableFactory implements TaskFactory {

    final Map rcrResult
    final Expando cyRef

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddRcrResultTable(rcrResult, cyRef)
        )
    }

    @Override
    boolean isReady() {
        true
    }
}
