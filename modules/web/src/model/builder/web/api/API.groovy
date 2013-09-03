package model.builder.web.api

interface API {

    String uri(Map data)

    String id(Map data)

    WebResponse comparison(String id)

    WebResponse model(String id)

    WebResponse models()

    WebResponse rcrResult(String id)

    WebResponse searchComparisons(Map data)

    WebResponse searchModels(Map data)

    WebResponse searchRcrResults(Map data)

    WebResponse tags(types)

    WebResponse[] modelRevisions(id, revision, uri)
}