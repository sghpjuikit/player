/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import util.Util;
import static util.async.Async.run;

/**
 *
 * @author Plutonium_
 */
public class ImprovedTable<T> extends TableView<T> {
    
    final TableColumn<T,Void> columnIndex = new TableColumn("#");
    private final Callback<TableColumn<T,Void>, TableCell<T,Void>> indexCellFactory;
    boolean zero_pad = true;
    
    
    public ImprovedTable() {
        indexCellFactory = buildIndexColumnCellFactory();
        columnIndex.setCellFactory(indexCellFactory);
        columnIndex.setSortable(false);
        getColumns().add(columnIndex);
        
        ListChangeListener<T> refresher = o -> {
            // unfortunately this doesnt work, it requires delay
            // table.getColumnResizePolicy().call(new TableView.ResizeFeatures(table, columnIndex, 0d));
            run(100, () -> getColumnResizePolicy().call(new ResizeFeatures(this, columnIndex, 0d)));
        };
        itemsProperty().addListener((o,ov,nv) -> {
            ov.removeListener(refresher);
            nv.addListener(refresher);
        });
    }
    
    /** Set visibility of columns header. Default true. */
    public void setHeaderVisible(boolean val) {
        if(val) getStylesheets().remove(PlaylistTable.class.getResource("Table.css").toExternalForm());
        else    getStylesheets().add(PlaylistTable.class.getResource("Table.css").toExternalForm());
    }
    
    public boolean isTableHeaderVisible() {
        Pane header = (Pane)lookup("TableHeaderRow");
        return header==null ? true : header.isVisible();
    }
    
    public double getTableHeaderHeight() {
        Pane header = (Pane)lookup("TableHeaderRow");
        return header==null ? getFixedCellSize() : header.getHeight();
    }
    
    /** Returns selected items. */
    public List<T> getSelectedItems() {
        return getSelectionModel().getSelectedItems();
    }
    
    /** Returns copy of selected items (to prevent modification). */
    public List<T> getSelectedItemsCopy() {
        return new ArrayList(getSelectionModel().getSelectedItems());
    }
    
    /** Will add zeros to index numbers to maintain length consistency. */
    public void setZeropadIndex(boolean val) {
        zero_pad = val;
        refreshColumn(columnIndex);
    }
    
    /** @see #setZeropadIndex(boolean) */
    public boolean isZeropadIndex() {
        return zero_pad;
    }
    
    /** Refreshes given column. */
    public void refreshColumn(TableColumn c) {
        Callback cf = c.getCellFactory();
        // c.setCellFactory(null);                      // this no longer works (since 8u40 ?)
        c.setCellFactory( column->new TableCell());     // use this
        c.setCellFactory(cf);
    }
    
    /** Builds index column. */
    public TableColumn<T,Void> buildIndexColumn() {
        TableColumn<T,Void> c = new TableColumn("#");
                            c.setCellFactory(buildIndexColumnCellFactory());
                            c.setSortable(false);
        return c;
    }
    
    /** Builds index column cell factory. Called only once. */
    protected Callback<TableColumn<T,Void>, TableCell<T,Void>> buildIndexColumnCellFactory() {
        return (column -> new TableCell<T,Void>() {
            { 
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    int i = 1+getIndex();
                    setText((zero_pad ? Util.zeroPad(i, getItems().size(),'0') : i) + ".");
                }
            }
        });
    }
    
/************************************ SORT ************************************/
    
    /**
     * Sorts the items by the column. Sorting operates on table's sort
     * order and items backing the table remain unchanged. Sort order of 
     * the table is changed so specified column is primary sorting column and
     * other columns remain unaffected.
     * <p>
     * This is a programmatic equivalent of sorting the table manually by
     * clicking on columns' header (which operates through sort order).
     * <p>
     * Does not work when field's respective column is invisible - does nothing.
     * <p>
     * Note, that if the field must support sorting - return Comparable type.
     * 
     * @param field 
     */
    public void sortBy(TableColumn c, TableColumn.SortType type) {
        getSortOrder().remove(c);
        c.setSortType(type);
        getSortOrder().add(0, c);
    }
}
