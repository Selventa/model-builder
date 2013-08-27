package model.builder.core

import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient
import org.cytoscape.work.TaskIterator

class BasicSdpModelImport extends AbstractWebServiceGUIClient implements SdpModelImport {
    BasicSdpModelImport() {
        super('https://sdptest.selventa.com/api', 'Selventa Development Platform (sdptest.selventa.com)', '')
    }

    @Override
    TaskIterator createTaskIterator(Object o) {
        String
        return null
    }
}
