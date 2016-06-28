package model.builder.ui

import model.builder.web.api.AccessInformation
import model.builder.web.api.AuthorizedAPI
import model.builder.web.internal.DefaultAuthorizedAPI

import javax.swing.JFrame

import static java.util.Collections.emptyList

AuthorizedAPI api = new DefaultAuthorizedAPI(
        new AccessInformation(
                true,
                "selventa-sdp.selventa.com",
                "abargnesi@selventa.com",
                "api:abargnesi@selventa.com",
                "superman"))

TableScrollable<Expando> tscroll = new SearchTableScrollable<>({
    int offset, int length ->
        def models = api.searchModels(start: offset, rows: length)
        if (models.statusCode != 200) return emptyList()

        def solr = models.data.response
        solr.docs.collect {
            new Expando(
                    id: it['id'],
                    name: it['name'],
                    tags: it['tags'].collect { it['name'] }.sort().join(', ') ?: ''
            )
        }.sort {it.name}
})

JFrame frame = new JFrame("Test")
frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
frame.getContentPane().add(tscroll)
frame.visible = true