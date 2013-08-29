package model.builder.web.api

interface API {

    String uri(String id)

    String id(String str)

    WebResponse model(String id)

    WebResponse models()

    WebResponse searchModels(Map data)

    WebResponse modelTags()

    WebResponse[] modelRevisions(id, revision)
}