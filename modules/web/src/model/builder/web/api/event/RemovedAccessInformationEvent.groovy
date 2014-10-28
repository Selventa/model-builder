package model.builder.web.api.event

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import org.cytoscape.event.AbstractCyPayloadEvent

class RemovedAccessInformationEvent extends AbstractCyPayloadEvent<APIManager, AccessInformation> {

    RemovedAccessInformationEvent(APIManager source, AccessInformation payload) {
        super(source, RemovedAccessInformationListener.class, [payload] as Collection<AccessInformation>)
    }
}
