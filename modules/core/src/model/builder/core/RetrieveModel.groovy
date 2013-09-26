package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.web.api.APIManager
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.work.Task
import org.cytoscape.work.TaskMonitor

@TupleConstructor
class RetrieveModel implements Task {

    final Map context
    final APIManager apiManager

    @Override
    void run(TaskMonitor monitor) throws Exception {
        AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default)

        WebResponse res = api.model(context['id'] as String)
        context['model'] = res.data.model as Map
    }

    @Override
    void cancel() {
    }
}
