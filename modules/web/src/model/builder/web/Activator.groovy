package model.builder.web

import model.builder.web.api.APIManager
import model.builder.web.internal.DefaultAPIManager
import model.builder.web.internal.JsonStream
import org.cytoscape.application.CyApplicationConfiguration
import org.cytoscape.service.util.AbstractCyActivator
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        CyApplicationConfiguration cyAppConfig = getService(bc, CyApplicationConfiguration.class)
        File cfg = cyAppConfig.getAppConfigurationDirectoryLocation(Activator.class)
        JsonStream.instance.initializeFactory()
        APIManager apiManager = new DefaultAPIManager(cfg)
        registerService(bc, apiManager, APIManager.class, [:] as Properties)
    }
}
