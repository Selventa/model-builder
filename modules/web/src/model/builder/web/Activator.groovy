package model.builder.web

import model.builder.common.JsonStream
import model.builder.web.api.APIManager
import model.builder.web.internal.DefaultAPIManager
import org.cytoscape.application.CyApplicationConfiguration
import org.cytoscape.event.CyEventHelper
import org.cytoscape.service.util.AbstractCyActivator
import org.openbel.ws.api.WsManager
import org.osgi.framework.BundleContext

import static model.builder.common.Util.cyReference;
import static model.builder.common.Constant.setLoggingExceptionHandler

class Activator extends AbstractCyActivator {

    static Expando CY;

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        CY = cyReference(bc, this.&getService,
                [
                        CyEventHelper.class,
                        CyApplicationConfiguration.class
                ] as Class<?>[]
        )
        File cfg = CY.cyApplicationConfiguration.getAppConfigurationDirectoryLocation(Activator.class)
        JsonStream.instance.initializeFactory()

        WsManager wsManager = getService(bc, WsManager.class)
        APIManager apiManager = new DefaultAPIManager(cfg, CY.cyEventHelper, wsManager)
        registerService(bc, apiManager, APIManager.class, [:] as Properties)

        setLoggingExceptionHandler()
    }
}
