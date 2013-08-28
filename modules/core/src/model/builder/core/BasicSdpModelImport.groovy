package model.builder.core

import groovy.transform.TupleConstructor
import model.builder.common.Model
import model.builder.ui.SearchModelsPanel
import model.builder.web.api.API
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskManager

@TupleConstructor
class BasicSdpModelImport extends AbstractWebServiceGUIClient implements SdpModelImport {

    private final API api
    final TaskManager taskMgr
    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr

    BasicSdpModelImport(API api, TaskManager taskMgr, CyApplicationManager appMgr,
                        CyNetworkFactory cynFac, CyNetworkViewFactory cynvFac,
                        CyNetworkManager cynMgr, CyNetworkViewManager cynvMgr) {
        super('https://sdptest.selventa.com/api', 'Selventa Development Platform (sdptest.selventa.com)', 'This is a description.')
        this.api = api
        this.taskMgr = taskMgr
        this.appMgr = appMgr
        this.cynFac = cynFac
        this.cynvFac = cynvFac
        this.cynMgr = cynMgr
        this.cynvMgr = cynvMgr
        gui = new SearchModelsPanel(api, this, taskMgr)
    }

    @Override
    TaskIterator createTaskIterator(Object o) {
        if (o instanceof Model) {
            return new TaskIterator(
                new CreateCyNetwork(o, api, appMgr, cynFac, cynvFac, cynMgr, cynvMgr)
            )
        }
        return new TaskIterator(0)
    }
}
