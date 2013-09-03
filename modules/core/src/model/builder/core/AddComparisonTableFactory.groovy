package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator

@TupleConstructor
class AddComparisonTableFactory implements TaskFactory {

    final Map comparison
    final Expando cyRef

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddComparisonTable(comparison, cyRef)
        )
    }

    @Override
    boolean isReady() {
        true
    }
}
