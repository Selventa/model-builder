package model.builder.web

import model.builder.common.JsonStream
import model.builder.web.api.APIManager
import model.builder.web.internal.DefaultAPIManager
import org.cytoscape.application.CyApplicationConfiguration
import org.cytoscape.service.util.AbstractCyActivator
import org.openbel.ws.api.WsManager
import org.osgi.framework.BundleContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import static model.builder.common.Constant.setLoggingExceptionHandler

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        CyApplicationConfiguration cyAppConfig = getService(bc, CyApplicationConfiguration.class)
        File cfg = cyAppConfig.getAppConfigurationDirectoryLocation(Activator.class)
        JsonStream.instance.initializeFactory()

        WsManager wsManager = getService(bc, WsManager.class)
        APIManager apiManager = new DefaultAPIManager(cfg, wsManager)
        registerService(bc, apiManager, APIManager.class, [:] as Properties)

        setLoggingExceptionHandler()
    }
}
