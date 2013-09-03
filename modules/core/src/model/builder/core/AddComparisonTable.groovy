package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyNode
import org.cytoscape.model.CyTable
import org.cytoscape.model.CyTableFactory
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import wslite.json.JSONObject

import static model.builder.core.Util.createColumn
import static org.cytoscape.model.CyNetwork.NAME

@TupleConstructor
class AddComparisonTable extends AbstractTask {

    final Map comparison
    final Expando cyRef

    @Override
    void run(TaskMonitor monitor) throws Exception {
        CyTableFactory tableFac = cyRef.cyTableFactory
        CyTable cmpTable = tableFac.createTable(
                "Comparison Measurements - ${comparison.name}",
                NAME, String.class, true, false)

        cyRef.cyTableManager.addTable(cmpTable)
        createColumn(cmpTable, 'abundance', Double.class, true)
        createColumn(cmpTable, 'fold_change', Double.class, true)
        createColumn(cmpTable, 'p_value', Double.class, true)

        comparison.measurements.each { JSONObject it ->
            def cyRow = cmpTable.getRow(it.getString('id'))
            cyRow.set('abundance', it.getDouble('abundance'))
            cyRow.set('fold_change', it.getDouble('fold_change'))
            cyRow.set('p_value', it.getDouble('p_value'))
        }

        def network = cyRef.cyApplicationManager.currentNetwork
        cyRef.cyNetworkTableManager.setTable(network, CyNetwork.class, "sdp.comparisons", cmpTable)

        cyRef.cyNetworkManager.networkSet.each {
            ['abundance', 'fold_change', 'p_value'].each(it.defaultNodeTable.&deleteColumn)
        }

        def mapTblFactory = cyRef.mapTableToNetworkTablesTaskFactory
        super.insertTasksAfterCurrentTask(mapTblFactory.createTaskIterator(
                cmpTable, false, cyRef.cyNetworkManager.networkSet as List,
                CyNode.class))
    }
}
