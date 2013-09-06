package model.builder.web

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.internal.DefaultAPIManager
import org.cytoscape.service.util.AbstractCyActivator
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        def access = new AccessInformation('janssen-sdp.selventa.com',
            'abargnesi@selventa.com', 'api:abargnesi@selventa.com', 'superman')
        APIManager apiManager = new DefaultAPIManager()
        apiManager.add(access)
        apiManager.setDefault(access)
        registerService(bc, apiManager, APIManager.class, [:] as Properties)
    }
}
