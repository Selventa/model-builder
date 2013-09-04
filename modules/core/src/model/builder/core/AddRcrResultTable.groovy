package model.builder.core

import org.cytoscape.model.CyNode
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import wslite.json.JSONObject

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyTable
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.Util.createColumn

@TupleConstructor
class AddRcrResultTable extends AbstractTask {

    final Map rcrResult
    final CyTable rcrTable
    final Expando cyRef

    @Override
    void run(TaskMonitor monitor) throws Exception {
        createColumn(rcrTable, 'direction', String.class, true)
        createColumn(rcrTable, 'richness', Double.class, true)
        createColumn(rcrTable, 'concordance', Double.class, true)
        createColumn(rcrTable, 'correct', Integer.class, true)
        createColumn(rcrTable, 'contra', Integer.class, true)
        createColumn(rcrTable, 'ambiguous', Integer.class, true)
        createColumn(rcrTable, 'observed', Integer.class, true)
        createColumn(rcrTable, 'possible', Integer.class, true)
        cyRef.cyTableManager.addTable(rcrTable)

        rcrResult.scores.each { JSONObject it ->
            def cyRow = rcrTable.getRow(it.getString('mechanism'))
            cyRow.set('direction', it.getString('direction'))
            cyRow.set('richness', it.getDouble('richness'))
            cyRow.set('concordance', it.getDouble('concordance'))
            cyRow.set('correct', it.getInt('correct'))
            cyRow.set('contra', it.getInt('contra'))
            cyRow.set('ambiguous', it.getInt('ambiguous'))
            cyRow.set('observed', it.getInt('observed'))
            cyRow.set('possible', it.getInt('possible'))
        }

        def selected = cyRef.cyApplicationManager.selectedNetworks
        selected.each {
            ['direction', 'richness', 'concordance', 'correct', 'contra',
             'ambiguous', 'observed', 'possible', 'rcr.concordance.fill'].
                    each(it.defaultNodeTable.&deleteColumn)
            cyRef.cyNetworkTableManager.setTable(it, CyNode.class, 'sdp.rcr', rcrTable)
        }
    }
}
