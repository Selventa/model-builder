package model.builder.web.api

import groovy.transform.EqualsAndHashCode
import groovy.transform.TupleConstructor

@TupleConstructor
@EqualsAndHashCode(cache = true)
class AccessInformation {

    boolean defaultAccess
    final String host
    final String email
    final String apiKey
    final String privateKey

    def String getConfigValue() {
        "$defaultAccess,$host,$email,$apiKey,$privateKey"
    }

    def String toString() {
        def m = ((host =~ /sdp(\w+)\.selventa\.com/) ?:
                 (host =~ /(\w+)-sdp\.selventa\.com/))
        if (m.matches())
            "${m[0][1].capitalize()} SDP"
        else
            host
    }
}
