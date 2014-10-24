package model.builder.web.api.event

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import org.cytoscape.event.AbstractCyPayloadEvent

class AddedAccessInformationEvent extends AbstractCyPayloadEvent<APIManager, AccessInformation> {

    AddedAccessInformationEvent(APIManager source, AccessInformation payload) {
        super(source, AddedAccessInformationListener.class, [payload] as Collection<AccessInformation>)
    }
}
