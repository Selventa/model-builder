package model.builder.ui.api

import javax.swing.JDialog

interface Dialogs {

    JDialog pathFacetSearch(Iterator<Map> itemIterator, Closure denormalizeClosure)
}
