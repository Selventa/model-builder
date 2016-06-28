package model.builder.common.facet

import org.junit.experimental.theories.DataPoints
import org.junit.experimental.theories.Theories
import org.junit.experimental.theories.Theory
import org.junit.runner.RunWith

import static model.builder.common.facet.Functions.filterFunction
import static org.junit.Assert.assertTrue

@RunWith(Theories.class)
class FilterFunction {

    @Theory void filterFunctionByType(Map data) {
        assertTrue filterFunction(data.values).call(data.functionValue) as Boolean
    }

    @DataPoints static Map[] data() {
        [
            // exact match; 1 to 1
            [values: 3, functionValue: 3],
            [values: 2.5, functionValue: 2.5],
            [values: true, functionValue: true],
            [values: false, functionValue: false],
            [values: 'one', functionValue: 'one'],
            [values: 'multi-word val', functionValue: 'multi-word val'],
            // match; 1 to many
            [values: [0, 1, 2, 3], functionValue: 1],
            [values: [0.0, 5.0, 10.0], functionValue: [5.0]],
            [values: [false, false, true], functionValue: true],
            [values: [false, false, true], functionValue: false],
            [values: ['one', 'two', 'three'], functionValue: ['two']],
            // match; many to 1
            [values: [3], functionValue: [0, 1, 2, 3]],
            [values: [5.0], functionValue: [0.0, 5.0, 10.0, 20.0]],
            [values: ['ONE'], functionValue: ['one', 'ONE', 'two']],
            [values: false, functionValue: [true, false]],
            // match; many to many
            [values: [0, 1, 2, 3], functionValue: [2, 3]],
            [values: [0.0, 5.0, 10.0], functionValue: [5.0, 10.0]],
            [values: ['one', 'two', 'three'], functionValue: ['one', 'two']],
            [values: [false, false, true], functionValue: [true, true, false]],
            // exact match; many to many
            [values: [0, 1, 2, 3], functionValue: [0, 1, 2, 3]],
            [values: [0.0, 5.0, 10.0], functionValue: [0.0, 5.0, 10.0]],
            [values: ['one', 'two'], functionValue: ['one', 'two']],
            [values: [false, true], functionValue: [false, true]],
            // many number values are always loose ranges
            [values: [0, 1, 2, 3], functionValue: [2.5]],
            [values: [0.0, 5.0, 10.0], functionValue: [2.0, 9]],
            // values can be mixed type; in this case values could not be
            // a range
            [values: [0, 1, '2', 3], functionValue: '2'],
            [values: [0, 1, '2', 3], functionValue: 0],
        ]
    }
}
