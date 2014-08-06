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
import javax.swing.JOptionPane

import static org.openbel.framework.common.BELUtilities.getFirstCause

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
        client.httpClient.connectTimeout = 10000
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

        if (!checkAPI(email)) return null

        try {
            client.get(path: "/api/apikeys/users/${email.toLowerCase()}") as WebResponse
        } catch (HTTPClientException e) {
            String msgLog = "GET Error; Params '[path: '/api/apikeys/users/$email']'; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

            def res = e.response as WebResponse
            if (res?.statusCode == 500) throw e
            res
        }
    }

    def checkAPI(String email) {
        try {
            def res = client.get(path: "/api/time") as WebResponse
            return res.statusCode == 200
        } catch (HTTPClientException e) {
            def host = new URL(client.url).host
            String msgLog = "API Check (/api/time), host: $host, email: $email; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

            if (e.cause?.class == UnknownHostException) {
                JOptionPane.showMessageDialog(null,
                    "The \"$host\" host is not known. Please confirm that the \n" +
                    "\"Host\" field matches your SDP host.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE)
            } else if (e.cause?.class == SocketTimeoutException) {
                JOptionPane.showMessageDialog(null,
                    "The \"$host\" host could not be reached (${e.message}).\n" +
                    "Please confirm that the \"Host\" field matches your SDP host.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE)
            } else if (getFirstCause(e).class == FileNotFoundException) {
                JOptionPane.showMessageDialog(null,
                    "The \"$host\" host does not appear to be an SDP instance (${e.message}).\n" +
                    "Please confirm that the \"Host\" field matches your SDP host.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE)
            } else {
                JOptionPane.showMessageDialog(null,
                    "The \"$host\" host could not be reached (${e.message}).\n" +
                    "Please confirm that the \"Host\" field matches your SDP host.",
                    "Connection Error", JOptionPane.ERROR_MESSAGE)
            }

            def res = e.response as WebResponse
            if (res?.statusCode == 500) throw e
            return false
        }
    }
}
