package model.builder.core

import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyTable
import org.osgi.framework.BundleContext

class Util {

    static CyColumn createColumn(CyTable table, String name, Class<?> type,
                                 boolean immutable) {
        table.getColumn(name) ?: (table.createColumn(name, type, immutable))
        table.getColumn(name)
    }

    static Expando cyReference(BundleContext bc, Closure cyAct, ... cyInterfaces) {
        Expando e = new Expando()
        cyInterfaces.each {
            def impl = cyAct.call(bc, it)
            def name = it.simpleName
            e.setProperty(name[0].toLowerCase() + name[1..-1], impl)
        }
        e
    }
}
