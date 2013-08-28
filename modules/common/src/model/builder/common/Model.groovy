package model.builder.common

import groovy.transform.TupleConstructor

@TupleConstructor
class Model {

    final String id
    final String name

    def String toString() { name }
}
