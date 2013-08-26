package org.openbel.ws

import org.cytoscape.service.util.AbstractCyActivator
import org.openbel.ws.api.API
import org.openbel.ws.internal.SdpAPI
import org.osgi.framework.BundleContext

class Activator extends AbstractCyActivator {

    /**
     * {@inheritDoc}
     */
    @Override
    void start(BundleContext bc) throws Exception {
        API wsAPI = new SdpAPI('https://sdpdemo.selventa.com/api')
        registerService(bc, wsAPI, API.class, [:].asType(Properties.class))
    }
}
