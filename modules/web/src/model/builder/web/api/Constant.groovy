package model.builder.web.api

class Constant {

    static final String JSON_MIME_TYPE = 'application/json'

    static def sortAscending(field) {
        "$field asc"
    }

    static def sortDescending(field) {
        "$field desc"
    }

    private Constant() {}
}
