package model.builder.core

import org.cytoscape.model.CyNetwork
import org.cytoscape.work.util.ListSingleSelection

import static model.builder.core.Activator.CY
import static org.cytoscape.model.CyNetwork.NAME
import static org.cytoscape.model.CyNetwork.SELECTED

class Tunables {

    static ListSingleSelection<Expando> tunableNetwork() {
        new ListSingleSelection<Expando>(
            CY.cyNetworkManager.networkSet.sort {
                it.getRow(it).get(NAME, String.class)
            }.collect { CyNetwork network ->
                new Expando(
                        networks: [network],
                        toString: {
                            network.getRow(network).get(NAME, String.class)
                        }
                )
            }
        )
    }

    static ListSingleSelection<Expando> tunableNetworkCategory() {
        ListSingleSelection<Expando> networkTunable = tunableNetwork()
        List<Expando> choices = networkTunable.getPossibleValues()

        choices.add(0,
            new Expando(
                    networks: CY.cyNetworkManager.networkSet.findAll {
                        CyNetwork network ->
                        network.getRow(network).get(SELECTED, Boolean.class)
                    },
                    toString: { "Selected Networks" }
            )
        )
        choices.add(0,
            new Expando(
                    networks: CY.cyNetworkManager.networkSet,
                    toString: { "All Networks" }
            )
        )
        networkTunable.setPossibleValues(choices)

        // set current selected value based on CyNetwork selection
        List<CyNetwork> selNetworks = CY.cyApplicationManager.selectedNetworks
        if (selNetworks) {
            if (selNetworks.size() == 1) {
                CyNetwork cyN = selNetworks.first()
                networkTunable.selectedValue = new Expando(
                        networks: [cyN],
                        toString: {
                            cyN.getRow(cyN).get(NAME, String.class)
                        }
                )
            } else {
                networkTunable.selectedValue = networkTunable.possibleValues.find {
                    it.toString() == 'Selected Networks'
                }
            }
        }

        networkTunable
    }

    private Tunables() {
        //static accessors only
    }
}
