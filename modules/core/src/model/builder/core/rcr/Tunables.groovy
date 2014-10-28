package model.builder.core.rcr

import org.cytoscape.work.util.ListSingleSelection

import static model.builder.core.rcr.Constant.SDP_URI_COLUMN

class Tunables {

    static ListSingleSelection<String> tunableMechanismPaintField() {
        new ListSingleSelection<String>(
                MechanismPaintField.values().collect { it.field.capitalize()})
    }

    static ListSingleSelection<Expando> tunableRcrField() {
        RcrResourceTableView rcrTableView = new RcrResourceTableView()

        List<Expando> rcrRowObjs = rcrTableView.getAll().collect {
            Map rcr ->
                new Expando(
                        name    : rcr.name,
                        uri     : rcr[SDP_URI_COLUMN] ,
                        toString: {
                            "${rcr.name} (${rcr[SDP_URI_COLUMN]})"
                        }
                )
        }.sort {
            it.toString()
        }
        new ListSingleSelection<Expando>(rcrRowObjs)
    }

    private Tunables() {
        // static accessors only
    }
}
