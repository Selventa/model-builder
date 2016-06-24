package model.builder.core

import org.cytoscape.model.CyNode
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
        createColumn(rcrTable, 'direction', String.class, true, null)
        createColumn(rcrTable, 'richness', Double.class, true, null)
        createColumn(rcrTable, 'concordance', Double.class, true, null)
        createColumn(rcrTable, 'correct', Integer.class, true, null)
        createColumn(rcrTable, 'contra', Integer.class, true, null)
        createColumn(rcrTable, 'ambiguous', Integer.class, true, null)
        createColumn(rcrTable, 'observed', Integer.class, true, null)
        createColumn(rcrTable, 'possible', Integer.class, true, null)
        cyRef.cyTableManager.addTable(rcrTable)

        def slink = rcrResult._links.find { it.href.endsWith('scores') }
        if (!slink) return

        def scores = slink.val
        scores.each { JSONObject it ->
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
