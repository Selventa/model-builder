package model.builder.core.rcr

public enum ScorePaintField {
    DIRECTION                ("Direction",                 String.class),
    CONCORDANCE              ("Concordance",               Double.class),
    CONCORDANCE_AND_DIRECTION("Concordance and Direction", Double.class),
    RICHNESS                 ("Richness",                  Double.class),
    RICHNESS_AND_DIRECTION   ("Richness and Direction",    Double.class)

    String field
    Class<?> type

    ScorePaintField(String field, Class<?> type) {
        this.field = field
        this.type  = type
    }

    static ScorePaintField fromField(String field) {
        values().find { field == it.field }
    }

    String toString() {
        field
    }
}
