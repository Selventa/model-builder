package org.openbel.ws.internal

import groovy.json.JsonOutput
import org.openbel.ws.api.API
import org.openbel.ws.api.WebResponse
import wslite.rest.RESTClient
import wslite.rest.Response

import javax.net.ssl.SSLContext

class SdpAPI implements API {

    def RESTClient client

    SdpAPI(String uri) {
        SSLContext.default = SSL.context

        client = new RESTClient(uri)
        client.requestBuilder = new AuthdRequestBuilder('test@sdpdemo.selventa.com', 'test')
        client.defaultAcceptHeader = 'application/json'
        client.defaultContentTypeHeader = 'application/json'
        client.defaultCharset = 'UTF-8'
        client.httpClient.followRedirects = true

        Response.metaClass.define {
            old = Response.metaClass.getMetaMethod("asType", [Class] as Class[])
            asType = { Class c ->
                if (c == WebResponse)
                    new WebResponse(
                            delegate.statusCode,
                            delegate.statusMessage,
                            delegate.contentType,
                            delegate.charset,
                            delegate.headers,
                            delegate.json as Map
                    )
                else
                    oldAsType.invoke(delegate, c)
            }
        }
    }

    @Override
    WebResponse model(String id) {
        client.get(path: "/models/$id") as WebResponse
    }

    @Override
    WebResponse models() {
        client.get(path: '/models') as WebResponse
    }

    @Override
    WebResponse models(String name, String... tags) {
        return null
    }

    static void main(String[] args) {
        API api = new SdpAPI('https://sdpdemo.selventa.com/api')
        println JsonOutput.prettyPrint(JsonOutput.toJson(api.models().data))
        println JsonOutput.prettyPrint(JsonOutput.toJson(api.model('51d9832442bc1d0619a4c44c').data))
    }
}
