package model.builder.core

import groovy.swing.SwingBuilder
import model.builder.common.JsonStream
import model.builder.core.task.AddBelColumnsToCurrentFactoryImpl
import model.builder.ui.SearchTableScrollable
import model.builder.ui.UI
import model.builder.ui.api.Dialogs
import model.builder.ui.internal.DialogsImpl
import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import model.builder.web.internal.DefaultAPIManager
import org.cytoscape.application.CyApplicationConfiguration
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.application.swing.AbstractCyAction
import org.cytoscape.application.swing.CyAction
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.event.CyEventHelper
import org.cytoscape.io.read.InputStreamTaskFactory
import org.cytoscape.io.util.StreamUtil
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
import org.cytoscape.task.read.LoadVizmapFileTaskFactory
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
import org.jdesktop.swingx.JXList
import org.jdesktop.swingx.JXTable
import org.jdesktop.swingx.JXTaskPane
import org.jdesktop.swingx.JXTaskPaneContainer
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.rest.RESTClientException

import java.awt.event.ActionEvent

import static javax.swing.KeyStroke.getKeyStroke
import static model.builder.common.Constant.setLoggingExceptionHandler
import static model.builder.core.Util.cyReference
import static model.builder.core.Util.contributeVisualStyles
import static model.builder.ui.MessagePopups.errorAccessNotSet;

class Activator extends AbstractCyActivator {

