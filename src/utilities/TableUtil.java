/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;

/**
 *
 * @author uranium
 */
public final class TableUtil {

    /**
     * Removes all selected items from TableView's table. Does nothing if table
     * is null, empty or nothing is selected.
     * @param t TableView to remove from.
     */
    public static void removeSelectedItems(TableView<?> t) {
        if (t == null || t.getItems().isEmpty() || t.getSelectionModel().getSelectedIndices().isEmpty()) {
            return;
        }
        List<Integer> items = new ArrayList<>();
        items.addAll(t.getSelectionModel().getSelectedIndices());
        t.getSelectionModel().clearSelection();
        for (int i = items.size() - 1; i >= 0; i--) {
            t.getItems().remove(items.get(i).intValue());
        }
    }

    /**
     * Removes all items with any index from specified index list. Does nothing 
     * if either list or list of indexes is either null or empty.
     * @param list
     * @param indexes
     */
    public static void removeItems(List<?> list, List<Integer> indexes) {
        if (list == null || indexes == null || list.isEmpty() || indexes.isEmpty()) {
            return;
        }
        for (int i = indexes.size() - 1; i >= 0; i--) {
            list.remove(indexes.get(i).intValue());
        }
    }

    /**
     * Convenience method to make it easier to select given rows of the
     * TableView via its SelectionModel.
     * This methods provides alternative to TableViewSelectionModel.selectIndices()
     * that requires array parameter. This method makes the appropriate conversions
     * and selects the items using List parameter
     * @param selectedIndexes
     * @param selectionModel
     */
    public static void selectRows(List<Integer> selectedIndexes, TableViewSelectionModel<?> selectionModel) {
        selectionModel.clearSelection();
        int[] newSelected = new int[selectedIndexes.size()];
        for (int i = 0; i < selectedIndexes.size(); i++) {
            newSelected[i] = selectedIndexes.get(i);
        }
        if (newSelected.length != 0) {
            selectionModel.selectIndices(newSelected[0], newSelected);
        }
    }
    
}
