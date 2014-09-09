package model.builder.ui;

import groovy.util.Expando;
import model.builder.web.api.SearchProvider;

import java.util.ArrayList;
import java.util.List;

public class SearchTableScrollable extends TableScrollable<Expando> {

    private static final String[] COLUMNS = new String[] { "Name", "Tags", "Created", "Updated" };
    private static final String[] PROPERTIES = new String[] { "name", "tags", "createdAt", "updatedAt" };
    private SearchProvider searchProvider;

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
