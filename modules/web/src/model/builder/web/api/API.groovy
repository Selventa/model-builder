package model.builder.web.api

interface API {

    WebResponse model(String id)

    WebResponse models()

    WebResponse searchModels(Map data)
}