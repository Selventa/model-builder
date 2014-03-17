package model.builder.web.internal

import model.builder.web.api.OpenAPI
import model.builder.web.api.WebResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.http.HTTPClientException
import wslite.http.HTTPResponse
import wslite.rest.RESTClient
import wslite.rest.Response

import javax.net.ssl.SSLContext

class DefaultOpenAPI implements OpenAPI {

    private static final Logger msg = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
    def RESTClient client

    DefaultOpenAPI(String host) {
        SSLContext.default = SSL.context

        String uri
        if (host == 'localhost') {
            uri = "http://${host}:8080"
        } else {
            uri = "https://${host}"
        }

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
                            delegate.json
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
        if (!email) throw new IllegalArgumentException("email must not be null")

        try {
            client.get(path: "/api/apikeys/users/${email.toLowerCase()}") as WebResponse
        } catch (HTTPClientException e) {
            String msgLog = "GET Error; Params '[path: '/api/apikeys/users/$email']'; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }
}
