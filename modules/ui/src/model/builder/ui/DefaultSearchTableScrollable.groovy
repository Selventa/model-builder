package model.builder.ui

import static java.util.Collections.emptyList

class DefaultSearchTableScrollable<Expando> extends TableScrollable<Expando> {

    private final Closure<? extends List<Expando>> moreRows

    private static final String[] COLUMNS = [ "Name", "Tags", "Created", "Updated" ]
    private static final String[] PROPERTIES = [ "name", "tags", "created_at", "updated_at" ]

    public DefaultSearchTableScrollable(Closure<? extends List<Expando>> moreRows) {
        this.moreRows = moreRows ?: {int offset, int length -> emptyList()}
    }

    @Override
    List<Expando> loadMoreRows(int offset, int length) {
        moreRows.call(offset, length)
    }

    @Override
    int getColumnCount() {
        return COLUMNS.length
    }

    @Override
    String getColumnName(int i) {
        return COLUMNS[i]
    }

    @Override
    Object getColumnValue(Expando t, int i) {
        t.getAt(PROPERTIES[i]) ?: ""
    }
}
