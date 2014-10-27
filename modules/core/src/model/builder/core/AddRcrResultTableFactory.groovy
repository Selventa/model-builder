package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.cytoscape.model.CyTableFactory
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskFactory
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskMonitor

import static org.cytoscape.model.CyNetwork.NAME

@TupleConstructor
class AddRcrResultTableFactory implements TaskFactory {

    final Map rcrResult
    final Expando cyRef
    final VisualMappingFunctionFactory dMapFac
    final VisualMappingFunctionFactory pMapFac

    @Override
    TaskIterator createTaskIterator() {
        CyTableFactory tableFac = cyRef.cyTableFactory
        CyTable rcrTable = tableFac.createTable(
                "RCR Scores - ${rcrResult.name}", NAME, String.class, true, false)
        def selected = cyRef.cyApplicationManager.selectedNetworks

        def mapTblFactory = cyRef.mapTableToNetworkTablesTaskFactory
        def tasks = new TaskIterator(
            new AddRcrResultTable(rcrResult, rcrTable, cyRef))
        tasks.append(mapTblFactory.createTaskIterator(
                rcrTable, true, selected, CyNode.class))
        tasks.append(new ApplyRcrResultStyle(cyRef, dMapFac, pMapFac))
        tasks.append(new AbstractTask() {
            @Override
            void run(TaskMonitor monitor) throws Exception {
                selected.collect {it.getTable(CyNode.class, 'sdp.rcr')}.each {
                    it.public = true
                }
            }
        })
        tasks
    }

    @Override
    boolean isReady() {
        true
    }
}
