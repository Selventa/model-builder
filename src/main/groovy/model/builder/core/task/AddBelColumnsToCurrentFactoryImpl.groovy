package model.builder.core.task

import groovy.transform.TupleConstructor
import model.builder.core.AddBelColumnsToCurrentFactory
import org.cytoscape.work.AbstractTaskFactory
import org.cytoscape.work.TaskIterator

@TupleConstructor
class AddBelColumnsToCurrentFactoryImpl extends AbstractTaskFactory
    implements AddBelColumnsToCurrentFactory {

    final Expando cyRef

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddBelColumnsToCurrent(cyRef.cyApplicationManager, null),
            new ApplyPreferredStyleToCurrent(cyRef.cyApplicationManager,
                    cyRef.cyEventHelper, cyRef.visualMappingManager))
    }
}
