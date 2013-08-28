package model.builder.core

import model.builder.ui.SdpModelImportProvider
import model.builder.web.api.API
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.application.swing.CyAction
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.event.CyEventHelper
import org.cytoscape.io.webservice.WebServiceClient
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.util.swing.OpenBrowser
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.swing.DialogTaskManager

import static java.awt.event.KeyEvent.*
import static java.awt.event.InputEvent.*
import static javax.swing.KeyStroke.*

import org.cytoscape.application.swing.AbstractCyAction
import org.cytoscape.service.util.AbstractCyActivator
import org.osgi.framework.BundleContext

import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.event.ActionEvent

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) {
        CySwingApplication cySwingApp = getService(bc, CySwingApplication.class)
        DialogTaskManager taskMgr = getService(bc, DialogTaskManager.class)
        CyApplicationManager appMgr = getService(bc, CyApplicationManager.class)
        CyNetworkFactory cynFac = getService(bc, CyNetworkFactory.class)
        CyNetworkManager cynMgr = getService(bc, CyNetworkManager.class)
        CyNetworkViewFactory cynvFac = getService(bc, CyNetworkViewFactory.class)
        CyNetworkViewManager cynvMgr = getService(bc, CyNetworkViewManager.class)
        CyLayoutAlgorithmManager cylMgr = getService(bc, CyLayoutAlgorithmManager.class)
        VisualMappingManager visMgr = getService(bc, VisualMappingManager.class)
        CyEventHelper evtHelper = getService(bc, CyEventHelper.class)
        ApplyPreferredLayoutTaskFactory aplFac = getService(bc, ApplyPreferredLayoutTaskFactory.class)

        OpenBrowser www = getService(bc, OpenBrowser.class)
        API api = getService(bc, API.class)

        JPanel ui = new JPanel()
        ui.add(new JLabel("UI goes here, bleh."))
        registerAllServices(bc, ui, [:] as Properties)
        registerAllServices(bc, new BasicSdpModelImport(api, taskMgr, appMgr, cynFac, cynvFac, cynMgr, cynvMgr), [:] as Properties)
        SdpModelImportProvider<SdpModelImport> sdpNetworks =
            new SdpModelImportProvider<>(
                    SdpModelImport.class, 'Import Model from SDP',
                    cySwingApp, taskMgr, www)
        registerServiceListener(bc, sdpNetworks, "addClient", "removeClient",
                WebServiceClient.class)

        AbstractCyAction showModelImport = new AbstractCyAction('SDP...') {

            @Override
            void actionPerformed(ActionEvent e) {
                sdpNetworks.prepareForDisplay()
                sdpNetworks.locationRelativeTo = cySwingApp.JFrame
                sdpNetworks.visible = true
            }
        }
        showModelImport.preferredMenu = 'File.Import.Network'
        showModelImport.acceleratorKeyStroke = getKeyStroke(VK_P, ALT_DOWN_MASK)

        registerService(bc, showModelImport, CyAction.class,
                [id: 'showSDPModelImport'] as Properties)
    }
}
