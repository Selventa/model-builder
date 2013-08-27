package model.builder.web.api

interface API {

    WebResponse model(String id)

    WebResponse models()

    WebResponse models(String name, String... tags)
}