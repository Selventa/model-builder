package model.builder.core

import model.builder.ui.SearchModelsPanel
import model.builder.web.api.API
import model.builder.web.api.WebResponse
import org.cytoscape.io.webservice.swing.AbstractWebServiceGUIClient
import org.cytoscape.work.TaskIterator
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.rest.RESTClientException

class BasicSdpModelImport extends AbstractWebServiceGUIClient implements SdpModelImport {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    private final API api
    final Expando cyRef
    final AddBelColumnsToCurrentFactory addBelFac

    BasicSdpModelImport(API api, Expando cyRef,
                        AddBelColumnsToCurrentFactory addBelFac) {
        super('https://janssen-sdp.selventa.com/api',
              'Janssen-SDP (janssen-sdp.selventa.com)',
              'The SDP server for Janssen.')
        this.api = api
        this.cyRef = cyRef
        this.addBelFac = addBelFac
        gui = new SearchModelsPanel(api, this, cyRef.dialogTaskManager)
    }

    @Override
    TaskIterator createTaskIterator(Object o) {
        def tasks = new TaskIterator()
        o.each {
            try {
                WebResponse res = api.model(it.id)
                def model = res.data.model
                def revisionNumber = model.revisions.length() - 1 as Integer
                WebResponse rev = api.modelRevisions(it.id, revisionNumber, '').first()
                tasks.append(new CreateCyNetworkForModelRevision(revisionNumber, rev.data.revision as Map, cyRef))
                tasks.append(addBelFac.createTaskIterator())
                tasks.append(new AddRevisionsTable(model as Map, cyRef))
            } catch (RESTClientException e) {
                msg.error("Error retrieving ${m.name}", e)
            }
        }
        tasks
    }
}
