package model.builder.core

import org.cytoscape.model.*
import org.openbel.framework.common.InvalidArgument
import org.openbel.framework.common.enums.RelationshipType
import org.openbel.framework.common.model.Term

import static model.builder.core.Activator.CY
import static model.builder.core.Constant.STYLE_NAMES
import static model.builder.core.Constant.STYLE_PATH
import static org.cytoscape.model.CyEdge.INTERACTION
import static org.cytoscape.model.CyEdge.Type.DIRECTED
import static org.cytoscape.model.CyNetwork.NAME
import static org.openbel.framework.common.bel.parser.BELParser.parseTerm
import static org.openbel.framework.common.enums.RelationshipType.fromAbbreviation
import static org.openbel.framework.common.enums.RelationshipType.fromString

class Util {

    static contributeVisualStyles() {
        // delete/add knowledge network styles (idempotent)
        def vmm = CY.visualMappingManager
        def lvf = CY.loadVizmapFileTaskFactory
        vmm.allVisualStyles.findAll { it.title in STYLE_NAMES }.each(vmm.&removeVisualStyle)
        lvf.loadStyles(Util.class.getResourceAsStream(STYLE_PATH))
    }

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

    static synchronized CyTable createTable(String name, String key, Class<?> type,
                               boolean pub, boolean mut,
                               CyTableManager manager, CyTableFactory factory) {
        def table =
                manager.getAllTables(false).find { it.title == name } ?:
                factory.createTable(name, key, type, pub, mut)
        manager.addTable(table)
        table
    }

    static CyTable getTable(String name) {
        CY.cyTableManager.getAllTables(false).find { name == it.title }
    }

    static void copyColumns(CyTable from, CyColumn fromKey,
                            CyTable to, CyColumn toKey, Boolean immutable = null) {
        from.columns.each {
            CyColumn fromCol ->
                if (to.getColumn(fromCol.name)) {
                    to.deleteColumn(fromCol.name)
                }
                to.createColumn(fromCol.name, fromCol.type,
                        (immutable != null ? immutable : fromCol.immutable),
                        fromCol.defaultValue)
        }

        to.allRows.each {
            CyRow toRow ->
                blankCells(toRow, from)
                Object toKeyValue = toRow.get(toKey.name, toKey.type)
                Collection<CyRow> matches = from.getMatchingRows(fromKey.name, toKeyValue)
                if (matches) {
                    CyRow fromRow = matches.first()
                    fromRow.getAllValues().each {
                        toRow.set(it.key, it.value)
                    }
                }
        }
    }

    static void removeColumns(CyTable from, CyTable containedIn) {
        containedIn.columns.each {
            CyColumn targetCol ->
                from.deleteColumn(targetCol.name)
        }
    }

