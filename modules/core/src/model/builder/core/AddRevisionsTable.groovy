package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.API
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyTable
import org.cytoscape.model.CyTableFactory
import org.cytoscape.work.AbstractTask
import org.cytoscape.work.TaskMonitor
import static model.builder.core.Util.createColumn

@TupleConstructor
class AddRevisionsTable extends AbstractTask {

    final Map model
    final API api
    final Expando cyRef

    @Override
    void run(TaskMonitor monitor) throws Exception {
        CyTableFactory tableFac = cyRef.cyTableFactory
        CyTable revTable = tableFac.createTable(
                "Revisions - ${model.name}", 'REVISION', Integer.class, true, false)
        createColumn(revTable, 'who', String.class, true)
        createColumn(revTable, 'when', String.class, true)
        createColumn(revTable, 'comment', String.class, true)
        createColumn(revTable, 'uri', String.class, true)
        createColumn(revTable, 'selected', Boolean.class, false)
        cyRef.cyTableManager.addTable(revTable)

        def rev = model.revisions.length() - 1
        model.revisions.each {
            def cyRow = revTable.getRow(rev)
            cyRow.set('who', it.getString('who'))
            cyRow.set('when', it.getString('when'))
            cyRow.set('comment', it.getString('comment'))
            cyRow.set('uri', api.uri(path: "/api/models/${model.id}/revisions/$rev"))
            rev--
        }

        def network = cyRef.cyApplicationManager.currentNetwork
        cyRef.cyNetworkTableManager.setTable(network, CyNetwork.class, 'sdp.revisions', revTable)
    }
}
