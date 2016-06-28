package model.builder.common.facet

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.excludeFacetValues
import static model.builder.common.facet.Functions.filter
import static model.builder.common.facet.Functions.includeFacetValues
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

@RunWith(Theories.class)
class Filter {

    @Theory void filterApplication(Map data) {
        assertThat filter(data.items, data.fieldDescriptions, data.facetsSelected.call(data.facets)), equalTo(data.filteredItems)
    }

    @DataPoints static Map[] data() {
        [
            // single facet value inclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'bar', 'B')
                },
                filteredItems: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ]
            ],
            // single facet value exclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    excludeFacetValues(facets, 'foo', 'A')
                },
                filteredItems: [
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ]
            ],
            // multiple facet inclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'foo', 'A')
                    includeFacetValues(facets, 'baz', 'D')
                },
                filteredItems: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]]
                ]
            ],
            // multiple facet exclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    excludeFacetValues(facets, 'foo', ['A'])
                    excludeFacetValues(facets, 'baz', ['D'])
                },
                filteredItems: []
            ],
            // multi-value facet inclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'baz', ['C', 'D'])
                },
                filteredItems: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ]
            ],
            // multi-value facet exclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    excludeFacetValues(facets, 'baz', ['C', 'D'])
                },
                filteredItems: []
            ],
            // multi-value facet inclusion and exclusion
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'foo', ['A', 'B'])
                    excludeFacetValues(facets, 'baz', ['D', 'E'])
                },
                filteredItems: []
            ],
            [
                items: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]],
                    [foo: [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]]
                ],
                fieldDescriptions: [
                    [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']],
                    [foo: 'B', bar: ['B', 'C'], baz: ['C', 'D', 'E']]
                ],
                facets: [
                    foo: [A: [count: 1], B: [count: 1]],
                    bar: [A: [count: 1], B: [count: 2], C: [count: 1]],
                    baz: [C: [count: 2], D: [count: 2], E: [count: 1]]
                ],
                facetsSelected: { facets ->
                    includeFacetValues(facets, 'foo', ['A'])
                    excludeFacetValues(facets, 'baz', ['E'])
                },
                filteredItems: [
                    [foo: [foo: 'A', bar: ['A', 'B'], baz: ['C', 'D']]]
                ]
            ]
        ]
    }
}
