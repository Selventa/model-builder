package model.builder.web.api

interface APIManager {

    AccessInformation getDefault()

    //AuthorizedAPI authorizedAPI(String host)

    AuthorizedAPI authorizedAPI(AccessInformation access)

    OpenAPI openAPI(String host)

    void add(AccessInformation access)

    void remove(AccessInformation access)

    Set<AccessInformation> all()

    void saveConfiguration(Set<AccessInformation> accessSet)
}