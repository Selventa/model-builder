package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class AddComparisonTableFactory implements TaskFactory {

    final Map comparison
    final Expando cyRef

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddComparisonTable(comparison, cyRef),
            new AbstractTask() {
                @Override
                void run(TaskMonitor monitor) throws Exception {
                    def selected = cyRef.cyApplicationManager.selectedNetworks
                    selected.collect {it.getTable(CyNode.class, 'sdp.comparison')}.each {
                        it.public = true
                    }
                }
            }
        )
    }

    @Override
    boolean isReady() {
        true
    }
}
