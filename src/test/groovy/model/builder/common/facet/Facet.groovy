package model.builder.common.facet

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.facet
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

@RunWith(Theories.class)
class Facet {

    @Theory void fieldValuesAggregate(Map data) {
        assertThat facet(data.fieldDescriptions), equalTo(data.facet)
    }

    @DataPoints static Map[] data() {
        [
            // same values
            [
                fieldDescriptions: [
                    [foo: 'A', bar: 'B', baz: 'C'],
                    [foo: 'A', bar: 'B', baz: 'C'],
                    [foo: 'A', bar: 'B', baz: 'C']
                ],
                facet: [
                    foo: [A: [value: 'A', count: 3, filterComparison: 'unset']],
                    bar: [B: [value: 'B', count: 3, filterComparison: 'unset']],
                    baz: [C: [value: 'C', count: 3, filterComparison: 'unset']]
                ]
            ],
            //  different values; case sensitivity
            [
                fieldDescriptions: [
                    [foo: 'A', bar: 'B', baz: 'C'],
                    [foo: 'a', bar: 'b', baz: 'c']
                ],
                facet: [
                    foo: [
                        A: [value: 'A', count: 1, filterComparison: 'unset'],
                        a: [value: 'a', count: 1, filterComparison: 'unset']
                    ],
                    bar: [
                        B: [value: 'B', count: 1, filterComparison: 'unset'],
                        b: [value: 'b', count: 1, filterComparison: 'unset']
                    ],
                    baz: [
                        C: [value: 'C', count: 1, filterComparison: 'unset'],
                        c: [value: 'c', count: 1, filterComparison: 'unset']
                    ]
                ]
            ],
            // asymmetrical items
            [
                fieldDescriptions: [
                    [foo: 'A', baz: 'C'],
                    [foo: 'a']
                ],
                facet: [
                    foo: [
                        A: [value: 'A', count: 1, filterComparison: 'unset'],
                        a: [value: 'a', count: 1, filterComparison: 'unset']
                    ],
                    baz: [
                        C: [value: 'C', count: 1, filterComparison: 'unset'],
                    ]
                ]
            ],
            // field with multi-valued values
            [
                fieldDescriptions: [
                    [foo: ['A', 'B'], bar: [1,2,3], baz: [false]],
                    [foo: ['B', 'C'], bar: [3,4,5], baz: [true]],
                    [foo: ['C', 'D'], bar: [5,6,7], baz: [false]],
                ],
                facet: [
                    foo: [
                        A: [value: 'A', count: 1, filterComparison: 'unset'],
                        B: [value: 'B', count: 2, filterComparison: 'unset'],
                        C: [value: 'C', count: 2, filterComparison: 'unset'],
                        D: [value: 'D', count: 1, filterComparison: 'unset'],
                    ],
                    bar: [
                        1: [value: 1, count: 1, filterComparison: 'unset'],
                        2: [value: 2, count: 1, filterComparison: 'unset'],
                        3: [value: 3, count: 2, filterComparison: 'unset'],
                        4: [value: 4, count: 1, filterComparison: 'unset'],
                        5: [value: 5, count: 2, filterComparison: 'unset'],
                        6: [value: 6, count: 1, filterComparison: 'unset'],
                        7: [value: 7, count: 1, filterComparison: 'unset'],
                    ],
                    baz: [
                        (false): [value: false, count: 2, filterComparison: 'unset'],
                        (true):  [value: true, count: 1, filterComparison: 'unset']
                    ]
                ]
            ],
            // empty field descriptions
            [
                    fieldDescriptions: [], facet: [:]
            ]
        ]
    }
}
