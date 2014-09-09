package model.builder.ui

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.AdvancedTableModel
import ca.odell.glazedlists.swing.DefaultEventTableModel

import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

abstract class TableScrollable<T> extends JScrollPane implements TableFormat<T>, ChangeListener {

    private JTable table;
    private AdvancedTableModel<T> model;
    private EventList<T> rows;

    public TableScrollable() {
        super

        rows = new BasicEventList<T>()
        model = new DefaultEventTableModel<T>(rows, this)
        table = new JTable(model)

        viewportView = table
        viewport.addChangeListener(this)
    }

    abstract List<T> loadMoreRows(int offset, int length)

    public void setRows(List<T> rows) {
        if (rows == null) {
            clearRows();
            return;
        }
    }

    public void clearRows() {
    }

    @Override
    void stateChanged(ChangeEvent e) {
        println("view position: ${viewport.viewPosition}\n" +
                "view rect: ${viewport.viewRect}\nview size: ${viewport.viewSize}\n" +
                "extent size: ${viewport.extentSize}\n" +
                "view coords (point): ${viewport.toViewCoordinates(viewport.viewPosition)}\n" +
                "view coords (rect): ${viewport.toViewCoordinates(viewport.viewSize)}\n")
    }
}
