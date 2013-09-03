package model.builder.web

import model.builder.web.api.API
import model.builder.web.internal.SdpAPI
import org.cytoscape.service.util.AbstractCyActivator
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        API wsAPI = new SdpAPI('https://janssen-sdp.selventa.com')
        registerService(bc, wsAPI, API.class, [:].asType(Properties.class))
    }
}
