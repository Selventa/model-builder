package model.builder.web.api

import wslite.http.HTTPClientException

import static java.util.Collections.emptyIterator

class SearchProvider implements Iterator<Expando> {

    private static final int MAX_ROWS_PER_REQUEST = 100
    private final AuthorizedAPI api
    private final Map search
    private Iterator<Expando> resultsPage = emptyIterator()
    private int resultsRead = 0;
    private int numFound = 0;

    public SearchProvider(AuthorizedAPI api, Map search) {
        if (!api) throw new NullPointerException("api cannot be null")
        if (!search) throw new NullPointerException("search cannot be null")
        this.api = api
        this.search = search
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean hasNext() {
        if (resultsPage.hasNext()) {
            return true
        } else {
            return advance()
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    Expando next() {
        if (resultsPage.hasNext()) {
            resultsPage.next()
        } else {
            if (advance()) {
                resultsPage.next()
            } else {
                throw new NoSuchElementException("read past end of search results")
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    void remove() {
        throw new UnsupportedOperationException("does not support removal")
    }

    private boolean advance() {
        if (resultsRead > numFound) return false

        def (numFound, results) = runSearch(api, search + [
                start: resultsRead,
                rows:  MAX_ROWS_PER_REQUEST
        ])
        this.numFound = numFound
        this.resultsRead += MAX_ROWS_PER_REQUEST
        this.resultsPage = results.iterator()
        return true
    }

    private static def runSearch(AuthorizedAPI api, Map search) {
        def response = api.search(search)
        if (response.statusCode != 200) {
            throw new HTTPClientException("Error with /search (${response.statusCode}).")
        }

        def solr = response.data.response
        def results = solr.docs.collect {
            new Expando(
                    id: it['id'],
                    name: it['name'],
                    description: it['description'] ?: '',
                    tags: it['tags'].collect { it['name'] }.sort().join(', ') ?: '',
                    createdAt: it['created_at'],
                    updatedAt: it['updated_at']
            )
        }.sort {it.name}

        // multi-return as List
        [
                ((int) (solr.numFound ?: results.size())),
                results
        ]
    }
}
