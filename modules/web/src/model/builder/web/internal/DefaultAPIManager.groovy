package model.builder.web.internal

import model.builder.web.api.API
import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation

class DefaultAPIManager implements APIManager {

    def apiProfiles = [] as Set<AccessInformation>
    def AccessInformation defaultAccess

    @Override
    AccessInformation getDefault() {
        defaultAccess
    }

    @Override
    void setDefault(AccessInformation access) {
        if (! access in apiProfiles)
            throw new IllegalStateException("$access must be added first")
        defaultAccess = access
    }

    @Override
    AccessInformation forHost(String host) {
        apiProfiles.find{it.host = host}
    }

    @Override
    API forAccess(AccessInformation access) {
        new SdpAPI(apiProfiles.find{it == access})
    }

    @Override
    void add(AccessInformation access) {
        apiProfiles.add(access)
    }

    @Override
    void remove(AccessInformation access) {
        apiProfiles.remove(access)
    }
}
