package model.builder.ui.api

import model.builder.web.api.AuthorizedAPI
import org.cytoscape.application.CyApplicationManager

import javax.swing.JDialog

interface Dialogs {

    JDialog pathSearch(CyApplicationManager appMgr, AuthorizedAPI api, Map controls)

    JDialog pathFacet(Iterator<Map> itemIterator, Closure denormalizeClosure)
}
