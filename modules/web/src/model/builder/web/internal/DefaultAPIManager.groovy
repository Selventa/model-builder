package model.builder.web.internal

import model.builder.web.api.APIManager
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.OpenAPI

class DefaultAPIManager implements APIManager {

    def authorizedAccess = [] as Set<AccessInformation>
    def openMap = [:] as Map<String, OpenAPI>
    def AccessInformation defaultAccess

    @Override
    AccessInformation getDefault() {
        defaultAccess
    }

    @Override
    void setDefault(AccessInformation access) {
        if (! access in authorizedAccess)
            throw new IllegalStateException("$access must be added first")
        defaultAccess = access
    }

    @Override
    AccessInformation authorizedAccess(String host) {
        authorizedAccess.find{it.host = host}
    }

    @Override
    AuthorizedAPI authorizedAPI(AccessInformation access) {
        new DefaultAuthorizedAPI(access)
    }

    @Override
    OpenAPI openAPI(String host) {
        openMap[host] ?: (openMap[host] = new DefaultOpenAPI(host))
    }

    @Override
    void add(AccessInformation access) {
        authorizedAccess.add(access)
    }

    @Override
    void remove(AccessInformation access) {
        authorizedAccess.remove(access)
    }

    @Override
    Set<AccessInformation> all() {
        authorizedAccess
    }
}
