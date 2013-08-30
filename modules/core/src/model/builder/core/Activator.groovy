package model.builder.core

import model.builder.ui.SdpModelImportProvider
import model.builder.web.api.API
import org.cytoscape.application.CyApplicationManager
import org.cytoscape.application.swing.AbstractCyAction
import org.cytoscape.application.swing.CyAction
import org.cytoscape.application.swing.CySwingApplication
import org.cytoscape.event.CyEventHelper
import org.cytoscape.io.webservice.WebServiceClient
import org.cytoscape.model.CyNetworkFactory
import org.cytoscape.model.CyNetworkManager
import org.cytoscape.model.CyNetworkTableManager
import org.cytoscape.model.CyTableFactory
import org.cytoscape.model.CyTableManager
import org.cytoscape.service.util.AbstractCyActivator
import org.cytoscape.task.visualize.ApplyPreferredLayoutTaskFactory
import org.cytoscape.util.swing.OpenBrowser
import org.cytoscape.view.layout.CyLayoutAlgorithmManager
import org.cytoscape.view.model.CyNetworkViewFactory
import org.cytoscape.view.model.CyNetworkViewManager
import org.cytoscape.view.vizmap.VisualMappingManager
import org.cytoscape.work.swing.DialogTaskManager
import org.openbel.kamnav.core.AddBelColumnsToCurrentFactory
import org.osgi.framework.BundleContext

import java.awt.event.ActionEvent

import static java.awt.event.InputEvent.ALT_DOWN_MASK
import static java.awt.event.KeyEvent.VK_P
import static javax.swing.KeyStroke.getKeyStroke
import static model.builder.core.Util.cyReference

class Activator extends AbstractCyActivator {

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
                ApplyPreferredLayoutTaskFactory.class, OpenBrowser.class)

        AddBelColumnsToCurrentFactory addBelFac = getService(bc, AddBelColumnsToCurrentFactory.class)
        API api = getService(bc, API.class)

        registerAllServices(bc, new BasicSdpModelImport(api, cyr, addBelFac), [:] as Properties)
        SdpModelImportProvider<SdpModelImport> sdpNetworks =
            new SdpModelImportProvider<>(SdpModelImport.class, 'Import Model', cyr)
        registerServiceListener(bc, sdpNetworks, "addClient", "removeClient",
                WebServiceClient.class)

        AbstractCyAction showModelImport = new AbstractCyAction('SDP...') {
            void actionPerformed(ActionEvent e) {
                sdpNetworks.prepareForDisplay()
                sdpNetworks.locationRelativeTo = cyr.cySwingApplication.JFrame
                sdpNetworks.visible = true
            }
        }
        showModelImport.preferredMenu = 'File.Import.Network'
        showModelImport.acceleratorKeyStroke = getKeyStroke(VK_P, ALT_DOWN_MASK)

        registerService(bc, showModelImport, CyAction.class,
                [id: 'showSDPModelImport'] as Properties)
    }
}
