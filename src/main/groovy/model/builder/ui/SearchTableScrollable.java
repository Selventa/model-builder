package model.builder.ui;

import ca.odell.glazedlists.swing.DefaultEventTableModel;
import groovy.util.Expando;
import model.builder.web.api.SearchProvider;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.search.TableSearchable;

import javax.swing.*;
import javax.swing.RowSorter.SortKey;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

public class SearchTableScrollable extends TableScrollable<Expando> {

    private static final String[] COLUMNS = new String[] { "Name", "Description", "Tags", "Created", "Updated" };
    private static final String[] PROPERTIES = new String[] { "name", "description", "tags", "createdAt", "updatedAt" };
    private SearchProvider searchProvider;

    public SearchTableScrollable() {
        super();

        JXTable table = new JXTable(new DefaultEventTableModel<Expando>(rows, this));
        table.setSearchable(new TableSearchable(table));
        table.setColumnControlVisible(true);
        table.getRowSorter().setSortKeys(asList(new SortKey(0, SortOrder.ASCENDING)));

        TextAreaRenderer longTextRenderer = new TextAreaRenderer();
        table.getColumnExt(0).setCellRenderer(longTextRenderer);
        table.getColumnExt(1).setCellRenderer(longTextRenderer);
        table.getColumnExt(2).setCellRenderer(longTextRenderer);

        setViewportView(table);
    }

    @Override
    public List<Expando> loadMoreRows(int numberOfRows) {
        List<Expando> rows = new ArrayList<Expando>(100);
        while (searchProvider.hasNext() && rows.size() < 100) {
            rows.add(searchProvider.next());
        }
        return rows;
    }

    public void setSearchProvider(SearchProvider searchProvider) {
        this.searchProvider = searchProvider;
        List<Expando> rows = new ArrayList<Expando>(100);
        while (searchProvider.hasNext() && rows.size() < 100) {
            rows.add(searchProvider.next());
        }
        setRows(rows);
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int i) {
        return COLUMNS[i];
    }

    @Override
    public Object getColumnValue(Expando t, int i) {
        Object prop = t.getProperty(PROPERTIES[i]);
        return prop == null ? "" : prop;
    }
}
