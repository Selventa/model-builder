package model.builder.core.rcr

import org.cytoscape.work.util.ListSingleSelection

import static model.builder.core.rcr.Constant.SDP_URI_COLUMN

class Tunables {

    static ListSingleSelection<String> tunableScorePaintField() {
        new ListSingleSelection<String>(
                ScorePaintField.values().collect { it.toString()})
    }

    static ListSingleSelection<Expando> tunableRcrField() {
        RcrResourceTableView rcrTableView = new RcrResourceTableView()

        List<Expando> rcrRowObjs = rcrTableView.getAll().collect {
            Map rcr ->
                new Expando(
                        name    : rcr.name,
                        uri     : rcr[SDP_URI_COLUMN] ,
                        toString: {
                            "${rcr.name}"
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
