/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utilities;

import java.util.List;
import javafx.scene.control.TableView.TableViewSelectionModel;

/**
 *
 * @author uranium
 */
public final class TableUtil {

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
     * <p>
     * After the method is invoked only the provided rows will be selected - it
     * clears any previously selected rows.
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
