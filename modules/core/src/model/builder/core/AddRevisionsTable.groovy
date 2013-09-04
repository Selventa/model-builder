package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.API
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import static model.builder.core.Util.*

@TupleConstructor
class AddRevisionsTable extends AbstractTask {

    final Map model
    final API api
    final Expando cyRef

    @Override
    void run(TaskMonitor monitor) throws Exception {
        CyTableFactory fac = cyRef.cyTableFactory
        CyTableManager mgr = cyRef.cyTableManager
        def revTable = mgr.getAllTables(false).find{it.title == 'SDP.Revisions'} ?:
                       fac.createTable('SDP.Revisions', 'uri', String.class, true, false)
        createColumn(revTable, 'who', String.class, true, null)
        createColumn(revTable, 'when', String.class, true, null)
        createColumn(revTable, 'comment', String.class, true, null)
        createListColumn(revTable, 'networks.SUID', Long.class, true, [])
        createColumn(revTable, 'selected', Boolean.class, false, null)
        cyRef.cyTableManager.addTable(revTable)

        def networkSUID = cyRef.cyApplicationManager.currentNetwork.SUID
        def rev = model.revisions.length() - 1
        model.revisions.each {
            def uri = api.uri(path: "/api/models/${model.id}/revisions/${rev}")
            def cyRow = revTable.getRow(uri)
            setAdd(cyRow, 'networks.SUID', Long.class, networkSUID)
            cyRow.set('who', it.getString('who'))
            cyRow.set('when', it.getString('when'))
            cyRow.set('comment', it.getString('comment'))
            rev--
        }
    }
}
