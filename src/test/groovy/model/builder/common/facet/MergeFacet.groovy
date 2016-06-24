package model.builder.common.facet

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.mergeFacets
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

@RunWith(Theories.class)
class MergeFacet {

    @Theory void mergeFacets(Map data) {
        assertThat mergeFacets([data.facetA, data.facetB]), equalTo(data.merge)
    }

    @DataPoints static Map[] data() {
        [
            // different field
            [
                facetA: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']]
                ],
                facetB: [
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']]
                ],
                merge: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']]
                ]
            ],
            // same field
            [
                facetA: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']]
                ],
                facetB: [
                    foo: [B: [value: 'B', count: 3, filterComparison: 'unset']]
                ],
                merge: [
                    foo: [
                        A: [value: 'A', count: 3, filterComparison: 'unset'],
                        B: [value: 'B', count: 3, filterComparison: 'unset']
                    ]
                ]
            ],
            // same field, same value; count accumulates; shift add filterComparison
            [
                facetA: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']]
                ],
                facetB: [
                    foo: [
                        A: [value: 'A', count: 1, filterComparison: 'inclusion'],
                        B: [value: 'B', count: 3, filterComparison: 'unset']
                    ],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']]
                ],
                merge: [
                    foo: [
                        A: [value: 'A', count: 4, filterComparison: 'inclusion'],
                        B: [value: 'B', count: 3, filterComparison: 'unset']
                    ],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']]
                ]
            ],
            // empty field descriptions
            [
                facetA: [:], facetB: [:], merge: [:]
            ]
        ]
    }
}
