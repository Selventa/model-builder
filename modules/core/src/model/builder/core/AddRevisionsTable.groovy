package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import static model.builder.core.Util.*
import static org.cytoscape.model.SavePolicy.*

@TupleConstructor
class AddRevisionsTable extends AbstractTask {

    final Map model
    final Expando cyRef

    @Override
    void run(TaskMonitor monitor) throws Exception {
        CyTableFactory fac = cyRef.cyTableFactory
        CyTableManager mgr = cyRef.cyTableManager
        def revTable = mgr.getAllTables(false).find{it.title == 'SDP.Revisions'} ?:
                       fac.createTable('SDP.Revisions', 'uri', String.class, true, true)
        revTable.savePolicy = SESSION_FILE
        createListColumn(revTable, 'networks.SUID', Long.class, true, [])
        createColumn(revTable, 'revision', Integer.class, true, null)
        createColumn(revTable, 'who', String.class, true, null)
        createColumn(revTable, 'when', String.class, true, null)
        createColumn(revTable, 'comment', String.class, true, null)
        createColumn(revTable, 'selected', Boolean.class, false, null)
        cyRef.cyTableManager.addTable(revTable)

        def networkSUID = cyRef.cyApplicationManager.currentNetwork.SUID
        model.revisions.sort{}each {
            String uri = it.getString('network')
            def cyRow = revTable.getRow(uri)
            setAdd(cyRow, 'networks.SUID', Long.class, networkSUID)
            cyRow.set('revision', uri[(uri.lastIndexOf('/')+1)..-1] as Integer)
            cyRow.set('who', it.getString('who'))
            cyRow.set('when', it.getString('when'))
            cyRow.set('comment', it.getString('comment'))
        }
    }
}