    static void blankCells(CyRow row, CyTable columnsFrom) {
        columnsFrom.columns.each {
            if (row.isSet(it.name)) {
                row.set(it.name, null)
            }
        }
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

    static void setMetadata(Map data, CyIdentifiable cyObj, CyTable cyTable) {
        setMetadata([data], [cyObj], cyTable)
    }

    /**
     * For CyNetwork, CyNode or CyEdge (multiple maps)
     */
    static void setMetadata(List data, List cyObjects, CyTable table) {
        if (!cyObjects)
            throw new NullPointerException("cyObjects cannot be null")

        cyObjects = [cyObjects].flatten()
        if (cyObjects.any { it == null })
            throw new NullPointerException("cyObjects cannot contain nulls")

        data = [data].flatten()
        if (data.size() != cyObjects.size())
            throw new IllegalArgumentException("data and cyObjects do not have same cardinality")

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
            if (!metadata) return

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
                    if (col.type == Double.class && v instanceof Integer)
                        v = v.toDouble()
                    row.set(k, v)
                }
            }
        }
    }

    static Map gather(List data) {
        data.inject([:].withDefault {[]}) { initial, next ->
            if (next) {
                next.each { k, v ->
                    initial[k] << v
                }
            }
            initial
        }
    }

    static Class<?> classReduce(List values) {
        if (!values) return

        // map values to their classes; drop null values
        def nonNullClasses = values.
                findAll { it != null && "$it" != 'null' }.
                groupBy { it?.class }

        // up-convert to BigDecimal for numerical values
        if (nonNullClasses.size() == 2 &&
                nonNullClasses.containsKey(Integer.class) &&
                nonNullClasses.containsKey(Double.class)) {
            return Double.class
        }

        // if value domain is empty or has multiple types; generalize as String
        if (nonNullClasses.isEmpty() || nonNullClasses.size() > 1) return String.class

        // return only class
        nonNullClasses.keySet().first()
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

    static def getOrCreateNode(CyNetwork cyN, String label) {
        getNode(cyN, label) ?: createNode(cyN, label)
    }

    static def getOrCreateEdge(CyNetwork cyN, src, rel, tgt) {
        def nodeSource = getOrCreateNode(cyN, src)
        if (!nodeSource) return null
        def nodeTarget = getOrCreateNode(cyN, tgt)
        if (!nodeTarget) return null
        RelationshipType rtype = fromString(rel)
        if (!rtype) rtype = fromAbbreviation(rel)
        if (!rtype) return null

        cyN.getConnectingEdgeList(nodeSource, nodeTarget, DIRECTED).find {
            def row = cyN.getRow(it)
            row.get(INTERACTION, String.class) == rtype.displayValue
        } ?: createEdge(cyN, nodeSource, nodeTarget, rel)
    }

    static def getNode(CyNetwork cyNetwork, String label) {
        def table = cyNetwork.defaultNodeTable
        table.getMatchingRows(NAME, label).
            collect { row ->
                long id = row.get(table.primaryKey.name, Long.class)
                if (!id) return null
                cyNetwork.getNode(id)
            }.find()
    }

    static def getEdge(CyNetwork cyNetwork, String source, String rel, String target) {
        def nodeSource = getNode(cyNetwork, source)
        if (!nodeSource) return null
        def nodeTarget = getNode(cyNetwork, target)
        if (!nodeTarget) return null
        RelationshipType rtype = fromString(rel)
        if (!rtype) rtype = fromAbbreviation(rel)
        if (!rtype) return null

        cyNetwork.getConnectingEdgeList(nodeSource, nodeTarget, DIRECTED).find {
            def row = cyNetwork.getRow(it)
            row.get(INTERACTION, String.class) == rtype.displayValue
        }
    }

    static def createNode(CyNetwork cyN, String label) {
        cyN.defaultNodeTable.getColumn('bel.function') ?:
            cyN.defaultNodeTable.createColumn('bel.function', String.class, false)
        cyN.addNode().with {
            cyN.getRow(it).set(NAME, label)
            cyN.getRow(it).set('bel.function', (toBEL(label).fx ?: '') as String)
            return it
        }
    }

    private static def createEdge(CyNetwork cyN, CyNode s, CyNode t, String rel) {
        cyN.addEdge(s, t, true).with {
            cyN.getRow(it).set(INTERACTION, rel)
            return it
        }
    }

    static def toBEL(String label) {
        try {
            Term term = parseTerm(label)
            if (!term) return [:]
            return [fx: term.functionEnum, lbl: label]
        } catch (InvalidArgument e) {
            // parse failure; cannot resolve so return
            return [:]
        }
    }

    static Integer convertToInteger(Object obj) {
        if (obj instanceof Integer) {
            ((Integer) obj)
        } else if (obj instanceof BigDecimal) {
            ((BigDecimal) obj).intValue()
        } else if (obj instanceof Double) {
            ((Double) obj).intValue()
        } else if (obj instanceof Float) {
            ((Float) obj).intValue()
        } else if (obj instanceof String) {
            try {
                ((String) obj).toInteger()
            } catch (NumberFormatException e) {
                return null
            }
        } else {
            null
        }
    }

    static Double convertToDouble(Object obj) {
        if (obj instanceof Double) {
            ((Double) obj)
        } else if (obj instanceof BigDecimal) {
            ((BigDecimal) obj).doubleValue()
        } else if (obj instanceof Float) {
            ((Float) obj).doubleValue()
        } else if (obj instanceof String) {
            try {
                ((String) obj).toDouble()
            } catch (NumberFormatException e) {
                return null
            }
        } else {
            null
        }
    }
}
