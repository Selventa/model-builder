package model.builder.web.api

interface AuthorizedAPI {

    String uri(Map data)

    String id(Map data)

    WebResponse user(String email)

    WebResponse comparison(String id)

    WebResponse model(String id)

    WebResponse models()

    WebResponse rcrResult(String id)

    WebResponse search(Map data)

    WebResponse searchComparisons(Map data)

    WebResponse searchModels(Map data)

    WebResponse searchRcrResults(Map data)

    WebResponse tags(types)

    WebResponse knowledgeNetworks()

    WebResponse[] modelRevisions(id, revision, uri)

    WebResponse postModel(comment, network)

    WebResponse putModelRevision(uri, comment, network)

    JsonStreamResponse paths(knowledgeNetwork, from, to, Map params)

    WebResponse sets()

    WebResponse postSet(name, description, elements)

    WebResponse putSet(uri, name, description, elements)

    WebResponse deleteSet(uri)
}