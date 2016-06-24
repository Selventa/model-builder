package model.builder.web.api

import model.builder.web.internal.DefaultAuthorizedAPI
import org.junit.Before
import org.junit.Test

class SearchProviderTest {

    AuthorizedAPI api

//    @Before
    public void setUpAPI() {
        // TODO Mock search results
        this.api = new DefaultAuthorizedAPI(null)
    }

//    @Test
    public void testModelIteration() {
        def search = new SearchProvider(this.api, [type: 'model'])
        int results = 0
        while (search.hasNext()) {
            println(search.next().name)
            results += 1
        }
        println("count: $results")
    }
}
