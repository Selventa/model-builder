package model.builder.core.rcr

import org.cytoscape.model.CyColumn
import org.cytoscape.model.CyRow

abstract class AbstractCyTableView<K, T> implements CyTableView<K, T> {

    @Override
    boolean exists(K key) {
        getTable().rowExists(key)
    }

    @Override
    List<T> getAll() {
        CyColumn primaryKey = getTable().primaryKey
        // XXX Here I am assuming the (K)ey type is a supported CyTable type.
        getTable().allRows.collect {
            it.get(primaryKey.name, K.class)
        }.collect {
            getObj(it)
        }
    }

    @Override
    CyRow removeObj(K key) {
        CyRow row = getTable().getRow(key)
        getTable().deleteRows([key]) ? row : null
    }
}
