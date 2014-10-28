package model.builder.web.api.event

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import org.cytoscape.event.AbstractCyPayloadEvent

class SetDefaultAccessInformationEvent extends AbstractCyPayloadEvent<APIManager, AccessInformation> {

    SetDefaultAccessInformationEvent(APIManager source,
                                     AccessInformation oldDefault,
                                     AccessInformation newDefault) {
        super(source,
              SetDefaultAccessInformationListener.class,
              [oldDefault, newDefault] as Collection<AccessInformation>)
    }
}
