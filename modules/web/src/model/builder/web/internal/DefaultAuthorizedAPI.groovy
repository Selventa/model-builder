package model.builder.web.internal

import model.builder.common.uri.URIBuilder
import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.JsonStreamResponse
import model.builder.web.api.WebResponse
import org.apache.commons.codec.binary.Hex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import wslite.http.HTTPClientException
import wslite.rest.RESTClient

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.SSLContext

import static java.lang.System.currentTimeMillis
import static wslite.rest.ContentType.JSON
import static model.builder.web.internal.Constant.HMAC

class DefaultAuthorizedAPI implements AuthorizedAPI {

    private static final Logger msg = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
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
            match = (uri as String) =~ /\/api\/rcr-results\/(.+)/
            if (match.find()) return match[0][1]
        }
    }

    @Override
    WebResponse user(String email) {
        get(path: "/api/users/$email", accept: JSON)
    }

    @Override
    WebResponse comparison(String id) {
        get(path: "/api/comparisons/$id", query: ['navigate': 'subresource'], accept: JSON)
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
        get(path: "/api/rcr-results/$id", query: ['navigate': 'subresource'], accept: JSON)
    }

    @Override
    WebResponse search(Map data) {
        if (data == null) throw new NullPointerException("data cannot be null")
        if (!data.type) throw new IllegalArgumentException("data must contain a type to search")

        def params = [:]
        params.q = "type:${data.type} AND name:${data.name ?: '*'}"
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
        get(path: '/api/knowledge-networks', accept: JSON)
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
        def path = "/api/knowledge-networks/$knowledgeNetwork/paths"
        def (scheme, host, port) = convertHost(access.host)

        URIBuilder ub = new URIBuilder("$scheme://$host")
        if (port) ub.setPort(port)
        ub.setPath(path)

        from.collect { ub.addQueryParam("from", it)}
        to.collect { ub.addQueryParam("to", it)}
        [
            'direction', 'max_path_length', 'num_returned', 'fx_include',
            'fx_exclude', 'rel_include', 'rel_exclude'
        ].each { key ->
            [params[key]].flatten().findAll().each { ub.addQueryParam(key, it)}
        }

        try {
            def url = hashURL(ub.toURL(), access.apiKey, access.privateKey)
            URLConnection urlc = url.openConnection()
            urlc.setRequestProperty('Accept-Encoding', 'gzip')

            def jsonResponse = new JsonStreamResponse((HttpURLConnection) urlc)
            if (jsonResponse)
                logPathsResponse(msg, access.apiKey, path, params, jsonResponse)

            return jsonResponse
        } catch(Throwable e) {
            msg.error("Paths GET Exception", e)
            throw e
        }
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
            def model = response.data
            model.id = this.id(uri: model.uri as String)
        }
        response
    }

    def WebResponse get(Map params) {
        try {
            return new WebResponse(client.get(params))
        } catch (HTTPClientException e) {
            String msgLog = "GET Error; Params '${params.toMapString()}'; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    def WebResponse put(Map params, Closure content) {
        try {
            return new WebResponse(client.put(params, content))
        } catch (HTTPClientException e) {
            String msgLog = "PUT Error; Params '${params.toMapString()}'; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    def WebResponse post(Map params, Closure content) {
        try {
            return new WebResponse(client.post(params, content))
        } catch (HTTPClientException e) {
            String msgLog = "POST Error; Params '${params.toMapString()}'; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

            def res = e.response as WebResponse
            if (res.statusCode == 500) throw e
            res
        }
    }

    def WebResponse delete(Map params) {
        try {
            return new WebResponse(client.delete(params))
        } catch (HTTPClientException e) {
            String msgLog = "DELETE Error; Params '${params.toMapString()}'; Status ${e.response?.statusCode}; ${e.response?.statusMessage}"
            msg.error(msgLog, e)

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

    static URL hashURL(URL unhashed, String apiKey, String privateKey) {
        SecretKeySpec keySpec = new SecretKeySpec(privateKey.bytes, HMAC);
        Mac mac = Mac.getInstance(HMAC);
        mac.init(keySpec);

        String decodedPath = unhashed.path
        String port = unhashed.port == -1 ? '' : ":${unhashed.port}"
        def creds = "apikey=${apiKey}&ts=${currentTimeMillis() / 1000 as int}".toString()

        String toHash = "${unhashed.protocol}://${unhashed.host}${port}$decodedPath?$unhashed.query&$creds"

        byte[] result = mac.doFinal(toHash.bytes);
        String hash = new String(Hex.encodeHex(result));

        new URL("${unhashed}&$creds&hash=$hash")
    }

    /**
     * Logs response of /paths request.
     *
     * XXX This is temporary in order to diagnose paths request failures.
     */
    private static void logPathsResponse(Logger log, String apiKey, String path,
                                         Map params, JsonStreamResponse jsonResponse) {
        String info =  "Paths GET Success"
        String warn =  "Paths GET Warning"
        String error = "Paths GET Error"
        String data = "Path: $path; " +
                "API Key: ${apiKey}; " +
                "Params: ${params?.toMapString()}; " +
                "Status Code: ${jsonResponse?.statusCode};" +
                "Status Message: ${jsonResponse?.statusMessage};" +
                "Headers: ${jsonResponse?.headers?.toMapString()}"
        switch(jsonResponse.statusCode) {
            case 200:
                log.warn("$info; $data")
                break
            case 500..<600:
                log.error("$error; $data")
                break
            default:
                log.warn("$warn; $data")
                break
        }
    }
}
