package org.openbel.ws.api

interface API {

    WebResponse model(String id)

    WebResponse models()

    WebResponse models(String name, String... tags)
}