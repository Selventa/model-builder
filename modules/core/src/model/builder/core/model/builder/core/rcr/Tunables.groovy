package model.builder.core.model.builder.core.rcr

import org.cytoscape.work.util.ListSingleSelection

/**
 * Created by tony on 10/24/14.
 */
class Tunables {

    static ListSingleSelection<String> tunableMechanismPaintField() {
        new ListSingleSelection<String>(
                MechanismPaintField.values().collect { it.field.capitalize()})
    }

    private Tunables() {
        // static accessors only
    }
}
