package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.work.Task
import org.cytoscape.work.TaskMonitor
import static model.builder.ui.MessagePopups.errorAccessNotSet

@TupleConstructor
class RetrieveRevision implements Task {

    final Map context
    final APIManager apiManager

    @Override
    void run(TaskMonitor monitor) throws Exception {
        AuthorizedAPI api = apiManager.byAccess(apiManager.default)
        if (!api) {
            errorAccessNotSet()
            return
        }

        def id = context.id
        def model = context.model

        def rev = model.revisions.length() - 1 as Integer
        WebResponse res = api.modelRevisions(id, rev, '').first()

        context.revisionNumber = rev
        context.revision = res.data.revision as Map
    }

    @Override
    void cancel() {
    }
}
