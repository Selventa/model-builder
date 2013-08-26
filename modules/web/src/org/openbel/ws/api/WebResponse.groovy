package org.openbel.ws.api

import groovy.transform.TupleConstructor

@TupleConstructor
class WebResponse {
    final int statusCode
    final String statusMessage
    final String contentType
    final String charset
    final Map headers
    final Map data
}
