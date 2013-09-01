package model.builder.web.api

interface API {

    String uri(Map data)

    String id(Map data)

    WebResponse model(String id)

    WebResponse models()

    WebResponse searchModels(Map data)

    WebResponse tags(types)

    WebResponse[] modelRevisions(id, revision, uri)
}