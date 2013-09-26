package model.builder.web.internal

import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.api.WebResponse
import wslite.http.HTTPClientException
import wslite.http.HTTPResponse
import wslite.rest.RESTClient
import wslite.rest.Response

import javax.net.ssl.SSLContext
import static wslite.rest.ContentType.JSON

class DefaultAuthorizedAPI implements AuthorizedAPI {

    def RESTClient client

    DefaultAuthorizedAPI(AccessInformation access) {
        SSLContext.default = SSL.context

        String uri
        if (access.host == 'localhost') {
            uri = "http://${access.host}:8080"
        } else {
            uri = "https://${access.host}"
        }

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
        addModelData(get(path: "/api/models/$id", accept: JSON))
    }

    @Override
    WebResponse models() {
        addModelData(get(path: '/api/models', accept: JSON))
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
    WebResponse putModelRevision(uri, network, comment) {
        return put(path: uri) {
            type JSON
            charset "utf8"
            json revision : ['comment': "$comment", 'network': network]
        } as WebResponse
    }

    def WebResponse addModelData(WebResponse response) {
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

    static void main(String[] args) {

        def proxy = ProxyMetaClass.getInstance(DefaultAuthorizedAPI.class)
        proxy.interceptor = new BenchmarkInterceptor()
        proxy.use {
            def access = new AccessInformation('janssen-sdp.selventa.com',
                    'abargnesi@selventa.com', 'api:abargnesi@selventa.com',
                    'superman')
            AuthorizedAPI api = new DefaultAuthorizedAPI(access)
            try {
                println api.tags().data.facet_counts.facet_fields.tags
//                println api.id(uri: 'https://sdpdemo.selventa.com/api/models/519695ea42bc1d34b1757f5a/revisions/1')
//                println api.id(uri: 'https://sdpdemo.selventa.com/api/models/519695ea42bc1d34b1757f5a/revisions/latest')
//                println api.id(uri: 'https://sdpdemo.selventa.com/api/models/519695ea42bc1d34b1757f5a')
//                println JsonOutput.prettyPrint(JsonOutput.toJson(api.model('519695ea42bc1d34b1757f5a').data))
//                println JsonOutput.prettyPrint(JsonOutput.toJson(api.modelRevisions('519695ea42bc1d34b1757f5a', 0).data))
//                println JsonOutput.toJson(api.models().data)
//                println api.searchModels(rows: 500).data.response.numFound
//                println api.searchModels(tags: ['NetworkKnitting']).data.response.numFound
//                println api.searchModels(tags: ['NetworkKnitting', 'SDPmigration'], species: ['9606']).data.response.numFound
//                println api.searchModels(name: 'Angiogenesis*').data.response.numFound
//                println JsonOutput.prettyPrint(JsonOutput.toJson(api.searchModels(name: 'Angiogenesis*', start: 5, rows: 1).data.response.docs))
            } catch (HTTPClientException e) {
                println "${e.response.statusCode}: ${e.response.statusMessage}"
            }
        }
        println proxy.interceptor.statistic()
    }
}
