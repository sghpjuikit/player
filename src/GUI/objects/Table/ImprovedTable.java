/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import java.util.List;
import javafx.collections.ListChangeListener;
import javafx.geometry.Pos;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
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
    
    public List<T> getSelectedItems() {
        return getSelectionModel().getSelectedItems();
    }
    
    public List<T> getSelectedItemsCopy() {
        return Util.copySelectedItems(this);
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
    
    public void refreshColumn(TableColumn c) {
        Callback cf = c.getCellFactory();
        c.setCellFactory(null);
        c.setCellFactory(cf);
    }
    
    protected Callback<TableColumn<T,Void>, TableCell<T,Void>> buildIndexColumnCellFactory() {
        return (column -> new TableCell<T,Void>() {
            { 
                setAlignment(Pos.CENTER_RIGHT);
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText("");
                } else {
                    int i = 1+getIndex();
                    setText((zero_pad ? Util.zeroPad(i, getItems().size(),'0') : i) + ".");
                }
            }
        });
    }
}
