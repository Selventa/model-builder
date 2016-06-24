package model.builder.web.api

import wslite.http.HTTPResponse
import wslite.rest.Response

class WebResponse {
    final int statusCode
    final String statusMessage
    final String contentType
    final String charset
    final Map headers
    final def data

    WebResponse(Response response) {
        if (!response) throw new NullPointerException("response cannot be null")
        this.statusCode = response.statusCode
        this.statusMessage = response.statusMessage
        this.contentType = response.contentType
        this.charset = response.charset
        this.headers = response.headers
        this.data = response.statusCode in 200..<400 ? response.json : []
    }

    WebResponse(HTTPResponse httpResponse) {
        if (!httpResponse) throw new NullPointerException("httpResponse cannot be null")
        this.statusCode = httpResponse.statusCode
        this.statusMessage = httpResponse.statusMessage
        this.contentType = httpResponse.contentType
        this.charset = httpResponse.charset
        this.headers = httpResponse.headers
        this.data = httpResponse.data
    }
}
