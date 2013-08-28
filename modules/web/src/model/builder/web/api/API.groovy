package model.builder.web.api

interface API {

    String uri(String id)

    WebResponse model(String id)

    WebResponse models()

    WebResponse searchModels(Map data)

    WebResponse modelTags()
}