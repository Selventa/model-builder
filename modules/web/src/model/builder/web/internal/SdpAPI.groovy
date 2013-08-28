package model.builder.web.internal

import groovy.json.JsonOutput
import model.builder.web.api.API
import model.builder.web.api.WebResponse
import wslite.http.HTTPClientException
import wslite.rest.RESTClient
import wslite.rest.Response

import javax.net.ssl.SSLContext

class SdpAPI implements API {

    def RESTClient client

    SdpAPI(String uri) {
        SSLContext.default = SSL.context

        client = new RESTClient(uri)
        client.requestBuilder = new AuthdRequestBuilder('test@sdpdemo.selventa.com', 'test')
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
    }

    @Override
    String uri(String idKey) {
        def (type, id) = idKey.split(/:/)
        "${client.url}/api/$type/$id"
    }

    @Override
    WebResponse model(String id) {
        client.get(path: "/api/models/$id") as WebResponse
    }

    @Override
    WebResponse models() {
        client.get(path: '/api/models') as WebResponse
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

        println params.toMapString()
        client.get(path: '/search', query: params) as WebResponse
    }

    @Override
    WebResponse modelTags() {
        def params = [q: 'type:model', rows: '0', facet: 'on', 'facet.field': 'tags']
        client.get(path: '/search', query: params) as WebResponse
    }

    static void main(String[] args) {

        def proxy = ProxyMetaClass.getInstance(SdpAPI.class)
        proxy.interceptor = new BenchmarkInterceptor()
        proxy.use {
            API api = new SdpAPI('https://sdpdemo.selventa.com')
            try {
                println JsonOutput.toJson(api.model('519695ea42bc1d34b1757f5a').data)
                println JsonOutput.toJson(api.models().data)
                println api.searchModels(rows: 500).data.response.numFound
                println api.searchModels(tags: ['NetworkKnitting']).data.response.numFound
                println api.searchModels(tags: ['NetworkKnitting', 'SDPmigration'], species: ['9606']).data.response.numFound
                println api.searchModels(name: 'Angiogenesis*').data.response.numFound
                println JsonOutput.prettyPrint(JsonOutput.toJson(api.searchModels(name: 'Angiogenesis*', start: 5, rows: 1).data.response.docs))
            } catch (HTTPClientException e) {
                println "${e.response.statusCode}: ${e.response.statusMessage}"
            }
        }
        println proxy.interceptor.statistic()
    }
}
