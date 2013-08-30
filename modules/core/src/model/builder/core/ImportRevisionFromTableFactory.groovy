package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.API
import model.builder.web.api.WebResponse
import org.cytoscape.model.CyColumn
import org.cytoscape.task.TableCellTaskFactory
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory

@TupleConstructor
class ImportRevisionFromTableFactory implements TableCellTaskFactory {

    final API api
    final Expando cyRef
    final AddBelColumnsToCurrentFactory addBelFac

    @Override
    TaskIterator createTaskIterator(CyColumn col, Object key) {
        def uri = col.table.getRow(key).get('uri', String.class)
        WebResponse rev = api.modelRevisions(null, null, uri).first()
        TaskIterator tasks = new TaskIterator(
            new CreateCyNetworkForModelRevision(key as int, rev.data.revision as Map, cyRef),
        )
        tasks.append(addBelFac.createTaskIterator())
        tasks
    }

    @Override
    boolean isReady(CyColumn col, Object key) {
        def t = col.table
        t.getPrimaryKey().name == 'REVISION' && t.getRow(key).isSet('uri')
    }
}
