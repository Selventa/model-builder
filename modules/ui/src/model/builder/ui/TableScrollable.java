package model.builder.ui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.AdvancedTableModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.Iterator;
import java.util.List;

abstract class TableScrollable<T> extends JScrollPane implements TableFormat<T>, ChangeListener {

    private EventList<T> rows;

    public TableScrollable() {
        super();

        rows = new BasicEventList<T>();
        AdvancedTableModel<T> model = new DefaultEventTableModel<T>(rows, this);
        JTable table = new JTable(model);

        setViewportView(table);
        viewport.addChangeListener(this);
    }

    abstract List<T> loadMoreRows(int numberOfRows);

    public void setRows(final List<T> rows) {
        final EventList<T> thisRowList = this.rows;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (rows == null) {
                    clearRows();
                    return;
                }
                thisRowList.addAll(rows);
            }
        });
    }

    public void clearRows() {
        final EventList<T> thisRowList = this.rows;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Iterator<T> it = thisRowList.iterator();
                while (it.hasNext()) it.remove();
            }
        });
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        final EventList<T> thisRowList = this.rows;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                List<T> more = loadMoreRows(10);
                thisRowList.addAll(more);
            }
        });

//        println("view position: ${viewport.viewPosition}\n" +
//                "view rect: ${viewport.viewRect}\nview size: ${viewport.viewSize}\n" +
//                "extent size: ${viewport.extentSize}\n" +
//                "view coords (point): ${viewport.toViewCoordinates(viewport.viewPosition)}\n" +
//                "view coords (rect): ${viewport.toViewCoordinates(viewport.viewSize)}\n")
    }
}
