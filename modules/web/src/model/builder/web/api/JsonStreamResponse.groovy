package model.builder.web.api

import groovy.transform.TupleConstructor
import model.builder.web.internal.JsonStream

import java.util.zip.GZIPInputStream

@TupleConstructor
class JsonStreamResponse {
    final int statusCode
    final String statusMessage
    final String contentType
    final String charset
    final Map headers
    final Iterator<Map> jsonObjects

    JsonStreamResponse(int statusCode, String statusMessage, String contentType,
                       String charset, Map headers, InputStream stream) {
        this.statusCode = statusCode
        this.statusMessage = statusMessage
        this.contentType = contentType
        this.charset = charset
        this.headers = headers
        this.jsonObjects = JsonStream.instance.jsonObjects(determineStream(headers, stream))
    }

    static InputStream determineStream(Map headers, InputStream stream) {
        headers['Content-Encoding']?.contains('gzip') ?
            new GZIPInputStream(stream) :
            stream
    }
}
