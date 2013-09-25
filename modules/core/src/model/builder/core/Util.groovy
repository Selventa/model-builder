package model.builder.core

import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.osgi.framework.BundleContext

class Util {

    static CyColumn createColumn(CyTable table, String name, Class<?> type,
                                 boolean immutable, Object defaultValue) {
        table.getColumn(name) ?: (table.createColumn(name, type, immutable, defaultValue))
        table.getColumn(name)
    }

    static CyColumn createListColumn(CyTable table, String name,
                                     Class<?> listElementType, boolean immutable,
                                     List defaultValue) {
        table.getColumn(name) ?: (table.createListColumn(name, listElementType, immutable, defaultValue))
        table.getColumn(name)
    }

    static <T> void setAdd(CyRow row, String name, Class<T> type, T element) {
        def list = row.getList(name, type, [])
        if (!list.contains(element)) {
            list.add(element)
            row.set(name, list)
        }
    }

    static <T> void listAdd(CyRow row, String name, Class<T> type, T element) {
        def list = row.getList(name, type, [])
        list.add(element)
        row.set(name, list)
    }

    static void addMetadata(Map metadata, CyNetwork cyN, CyTable table) {
        Map columns = metadata.collectEntries {
            String k, Object v -> [k, toColumn(table, k, v)]
        }
        metadata.each { String k, Object v ->
            CyColumn col = columns[k]
            CyRow r = cyN.getRow(cyN)
            v = (v == null || "$v" == "null") ? null : v
            col.listElementType ? r.getList(k, col.listElementType, []) : r.set(k, v)
        }
    }

    static CyColumn toColumn(CyTable table, String name, Object val) {
        if (!name) return null

        // read value's class; if val type of JSONObject$Null then default to
        // String class
        Class<?> c = (val == null || "$val" == "null") ? String.class : val.class
        if (val instanceof List) {
            def l = (val as List)
            c = l[0].class
            return createListColumn(table, name, c, false, [])
        }
        createColumn(table, name, c, false, null)
    }

    static def concordanceColor(String direction, Double concordance) {
        switch(direction) {
            case 'Down':
                def down = [(0.0..0.001):  '#000666', (0.001..0.005): '#333399',
                            (0.005..0.01): '#6666CC', (0.01..0.05):   '#9999FF',
                            (0.05..0.1):   '#CCCCFF', (0.1..1):       '#FFFFFF']
                return down.find {it.key.containsWithinBounds(concordance)}?.value
            case 'Up':
                def up = [(0.0..0.001):'#FFA000', (0.001..0.005): '#FFC800',
                        (0.005..0.01): '#FFE800', (0.01..0.05):   '#FFF800',
                        (0.05..0.1):   '#FFFF99', (0.1..1):       '#FFFFFF']
                return up.find {it.key.containsWithinBounds(concordance)}?.value
            case 'None':
                return '#FFFFFF'
            default:
                return '#AAAAAA'
        }
    }

    static Expando cyReference(BundleContext bc, Closure cyAct, Class<?>[] cyInterfaces) {
        Expando e = new Expando()
        cyInterfaces.each {
            def impl = cyAct.call(bc, it)
            def name = it.simpleName
            e.setProperty(name[0].toLowerCase() + name[1..-1], impl)
        }
        e
    }
}
