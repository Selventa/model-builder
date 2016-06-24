package model.builder.ui.api

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.application.CyApplicationManager

import javax.swing.JDialog

interface Dialogs {

    JDialog pathSearch(CyApplicationManager appMgr, AuthorizedAPI api, Map controls,
                       Closure addEdges)

    JDialog pathFacet(CyApplicationManager appMgr, Iterator<Map> itemIterator,
                      Closure denormalize, Closure addEdges)
}
