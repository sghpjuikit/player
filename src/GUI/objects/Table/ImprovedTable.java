/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects.Table;

import java.util.List;
import javafx.scene.control.TableView;
import utilities.Util;

/**
 *
 * @author Plutonium_
 */
public class ImprovedTable<T> extends TableView<T> {
    
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
    
}
