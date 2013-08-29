package model.builder.core

import model.builder.common.Model
import model.builder.ui.SearchModelsPanel
import model.builder.web.api.API
import model.builder.web.api.WebResponse
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.TaskManager
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.rest.RESTClientException

class BasicSdpModelImport extends AbstractWebServiceGUIClient implements SdpModelImport {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    private final API api
    final TaskManager taskMgr
    final CyApplicationManager appMgr
    final CyNetworkFactory cynFac
    final CyNetworkViewFactory cynvFac
    final CyNetworkManager cynMgr
    final CyNetworkViewManager cynvMgr
    final AddBelColumnsToCurrentFactory addBelFac

    BasicSdpModelImport(API api, TaskManager taskMgr, CyApplicationManager appMgr,
                        CyNetworkFactory cynFac, CyNetworkViewFactory cynvFac,
                        CyNetworkManager cynMgr, CyNetworkViewManager cynvMgr,
                        AddBelColumnsToCurrentFactory addBelFac) {
        super('https://sdptest.selventa.com/api', 'Selventa Development Platform (sdptest.selventa.com)', 'This is a description.')
        this.api = api
        this.taskMgr = taskMgr
        this.appMgr = appMgr
        this.cynFac = cynFac
        this.cynvFac = cynvFac
        this.cynMgr = cynMgr
        this.cynvMgr = cynvMgr
        this.addBelFac = addBelFac
        gui = new SearchModelsPanel(api, this, taskMgr)
    }

    @Override
    TaskIterator createTaskIterator(Object o) {
        if (o instanceof Model) {
            Model m = o
            try {
                WebResponse res = api.model(m.id)
                def model = res.data.model
                def tasks = new TaskIterator(
                        new CreateCyNetwork(model as Map, api, appMgr, cynFac, cynvFac, cynMgr, cynvMgr)
                )
                tasks.append(addBelFac.createTaskIterator())
                return tasks
            } catch (RESTClientException e) {
                msg.error("Error retrieving ${m.name}", e)
                return new TaskIterator(0)
            }
        }
        return new TaskIterator(0)
    }
}
