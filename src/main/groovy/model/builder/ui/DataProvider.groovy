package model.builder.ui

public interface DataProvider<T> extends Iterator<List<T>> {

    public int getDataSize();

    public List<T> getInitialData();


}