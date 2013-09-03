package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.Task
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class AddRcrResultTableFactory implements TaskFactory {

    final Map rcrResult
    final Expando cyRef
    final VisualMappingFunctionFactory cMapFac
    final VisualMappingFunctionFactory dMapFac

    @Override
    TaskIterator createTaskIterator() {
        new TaskIterator(
            new AddRcrResultTable(rcrResult, cyRef, cMapFac, dMapFac),
            new AbstractTask() {
                @Override
                void run(TaskMonitor monitor) throws Exception {
                    def selected = cyRef.cyApplicationManager.selectedNetworks
                    selected.collect {it.getTable(CyNode.class, 'sdp.rcr')}.each {
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
