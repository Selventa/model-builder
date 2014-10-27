package model.builder.core.rcr

import org.cytoscape.model.CyTable
import org.cytoscape.work.util.ListSingleSelection
import static model.builder.core.Activator.CY
import static model.builder.core.rcr.Constant.SDP_RCR_SCORES_TABLE_PREFIX
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
