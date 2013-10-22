package model.builder.web.internal

import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.JsonStreamResponse
import model.builder.web.api.WebResponse
import org.apache.commons.codec.binary.Hex
import wslite.http.HTTPClientException
import wslite.http.HTTPResponse
import wslite.rest.RESTClient
import wslite.rest.Response

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext

import static java.lang.System.currentTimeMillis
import static model.builder.web.api.Constant.*
import static wslite.rest.ContentType.JSON
import static model.builder.web.internal.Constant.HMAC

class DefaultAuthorizedAPI implements AuthorizedAPI {

    final RESTClient client
    final AccessInformation access

    DefaultAuthorizedAPI(AccessInformation access) {
        SSLContext.default = SSL.context

        def (scheme, host, port, portPart) = convertHost(access.host)
        String uri = "$scheme://$host$portPart"

        this.access = access
        client = new RESTClient(uri)
        client.requestBuilder = new AuthdRequestBuilder(access.apiKey, access.privateKey)
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
                    old.invoke(delegate, c)
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
                    old.invoke(delegate, c)
            }
        }
        HttpURLConnection.metaClass.define {
            old = HttpURLConnection.metaClass.getMetaMethod("asType", [Class] as Class[])
            asType = { Class c ->
                if (c == JsonStreamResponse) {
                    if (!delegate.contentType.contains(JSON_MIME_TYPE))
                        throw new IllegalArgumentException("stream is not json")
                    new JsonStreamResponse(
                            delegate.responseCode,
                            delegate.responseMessage,
                            delegate.contentType,
                            delegate.contentEncoding,
                            delegate.headerFields,
                            delegate.inputStream
                    )
                } else
                    old.invoke(delegate, c)
            }
        }
    }

    @Override
    String uri(Map data) {
        if (data.searchKey) {
            def (type, id) = data.searchKey.split(/:/)
            return "${client.url}/api/$type/$id"
        }

        if (data.path) {
            return client.url + data.path
        }

        def msg = "data map should contain either \"searchKey\" or \"path\""
        throw new IllegalArgumentException(msg)
    }

    @Override
    String id(Map data) {
        if (data.searchKey) {
            return data.searchKey.split(/:/)[1]
        }

        if (data.uri) {
            def uri = new URI(data.uri)
            def match = (uri as String) =~ /\/api\/models\/\w+\/revisions\/(\d+|latest)/
            if (match.find()) return match[0][1]
            match = (uri as String) =~ /\/api\/models\/(.+)/
            if (match.find()) return match[0][1]
            match = (uri as String) =~ /\/api\/rcr_results\/(.+)/
            if (match.find()) return match[0][1]
        }
    }

    @Override
    WebResponse user(String email) {
        get(path: "/api/users/$email", accept: JSON)
    }

    @Override
    WebResponse comparison(String id) {
        get(path: "/api/comparisons/$id", accept: JSON)
    }

    @Override
    WebResponse model(String id) {
        addId(get(path: "/api/models/$id", accept: JSON))
    }

    @Override
    WebResponse models() {
        addId(get(path: '/api/models', accept: JSON))
    }

    @Override
    WebResponse rcrResult(String id) {
        get(path: "/api/rcr_results/$id", accept: JSON)
    }

    @Override
    WebResponse searchComparisons(Map data) {
        def params = [:]
        params.q = "type:comparison AND name:${data.name ?: '*'}"
        params.start = data.start ?: 0
        params.rows = data.rows ?: 100
        if (data.tags)
            params.q += " AND (${data.tags.collect {"tags:\"$it\""}.join(' OR ')})"
        if (data.sort) params.sort = data.sort

        get(path: '/search', query: params, accept: JSON)
    }

    @Override
    WebResponse searchModels(Map data = [:]) {
        def params = [:]
        params.q = "type:model AND name:${data.name ?: '*'}"
        params.start = data.start ?: 0
        params.rows = data.rows ?: 100
        if (data.tags)
            params.q += " AND (${data.tags.collect {"tags:\"$it\""}.join(' OR ')})"
        if (data.species)
            params.q += " AND (${data.species.collect {"species:\"$it\""}.join(' OR ')})"
        if (data.sort) params.sort = data.sort

        get(path: '/search', query: params, accept: JSON)
    }

    @Override
    WebResponse searchRcrResults(Map data) {
        def params = [:]
        params.q = "type:rcr_result AND name:${data.name ?: '*'}"
        params.start = data.start ?: 0
        params.rows = data.rows ?: 100
        if (data.tags)
            params.q += " AND (${data.tags.collect {"tags:\"$it\""}.join(' OR ')})"
        if (data.sort) params.sort = data.sort

        get(path: '/search', query: params, accept: JSON)
    }

    @Override
    WebResponse tags(types = ['*']) {
        def params = [q: types.collect {"type:$it"}.join(' OR '),
                      rows: '0', facet: 'on', 'facet.field': 'tags']
        get(path: '/search', query: params, accept: JSON)
    }

    @Override
    WebResponse knowledgeNetworks() {
        get(path: '/api/knowledge_networks', accept: JSON)
    }

    @Override
    WebResponse[] modelRevisions(id, revision, uri = '') {
        if (uri) {
            def tokens = "$uri".split(/\/api\//)
            if (tokens.length != 2)
                throw new IllegalArgumentException("uri is an invalid model revision uri: $uri")
            return get(path: "/api/${tokens[1]}")
        }

        [revision].flatten().collect {
            String path = "/api/models/$id/revisions/$revision"
            get(path: path, accept: JSON)
        } as WebResponse[]
    }

    @Override
    WebResponse postModel(comment, network) {
        post(path: '/api/models') {
            type JSON
            charset "utf8"
            json model : ['comment': "$comment", 'network': network]
        }
    }

    @Override
    WebResponse putModelRevision(uri, network, comment) {
        put(path: uri) {
            type JSON
            charset "utf8"
            json revision : ['comment': "$comment", 'network': network]
        }
    }

    @Override
    JsonStreamResponse paths(knowledgeNetwork, from, to, Map params = [:]) {
        def path = "/api/knowledge_networks/$knowledgeNetwork/paths"
        def (scheme, host, port) = convertHost(access.host)
        def query = (
            [
                from.collect {"from=$it"},
                to.collect {"to=$it"},
                "apikey=${access.apiKey}",
                "ts=${currentTimeMillis() / 1000 as int}"
            ] +
            [
                'direction', 'max_path_length', 'num_returned', 'fx_include',
                'fx_exclude', 'rel_include', 'rel_exclude'
            ].collect { key ->
                if (params."$key") [params."$key"].flatten().collect {"$key=$it"}
            }.findAll()
        ).flatten().join('&')

        def url = hashURL(toURL(scheme, host, port, path, query), access.privateKey)
        URLConnection urlc = url.openConnection()
        urlc.setRequestProperty('Accept-Encoding', 'gzip')
        urlc as JsonStreamResponse
    }

    @Override
    WebResponse sets() {
        get(path: '/api/sets', accept: JSON)
    }

    @Override
    WebResponse postSet(name, description, elements) {
        post(path: '/api/sets') {
            type JSON
            charset "utf8"
            json name: name, description: description, elements: elements
        }
    }

    @Override
    WebResponse putSet(uri, name, description, elements) {
        put(path: uri) {
            type JSON
            charset "utf8"
            json name: name, description: description, elements: elements
        }
    }

    @Override
    WebResponse deleteSet(uri) {
        delete(path: uri)
    }

    def WebResponse addId(WebResponse response) {
        if (response.data) {
            def map = response.data
            map.model.id = this.id(uri: map.model.uri as String)
        }
        response
    }

    def WebResponse get(Map params) {
        try {
            client.get(params) as WebResponse
        } catch (HTTPClientException e) {
            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    def WebResponse put(Map params, Closure content) {
        try {
            client.put(params, content) as WebResponse
        } catch (HTTPClientException e) {
            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    def WebResponse post(Map params, Closure content) {
        try {
            client.post(params, content) as WebResponse
        } catch (HTTPClientException e) {
            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    def WebResponse delete(Map params) {
        try {
            client.delete(params) as WebResponse
        } catch (HTTPClientException e) {
            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    static List convertHost(String host) {
        host == 'localhost' ?
            ['http', 'localhost', '8080', ':8080'] :
            ['https', host, '', '']
    }

    static URL toURL(scheme, host, port, path, query) {
        if (port)
            new URI(scheme, null, host, port as int, path, query, null).toURL()
        else
            new URI(scheme, host, path, query, null).toURL()
    }

    static URL hashURL(URL unhashed, String privateKey) {
        SecretKeySpec keySpec = new SecretKeySpec(privateKey.bytes, HMAC);
        Mac mac = Mac.getInstance(HMAC);
        mac.init(keySpec);

        byte[] result = mac.doFinal(unhashed.bytes);
        String hash = new String(Hex.encodeHex(result));

        new URL("${unhashed}&hash=$hash")
    }
}
