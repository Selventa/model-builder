package model.builder.core

import model.builder.ui.UI
import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.application.swing.AbstractCyAction
import org.cytoscape.application.swing.CyAction
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.event.CyEventHelper
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyNetworkTableManager
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.NetworkTaskFactory
import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.util.swing.OpenBrowser
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.view.vizmap.VisualStyleFactory
import org.cytoscape.work.TaskIterator
import org.cytoscape.work.swing.DialogTaskManager
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.rest.RESTClientException

import java.awt.event.ActionEvent

import static model.builder.core.Util.cyReference

class Activator extends AbstractCyActivator {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        def cyr = cyReference(bc, this.&getService,
                CySwingApplication.class, DialogTaskManager.class, CyApplicationManager.class,
                CyNetworkFactory.class, CyNetworkManager.class, CyNetworkViewFactory.class,
                CyNetworkViewManager.class, CyLayoutAlgorithmManager.class,
                VisualMappingManager.class, CyEventHelper.class, CyTableFactory.class,
                CyTableManager.class, CyNetworkTableManager.class,
                ApplyPreferredLayoutTaskFactory.class, OpenBrowser.class,
                MapTableToNetworkTablesTaskFactory.class, VisualMappingManager.class,
                VisualStyleFactory.class)

        VisualMappingFunctionFactory dMapFac = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        VisualMappingFunctionFactory pMapFac = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
        AddBelColumnsToCurrentFactory addBelFac = getService(bc, AddBelColumnsToCurrentFactory.class)
        APIManager apiManager = getService(bc, APIManager.class)
        registerAllServices(bc, new Listener(cyr), [:] as Properties)

        // ... Apps > SDP Menu Actions ...

        // ... Add Configure
        AbstractCyAction configure = new AbstractCyAction('Configure') {
            void actionPerformed(ActionEvent e) {
                UI.configuration(apiManager,
                    { host, email, pass ->
                        def res = apiManager.openAPI(host).apiKeys(email)
                        if (res.statusCode == 404) return null
                        String apiKey = res.data.api_keys.find {String k -> k.startsWith('api:')}
                        if (!apiKey) return null

                        AccessInformation access = new AccessInformation(false, host, email, apiKey, pass)
                        AuthorizedAPI authAPI = apiManager.authorizedAPI(access)
                        res = authAPI.user(email)
                        switch(res.statusCode) {
                            case 200:
                                return apiKey
                            case 401:
                            case 404:
                                return null
                        }
                    },
                    apiManager.&saveConfiguration)
            }
        }
        configure.preferredMenu = 'Apps.SDP'
        registerService(bc, configure, CyAction.class, [
                id: 'apps_sdp.configure'
        ] as Properties)

        // ... Add Comparison
        AbstractCyAction importComparison = new AbstractCyAction('Add Comparison') {
            void actionPerformed(ActionEvent e) {
                AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default);
                def importData = { id ->
                    WebResponse res = api.comparison(id)
                    cyr.dialogTaskManager.execute(
                            new AddComparisonTableFactory(res.data.comparison, cyr).createTaskIterator())
                }
                UI.addComparison(api, importData)
            }
        }
        importComparison.menuGravity = 100.0
        importComparison.preferredMenu = 'Apps.SDP.Data'
        registerService(bc, importComparison, CyAction.class, [
                id: 'apps_sdp.import_comparison'
        ] as Properties)

        // ... Import Model
        AbstractCyAction importModel = new AbstractCyAction('Import Models') {
            void actionPerformed(ActionEvent ev) {
                AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default);
                def importModel = {
                    def tasks = new TaskIterator()
                    try {
                        WebResponse res = api.model(it.id)
                        def model = res.data.model
                        def revisionNumber = model.revisions.length() - 1 as Integer
                        WebResponse rev = api.modelRevisions(it.id, revisionNumber, '').first()
                        tasks.append(new CreateCyNetworkForModelRevision(revisionNumber, rev.data.revision as Map, cyr))
                        tasks.append(addBelFac.createTaskIterator())
                        tasks.append(new AddRevisionsTable(model as Map, cyr))
                        cyr.dialogTaskManager.execute(tasks)
                    } catch (RESTClientException e) {
                        msg.error("Error retrieving ${it.name}", e)
                    }
                }
                UI.importModel(api, importModel)
            }
        }
        importModel.preferredMenu = 'Apps.SDP.Models'
        importModel.menuGravity = 101.0
        registerService(bc, importModel, CyAction.class, [
            id: 'apps_sdp.import_model'
        ] as Properties)

        // ... Import Model Revision
        registerService(bc, new ImportRevisionFromMenuFactory(apiManager, cyr, addBelFac),
                NetworkTaskFactory.class, [
                preferredMenu: 'Apps.SDP.Models',
                menuGravity: 102.0,
                title: 'Import Model Revision'
        ] as Properties)

        // ... Import RCR Result
        AbstractCyAction importRCR = new AbstractCyAction('Add RCR Result') {
            void actionPerformed(ActionEvent e) {
                AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default);
                def importData = { id ->
                    WebResponse res = api.rcrResult(id)
                    cyr.dialogTaskManager.execute(
                            new AddRcrResultTableFactory(res.data.rcr_result, cyr, dMapFac, pMapFac).createTaskIterator())
                }
                UI.addRcr(api, importData)
            }
        }
        importRCR.menuGravity = 103.0
        importRCR.preferredMenu = 'Apps.SDP.Data'
        registerService(bc, importRCR, CyAction.class, [
                id: 'apps_sdp.import_rcr'
        ] as Properties)
    }
}
