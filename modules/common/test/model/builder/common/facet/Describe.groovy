package model.builder.common.facet

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.describe
import static org.hamcrest.Matchers.equalTo
import static org.junit.Assert.assertThat

@RunWith(Theories.class)
class Describe {

    @Theory void itemsCanBeIterated(Map data) {
        assertThat describe(data.items, data.closure).count{}, equalTo(data.items?.count{} ?: 0)
    }

    @Theory void fieldDescriptionsMatch(Map data) {
        describe(data.items, data.closure).each { fieldDescription ->
            assertThat fieldDescription.keySet() as List, equalTo(data.fields)
        }
    }

    @DataPoints static Map[] data() {
        [
            // strings; items is a List
            [
                items: [
                    [foo: [bar: 'A', baz: 'B']],
                    [foo: [bar: 'A', baz: 'C']],
                    [foo: [bar: 'B', baz: 'C']]
                ],
                closure: { item ->
                    [bar: item.foo.bar, baz: item.foo.baz]
                },
                fields: ['bar', 'baz']
            ],
            // ints and strings; items is a Set
            [
                items: [
                    [foo: 0, val: [bar: 'A', baz: 'B']],
                    [foo: 1, val: [bar: 'A', baz: 'C']],
                    [foo: 2, val: [bar: 'B', baz: 'C']]
                ] as Set,
                closure: { item ->
                    [foo: item.foo, bar: item.val.bar, baz: item.val.baz]
                },
                fields: ['foo', 'bar', 'baz']
            ],
            // closure yields empty fields; items is an Iterator
            [
                items: [
                    [foo: 'A'],
                    [foo: 'B'],
                    [foo: 'C']
                ].iterator(),
                closure: { item ->
                    [:]
                },
                fields: []
            ],
            // closure yields null(s)
            // FIXME cannot test this easily at the moment
            //[
            //    items: [
            //        [foo: 'A']
            //    ],
            //    closure: {},
            //    fields: []
            //],
            // items; closure is null
            [
                items: [
                    [foo: 'A']
                ],
                closure: null,
                fields: []
            ],
            // items is empty; closure yields null
            [
                items: [],
                closure: {},
                fields: []
            ],
            // items is empty; closure is null
            [
                items: [],
                closure: null,
                fields: []
            ],
            // items and closure are null
            [
                items: null,
                closure: null,
                fields: []
            ]
        ]
    }
}
