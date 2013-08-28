package model.builder.core

import model.builder.ui.SearchModelsPanel
import model.builder.web.api.API
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient
import org.cytoscape.work.TaskIterator

class BasicSdpModelImport extends AbstractWebServiceGUIClient implements SdpModelImport {
    BasicSdpModelImport(API api) {
        super('https://sdptest.selventa.com/api', 'Selventa Development Platform (sdptest.selventa.com)', 'This is a description.')
        gui = new SearchModelsPanel(api)
    }

    @Override
    TaskIterator createTaskIterator(Object o) {
        return new TaskIterator()
    }
}
