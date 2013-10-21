package model.builder.common.facet

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.*
import static org.hamcrest.core.IsEqual.equalTo
import static org.junit.Assert.assertThat

@RunWith(Theories.class)
class MutateFacetValues {

    @Theory void inclusions(Map data) {
        assertThat includeFacetValues(data.facets, data.key, data.values), equalTo(data.facetInclusion)
    }

    @Theory void exclusions(Map data) {
        assertThat excludeFacetValues(data.facets, data.key, data.values), equalTo(data.facetExclusion)
    }

    @Theory void none(Map data) {
        assertThat unsetFacetValues(data.facets, data.key, data.values), equalTo(data.facetUnset)
    }

    @DataPoints static Map[] data() {
        [
            // same values
            [
                facets: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']],
                    baz: [C: [value: 'C', count: 3, filterComparison: 'unset']]
                ],
                key: 'foo',
                values: ['A'],
                facetInclusion: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'inclusion']],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']],
                    baz: [C: [value: 'C', count: 3, filterComparison: 'unset']]
                ],
                facetExclusion: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'exclusion']],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']],
                    baz: [C: [value: 'C', count: 3, filterComparison: 'unset']]
                ],
                facetUnset: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']],
                    baz: [C: [value: 'C', count: 3, filterComparison: 'unset']]
                ]
            ],
            // empty field descriptions
            [
                facets: [:], key: 'foo', values: ['A'],
                facetInclusion: [:], facetExclusion: [:], facetUnset: [:]
            ]
        ]
    }
}
