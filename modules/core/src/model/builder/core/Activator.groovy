package model.builder.core

import model.builder.ui.UI
import model.builder.ui.api.Dialogs
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
import org.cytoscape.task.NetworkViewTaskFactory
import org.cytoscape.task.NodeViewTaskFactory
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

import static javax.swing.KeyStroke.getKeyStroke
import static model.builder.core.Util.cyReference

class Activator extends AbstractCyActivator {

    private static final Logger msg = LoggerFactory.getLogger("CyUserMessages")

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        def cyr = cyReference(bc, this.&getService,
            [
                CySwingApplication.class, DialogTaskManager.class,
                CyApplicationManager.class, CyNetworkFactory.class,
                CyNetworkManager.class, CyNetworkViewFactory.class,
                CyNetworkViewManager.class, CyLayoutAlgorithmManager.class,
                VisualMappingManager.class, CyEventHelper.class,
                CyTableFactory.class, CyTableManager.class,
                CyNetworkTableManager.class, ApplyPreferredLayoutTaskFactory.class,
                OpenBrowser.class, MapTableToNetworkTablesTaskFactory.class,
                VisualMappingManager.class, VisualStyleFactory.class
            ] as Class<?>[])

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
        configure.acceleratorKeyStroke = getKeyStroke('control alt O')
        registerService(bc, configure, CyAction.class, [
                id: 'apps_sdp.configure'
        ] as Properties)

        // ... Add Sets > Manage Sets Action
        AbstractCyAction manageSets = new AbstractCyAction('Manage') {
            void actionPerformed(ActionEvent e) {
                UI.manageSets(apiManager)
            }
        }
        manageSets.preferredMenu = 'Apps.SDP.Sets'
        registerService(bc, manageSets, CyAction.class, [
                id: 'apps_sdp.sets.manage'
        ] as Properties)

        registerService(bc,
            new CreateSetFactory(apiManager),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.SDP',
                menuGravity: 11.0,
                title: "Create Set from Selected Nodes"
            ] as Properties)

        // ... Add Pathfind
        Dialogs dialogs = getService(bc, Dialogs.class)
        AbstractCyAction pathfind = new AbstractCyAction('Find Paths') {
            void actionPerformed(ActionEvent e) {
                AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default)
                dialogs.pathSearch(cyr.cyApplicationManager, api, [:], { edges ->
                    def cyN = cyr.cyApplicationManager.currentNetwork
                    edges.collect {
                        [it.source_node.label, it.relationship, it.target_node.label]
                    }.collect { triple -> Util.&getOrCreateEdge.curry(cyN).call(*triple)}
                })
            }
        }
        pathfind.preferredMenu = 'Apps.SDP'
        pathfind.acceleratorKeyStroke = getKeyStroke('control alt P')
        registerService(bc, pathfind, CyAction.class, [
                id: 'apps_sdp.pathfind'
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
        importComparison.acceleratorKeyStroke = getKeyStroke('control alt C')
        registerService(bc, importComparison, CyAction.class, [
                id: 'apps_sdp.data.add_comparison'
        ] as Properties)

        // ... Import Model
        AbstractCyAction importModel = new AbstractCyAction('Import') {
            void actionPerformed(ActionEvent ev) {
                AuthorizedAPI api = apiManager.authorizedAPI(apiManager.default);
                def importModel = { models ->
                    def all = new TaskIterator()
                    try {
                        models.collect {
                            def tasks = new TaskIterator()
                            def context = ['id': it.id]
                            tasks.append(new RetrieveModel(context, apiManager))
                            tasks.append(new RetrieveRevision(context, apiManager))
                            tasks.append(new CreateCyNetworkForModelRevision(context, cyr))
                            tasks.append(addBelFac.createTaskIterator())
                            tasks.append(new AddRevisionsTable(context, cyr))
                            tasks
                        }.each(all.&append)
                        cyr.dialogTaskManager.execute(all)
                    } catch (RESTClientException e) {
                        msg.error("Error retrieving ${it.name}", e)
                    }
                }
                UI.importModel(api, importModel)
            }
        }
        importModel.acceleratorKeyStroke = getKeyStroke('control alt M')
        importModel.preferredMenu = 'Apps.SDP.Models'
        importModel.menuGravity = 101.0
        registerService(bc, importModel, CyAction.class, [
            id: 'apps_sdp.models.import'
        ] as Properties)

        // ... Import Model Revision
        registerService(bc, new ImportRevisionFromMenuFactory(apiManager, cyr, addBelFac),
                NetworkTaskFactory.class, [
                id: 'apps_sdp.models.import_revision',
                preferredMenu: 'Apps.SDP.Models',
                menuGravity: 102.0,
                title: 'Import Revision'
        ] as Properties)

        // ... Save Model (New revision)
        registerService(bc, new SaveModelFactory(cyr, apiManager),
                NetworkViewTaskFactory.class, [
                id: 'apps_sdp.models.save_revision',
                preferredMenu: 'Apps.SDP.Models',
                menuGravity: 103.0,
                title: 'Save Revision',
                accelerator: 'control alt S'
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
        importRCR.acceleratorKeyStroke = getKeyStroke('control alt R')
        registerService(bc, importRCR, CyAction.class, [
                id: 'apps_sdp.data.add_rcr_result'
        ] as Properties)
    }
}
