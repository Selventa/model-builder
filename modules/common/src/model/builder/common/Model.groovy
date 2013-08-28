package model.builder.common

import groovy.transform.TupleConstructor

@TupleConstructor
class Model {

    final String uri
    final String name

    def String toString() { name }
}
