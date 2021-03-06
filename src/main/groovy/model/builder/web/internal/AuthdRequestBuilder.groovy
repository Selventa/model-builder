package model.builder.web.internal

import groovy.transform.TupleConstructor
import org.apache.commons.codec.binary.Hex
import wslite.http.HTTPMethod
import wslite.http.HTTPRequest
import wslite.rest.RequestBuilder

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import static java.lang.System.currentTimeMillis
import static model.builder.web.internal.Constant.HMAC

@TupleConstructor
class AuthdRequestBuilder extends RequestBuilder {

    final String apiKey
    final String privateKey

    HTTPRequest build(HTTPMethod meth, String url, Map params, byte[] data) {
        params['query'] = (params['query'] ?: [:]) +
                [apikey: apiKey, ts: currentTimeMillis() / 1000 as int]
        HTTPRequest req = super.build(meth, url, params, data)

        SecretKeySpec keySpec = new SecretKeySpec(privateKey.bytes, HMAC);
        Mac mac = Mac.getInstance(HMAC);
        mac.init(keySpec);
        byte[] result = mac.doFinal("${req.url}".bytes);
        String hash = new String(Hex.encodeHex(result));

        def hashedUrl = "${req.url}&hash=$hash"
        req.setUrl(new URL(hashedUrl))
        req
    }
}
