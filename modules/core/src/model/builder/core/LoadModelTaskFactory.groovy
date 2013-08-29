package model.builder.core

import model.builder.web.api.API
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.task.AddBelColumnsToCurrent

class LoadModelTaskFactory implements ModelTaskFactory {

    private final API api
    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr

    @Override
    boolean isReady(Map model) {
        model.every {it.key in ['uri', 'name', 'revisions']}
    }

    @Override
    TaskIterator createTaskIterator(Map model) {
        return new TaskIterator(
                new CreateCyNetwork(model, api, appMgr, cynFac, cynvFac, cynMgr, cynvMgr),
                new AddBelColumnsToCurrent(appMgr)
        )
    }
}
