package model.builder.core

import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyIdentifiable
import org.cytoscape.model.CyNetwork
import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable
import org.osgi.framework.BundleContext

class Util {

    static CyColumn createColumn(table, name, type, immutable, defaultValue) {
        name = "$name"
        table.getColumn(name) ?: (table.createColumn(name, type, immutable, defaultValue))
        table.getColumn(name)
    }

    static CyColumn createListColumn(table, name, listElementType, immutable,
                                     defaultValue) {
        name = "$name"
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

    /**
     * For CyNetwork, CyNode or CyEdge (multiple maps)
     */
    static void setMetadata(List data, List cyObjects, CyTable table) {
        Map gathered = gather(data)
        Map columns = gathered.collectEntries { k, v ->
            [k, classReduce(v)]
        }.
        collectEntries { k, v ->
            switch(v) {
                case List:
                    def clazz = classReduce(gathered[k].flatten()) ?: String.class
                    [k, createListColumn(table, k, clazz, false, [])]
                    break
                default:
                    v = (v && "$v" != 'null') ? v : String.class
                    [k, createColumn(table, k, v, false, null)]
            }
        }
        [data, cyObjects].transpose().each {
            def (metadata, CyIdentifiable cyObj) = it
            metadata.each { k, v ->
                def col = columns[k] as CyColumn
                CyRow row = table.getRow(cyObj.SUID)
                if(col.listElementType) {
                    def values = row.getList(k, col.listElementType, [])
                    if (v != null && v instanceof List) {
                        values.addAll(v.collect {
                            (it == null || "$it" == "null") ? null : it
                        })
                    }
                    row.set(k, values)
                } else {
                    v = (v == null || "$v" == "null") ? null : v
                    row.set(k, v)
                }
            }
        }
    }

    static Map gather(List data) {
        data.inject([:].withDefault {[]}) { initial, next ->
            next.each { k, v ->
                initial[k] << v
            }
            initial
        }
    }

    static Class<?> classReduce(values) {
        if (!values) return
        values.find { it != null && "$it" != 'null' }?.class as Class<?>
    }

    static CyColumn addData(String k, Object v, CyNetwork cyN,
                            CyIdentifiable id, CyTable table) {
        CyColumn col = toColumn(table, k, v)
        CyRow r = cyN.getRow(id)
        v = (v == null || "$v" == "null") ? null : v
        if(col.listElementType) {
            def values = r.getList(k, col.listElementType, [])
            if (v != null && v.class.isAssignableFrom(col.listElementType))
                values.addAll(v)
            r.set(k, values)
        } else {
            r.set(k, v)
        }
        col
    }

    static Map rowData(CyIdentifiable id, CyNetwork cyN, CyTable table) {
        cyN.getRow(id).allValues.collectEntries { k, v ->
            def col = table.getColumn(k)
            if (col.listElementType && v == null) {
                [k, []]
            } else {
                [k, v]
            }
        }
    }

    static CyColumn toColumn(CyTable table, String name, Object val) {
        if (!name) return null

        // read value's class; if val type of JSONObject$Null then default to
        // String class
        Class<?> c = (val == null || "$val" == "null") ? String.class : val.class
        if (val instanceof List) {
            c = val.isEmpty() ? String.class : val.first().class
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
                def up = [(0.0..0.001):  '#FFA000', (0.001..0.005): '#FFC800',
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
