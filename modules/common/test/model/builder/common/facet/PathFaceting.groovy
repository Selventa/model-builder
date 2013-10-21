package model.builder.common.facet

import model.builder.common.JsonStream
import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.*
import static org.hamcrest.Matchers.is
import static org.junit.Assert.assertThat
import static org.junit.Assert.assertTrue

@RunWith(Theories.class)
class PathFaceting {

    @Theory void equalCardinality(Map data) {
        assertThat data.items.size(), is(data.fieldDescriptions.size())
    }

    @Theory void flatFieldDescriptions(Map data) {
        assertTrue data.fieldDescriptions.every {
            !(Map in it.values().collect {it.class})
        }
    }

    @Theory void facetStructure(Map data) {
        assertTrue data.facets.every { field, facet ->
            facet.values().every {it.&containsKey('count')}
        }
    }

    @Theory void filterApplication(Map data) {
        assertThat filter(data.items, data.fieldDescriptions, data.facetsSelected.call(data.facets)).size(), is(data.filteredCount)
    }

    @DataPoints
    static Map[] data() {
        JsonStream.instance.initializeFactory()
        InputStream file = PathFaceting.class.getResourceAsStream('paths.json')
        List objs = JsonStream.instance.jsonObjects(file).toList()
        def descClosure = {
            def desc = [
                    start: it.start.label,
                    end: it.end.label,
                    path_length: (int) (it.path.size() / 2)
            ].withDefault {[]}
            def nodes = it.path.collect {it.label}.findAll().unique()
            nodes.subList(1, nodes.size()-1).each {
                desc['intermediates'] << it
            }
            it.path.collect {it.relationship}.findAll().unique().each {
                desc['relationships'] << it
            }
            it.path.collect { it.evidence*.annotations }.
                    flatten().findAll().
                    inject([:].withDefault { [] }) { agg, item ->
                        item.each { k, v ->
                            agg[k] << v
                        }
                        agg
                    }.each { k, v ->
                v.unique().each {
                    desc[k] << it
                }
            }
            desc
        }

        [
            [
                items: objs,
                fieldDescriptions: describe(objs, descClosure),
                facets: facet(describe(objs, descClosure)),
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'end', 'p(HGNC:AKT2)')
                },
                filteredCount: 80
            ],
            [
                items: objs,
                fieldDescriptions: describe(objs, descClosure),
                facets: facet(describe(objs, descClosure)),
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'start', 'p(HGNC:AKT1)')
                    includeFacetValues(facets, 'path_length', 4)
                    includeFacetValues(facets, 'intermediates', 'a(SCHEM:\"LY 294002\")')
                },
                filteredCount: 2
            ]
        ]
    }
}
