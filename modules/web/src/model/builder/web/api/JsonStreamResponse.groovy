package model.builder.web.api

import model.builder.common.JsonStream

import java.util.zip.GZIPInputStream

class JsonStreamResponse {
    final int statusCode
    final String statusMessage
    final String contentType
    final String charset
    final Map headers
    final InputStream responseData

    JsonStreamResponse(HttpURLConnection httpConnection) {
        if (httpConnection == null) throw new NullPointerException("httpConnection cannot be null")
        this.statusCode = httpConnection.responseCode
        this.statusMessage = httpConnection.responseMessage
        this.contentType = httpConnection.contentType
        this.charset = httpConnection.contentEncoding
        this.headers = httpConnection.headerFields
        this.responseData = httpConnection.inputStream
    }

    Iterator<Map> getJsonObjectStream() {
        JsonStream.instance.jsonObjects(determineStream(headers, responseData))
    }

    private InputStream determineStream(Map headers, InputStream stream) {
        headers['Content-Encoding']?.contains('gzip') ?
            new GZIPInputStream(stream) :
            stream
    }
}
