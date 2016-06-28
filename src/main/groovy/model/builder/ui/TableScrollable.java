package model.builder.ui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.*;
import java.util.List;

abstract class TableScrollable<T> extends JScrollPane implements TableFormat<T>, ChangeListener {

    protected final EventList<T> rows;
    private boolean fullRead = false;
    private volatile boolean busyLoad = false;
    private JTable table;

    public TableScrollable() {
        super();
        rows = new BasicEventList<T>();
        viewport.addChangeListener(this);
    }

    @Override
    public void setViewportView(Component view) {
        super.setViewportView(view);
        if (view == null || !(view instanceof JTable))
            throw new IllegalArgumentException("view must be a type of JTable");
        table = ((JTable) view);
    }

    abstract List<T> loadMoreRows(int numberOfRows);

    public void setRows(final List<T> rows) {
        final EventList<T> thisRowList = this.rows;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Iterator<T> it = thisRowList.iterator();
                while (it.hasNext()) {
                    it.next();
                    it.remove();
                }
                if (rows != null) thisRowList.addAll(rows);
            }
        });
        fullRead = false;
    }

    public void clearRows() {
        final EventList<T> thisRowList = this.rows;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Iterator<T> it = thisRowList.iterator();
                while (it.hasNext()) {
                    it.next();
                    it.remove();
                }
            }
        });
        fullRead = false;
    }

    public JTable getTable() {
        return table;
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        if (busyLoad || fullRead || this.rows.isEmpty()) return;

        double viewportY = viewport.toViewCoordinates(viewport.getViewPosition()).getY();
        double viewportHeight = viewport.getViewRect().getHeight();
        double tableHeight = viewport.toViewCoordinates(viewport.getViewSize()).getHeight();
        if (viewportY > ((tableHeight - viewportHeight) * 0.75)) {
            final EventList<T> thisRowList = this.rows;
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    List<T> more = loadMoreRows(100);
                    if (more == null || more.isEmpty()) {
                        fullRead = true;
                        busyLoad = false;
                        return;
                    }
                    thisRowList.addAll(more);
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {}
                    busyLoad = false;
                }
            });
        } else {
            busyLoad = false;
        }
    }
}