    public static SwingBuilder swing

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
                    VisualMappingManager.class, VisualStyleFactory.class,
                    CyApplicationConfiguration.class, LoadVizmapFileTaskFactory.class
            ] as Class<?>[])

        VisualMappingFunctionFactory dMapFac = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        VisualMappingFunctionFactory pMapFac = getService(bc,VisualMappingFunctionFactory.class, "(mapping.type=passthrough)");
        AddBelColumnsToCurrentFactory addBelFac = new AddBelColumnsToCurrentFactoryImpl(cyr)

        File cfg = cyr.cyApplicationConfiguration.getAppConfigurationDirectoryLocation(Activator.class)
        JsonStream.instance.initializeFactory()

        APIManager apiManager = new DefaultAPIManager(cfg)

        // Reader / Writer for Model Json format
        StreamUtil util = new StreamUtilImpl()
        registerService(bc, JsonNetworkReaderFactory.create(cyr, util), InputStreamTaskFactory.class,
            [readerId: 'modelJSONReader', readerDescription: 'Model JSON reader'] as Properties)
        registerAllServices(bc, JsonNetworkWriterFactory.create(util), [:] as Properties)

        // Styles
        contributeVisualStyles(cyr.visualMappingManager, cyr.loadVizmapFileTaskFactory)

        // ... Apps > SDP Menu Actions ...

        // ... Add Comparison
        AbstractCyAction importComparison = new AbstractCyAction('Add Comparison') {
            void actionPerformed(ActionEvent e) {
                AuthorizedAPI api = apiManager.byAccess(apiManager.default);
                if (!api) {
                    errorAccessNotSet()
                    return
                }
                def importData = { id ->
                    WebResponse res = api.comparison(id)
                    cyr.dialogTaskManager.execute(
                            new AddComparisonTableFactory(res.data, cyr).createTaskIterator())
                }
                UI.addComparison(api, importData)
            }
        }
        importComparison.menuGravity = 0.0
        importComparison.preferredMenu = 'Apps.SDP.Data'
        importComparison.acceleratorKeyStroke = getKeyStroke('control alt C')
        registerService(bc, importComparison, CyAction.class, [
                id: 'apps_sdp.data.add_comparison'
        ] as Properties)

        // ... Add RCR Result
        AbstractCyAction importRCR = new AbstractCyAction('Add RCR Result') {
            void actionPerformed(ActionEvent e) {
                AuthorizedAPI api = apiManager.byAccess(apiManager.default);
                if (!api) {
                    errorAccessNotSet()
                    return
                }
                def importData = { id ->
                    WebResponse res = api.rcrResult(id)
                    cyr.dialogTaskManager.execute(
                            new AddRcrResultTableFactory(res.data, cyr, dMapFac, pMapFac).createTaskIterator())
                }
                UI.addRcr(api, importData)
            }
        }
        importRCR.menuGravity = 0.0
        importRCR.preferredMenu = 'Apps.SDP.Data'
        importRCR.acceleratorKeyStroke = getKeyStroke('control alt R')
        registerService(bc, importRCR, CyAction.class, [
                id: 'apps_sdp.data.add_rcr_result'
        ] as Properties)

        // ... Import Model
        AbstractCyAction importModel = new AbstractCyAction('Import') {
            void actionPerformed(ActionEvent ev) {
                AuthorizedAPI api = apiManager.byAccess(apiManager.default);
                if (!api) {
                    errorAccessNotSet()
                    return
                }

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

        // ... Save (New revision)
        registerService(bc, new SaveModelFactory(cyr, apiManager),
                NetworkViewTaskFactory.class, [
                id: 'apps_sdp.models.save_revision',
                preferredMenu: 'Apps.SDP.Models',
                menuGravity: 103.0,
                title: 'Save',
                accelerator: 'control alt S'
        ] as Properties)

        // ... Save as (New model)
        registerService(bc, new SaveAsNewModelFactory(cyr, apiManager),
                NetworkViewTaskFactory.class, [
                id: 'apps_sdp.models.save_as_new_model',
                preferredMenu: 'Apps.SDP.Models',
                menuGravity: 103.0,
                title: 'Save As',
                accelerator: 'control alt shift S'
        ] as Properties)

        // Dialogs
        swing = new SwingBuilder()
        swing.registerBeanFactory('taskPaneContainer', JXTaskPaneContainer.class)
        swing.registerBeanFactory('taskPane', JXTaskPane.class)
        swing.registerBeanFactory('jxList', JXList.class)
        swing.registerBeanFactory('jxTable', JXTable.class)
        swing.registerBeanFactory('searchTableScrollable', SearchTableScrollable.class)
        Dialogs dialogs = new DialogsImpl(swing)

        // ... Add Configure
        AbstractCyAction configure = new AbstractCyAction('Configure') {
            void actionPerformed(ActionEvent e) {
                UI.configuration(apiManager,
                    { host, email, pass ->
                        def res = apiManager.openAPI(host).apiKeys(email)
                        if (res?.statusCode != 200) return null

                        String apiKey = res.data.find {String k -> k.startsWith('api:')}
                        if (!apiKey) return null

                        AccessInformation access = new AccessInformation(false, host, email, apiKey, pass)
                        AuthorizedAPI authAPI = apiManager.byAccess(access)
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
        configure.menuGravity = 100.0
        configure.preferredMenu = 'Apps.SDP'
        configure.acceleratorKeyStroke = getKeyStroke('control alt O')
        registerService(bc, configure, CyAction.class, [
                id: 'apps_sdp.configure'
        ] as Properties)

        // ... Add Find Paths
        AbstractCyAction findPaths = new AbstractCyAction('Find Paths') {
            void actionPerformed(ActionEvent e) {
                AuthorizedAPI api = apiManager.byAccess(apiManager.default)
                if (!api) {
                    errorAccessNotSet()
                    return
                }
                dialogs.pathSearch(cyr.cyApplicationManager, api, [:], { edges ->
                    def cyN = cyr.cyApplicationManager.currentNetwork
                    edges.collect {
                        [it.source_node.label, it.relationship, it.target_node.label]
                    }.collect { triple -> Util.&getOrCreateEdge.curry(cyN).call(*triple)}
                })
            }
        }
        findPaths.menuGravity = 110.0
        findPaths.preferredMenu = 'Apps.SDP'
        findPaths.acceleratorKeyStroke = getKeyStroke('control alt P')
        registerService(bc, findPaths, CyAction.class, [
                id: 'apps_sdp.find_paths'
        ] as Properties)

        // ... Add Manage Sets Action
        AbstractCyAction manageSets = new AbstractCyAction('Manage Sets') {
            void actionPerformed(ActionEvent e) {
                UI.manageSets(apiManager)
            }
        }
        manageSets.preferredMenu = 'Apps.SDP'
        manageSets.menuGravity = 120.0
        manageSets.acceleratorKeyStroke = getKeyStroke('control alt T')
        registerService(bc, manageSets, CyAction.class, [
                id: 'apps_sdp.manage_sets'
        ] as Properties)

        registerService(bc,
            new CreateSetFactory(apiManager),
            NodeViewTaskFactory.class, [
                preferredMenu: 'Apps.SDP',
                menuGravity: 11.0,
                title: "Create Set from Selected Nodes"
            ] as Properties)

        setLoggingExceptionHandler()
    }
}
