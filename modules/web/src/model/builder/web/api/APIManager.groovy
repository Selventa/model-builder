package model.builder.web.api

interface APIManager {

    AccessInformation getDefault()

    void setDefault(AccessInformation access)

    AccessInformation forHost(String host)

    API forAccess(AccessInformation access)

    void add(AccessInformation access)

    void remove(AccessInformation access)
}