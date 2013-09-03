package model.builder.core

import org.cytoscape.model.CyNode
import wslite.json.JSONObject

import static org.cytoscape.model.CyNetwork.NAME
import groovy.transform.TupleConstructor
import org.cytoscape.model.CyTable
import org.cytoscape.model.CyTableFactory
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor

import static model.builder.core.Util.createColumn

@TupleConstructor
class AddRcrResultTable extends AbstractTask {

    final Map rcrResult
    final Expando cyRef

    @Override
    void run(TaskMonitor monitor) throws Exception {
        CyTableFactory tableFac = cyRef.cyTableFactory
        CyTable rcrTable = tableFac.createTable(
                "RCR Scores - ${rcrResult.name}", NAME, String.class, true, false)

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
             'ambiguous', 'observed', 'possible'].each(it.defaultNodeTable.&deleteColumn)
        }

        def mapTblFactory = cyRef.mapTableToNetworkTablesTaskFactory
        super.insertTasksAfterCurrentTask(mapTblFactory.createTaskIterator(
                rcrTable, true, selected, CyNode.class))
    }
}
