package model.builder.core.rcr

import org.cytoscape.model.CyRow
import org.cytoscape.model.CyTable

public interface CyTableView<K, T> {

    public CyTable getTable()

    public CyRow addObject(T obj)

    public boolean exists(K key)

    public T getObj(K key)

    public List<T> getAll()

    public CyRow removeObj(K key)
}