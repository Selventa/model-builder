package model.builder.web.api

class Constant {

    static final String JSON_MIME_TYPE = 'application/json'

    static def sortAscending(field) {
        "$field asc"
    }

    static def sortDescending(field) {
        "$field desc"
    }

    static final String MODEL_TYPE = "model"
    static final String COMPARISON_TYPE = "comparison"
    static final String RCR_RESULT_TYPE = "rcr_result"

    private Constant() {}
}
