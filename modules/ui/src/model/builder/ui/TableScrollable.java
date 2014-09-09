package model.builder.ui;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.AdvancedTableModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.search.TableSearchable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.RowSorter.SortKey;
import java.util.*;

import static java.util.Arrays.asList;

abstract class TableScrollable<T> extends JScrollPane implements TableFormat<T>, ChangeListener {

    private EventList<T> rows;
    private boolean fullRead = false;
    private volatile boolean busyLoad = false;

    public TableScrollable() {
        super();

        rows = new BasicEventList<T>();
        AdvancedTableModel<T> model = new DefaultEventTableModel<T>(rows, this);
        JXTable table = new JXTable(model);
        table.setSearchable(new TableSearchable(table));
        table.setColumnControlVisible(true);
        table.getRowSorter().setSortKeys(asList(new SortKey(0, SortOrder.ASCENDING)));

        setViewportView(table);
        viewport.addChangeListener(this);
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
