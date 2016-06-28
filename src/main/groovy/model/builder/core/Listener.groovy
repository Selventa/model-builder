package model.builder.core

import groovy.transform.TupleConstructor
import org.cytoscape.model.events.NetworkAboutToBeDestroyedEvent
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener

@TupleConstructor
class Listener implements NetworkAboutToBeDestroyedListener {

    final Expando cyRef

    @Override
    void handleEvent(NetworkAboutToBeDestroyedEvent ev) {
        def networkSUID = ev.network.SUID
        def mgr = cyRef.cyTableManager
        def revTable = mgr.globalTables.find {it.title == 'SDP.Revisions'}

        if (revTable) {
            def orphaned = revTable.allRows.
            each { row ->
                def suids = row.getList('networks.SUID', Long.class, [])
                suids.removeAll([networkSUID])
            }.
            findAll { row ->
                row.getList('networks.SUID', Long.class, []).empty
            }.
            collect { row ->
                row.get('uri', String.class)
            }.
            flatten()

            revTable.deleteRows(orphaned.unique())
        }
    }
}
