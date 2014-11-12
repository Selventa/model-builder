package model.builder.core.rcr

public enum MechanismPaintField {
    DIRECTION  ("direction",   String.class),
    CONCORDANCE("concordance", Double.class),
    RICHNESS   ("richness",    Double.class);

    String field
    Class<?> type

    MechanismPaintField(String field, Class<?> type) {
        this.field = field
        this.type  = type
    }

    static MechanismPaintField fromField(String field) {
        values().find { field == it.field }
    }

    String toString() {
        this.field.capitalize()
    }
}
