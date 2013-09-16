package model.builder.web.internal

import model.builder.web.api.OpenAPI
import model.builder.web.api.WebResponse
import wslite.http.HTTPClientException
import wslite.http.HTTPResponse
import wslite.rest.RESTClient
import wslite.rest.Response

import javax.net.ssl.SSLContext

class DefaultOpenAPI implements OpenAPI {

    def RESTClient client

    DefaultOpenAPI(String host) {
        SSLContext.default = SSL.context

        String uri = "https://${host}"
        new URI(uri).host

        client = new RESTClient(uri)
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
        HTTPResponse.metaClass.define {
            old = HTTPResponse.metaClass.getMetaMethod("asType", [Class] as Class[])
            asType = { Class c ->
                if (c == WebResponse)
                    new WebResponse(
                            delegate.statusCode,
                            delegate.statusMessage,
                            delegate.contentType,
                            delegate.charset,
                            delegate.headers
                    )
                else
                    oldAsType.invoke(delegate, c)
            }
        }
    }

    @Override
    WebResponse apiKeys(String email) {
        try {
            client.get(path: "/api/apikeys/users/$email") as WebResponse
        } catch (HTTPClientException e) {
            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }
}