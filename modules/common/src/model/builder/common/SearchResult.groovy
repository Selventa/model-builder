package model.builder.common

import groovy.transform.TupleConstructor

@TupleConstructor
class SearchResult {

    final String id
    final String name
    final String created
    final String tags

    def String toString() { name }
}
