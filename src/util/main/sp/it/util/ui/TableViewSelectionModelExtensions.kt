package sp.it.util.ui

import javafx.scene.control.TableView.TableViewSelectionModel

/**
 * Convenience method to make it easier to select given rows of the
 * TableView via its SelectionModel.
 * This method provides alternative to TableViewSelectionModel.selectIndices()
 * that requires array parameter. This method makes the appropriate conversions
 * and selects the items using List parameter
 *
 * After the method is invoked only the provided rows will be selected - it
 * clears any previously selected rows.
 */
fun TableViewSelectionModel<*>.clearAndSelect(selectedIndexes: List<Int>) {
   clearSelection()
   if (selectedIndexes.isNotEmpty()) selectIndices(selectedIndexes[0], *selectedIndexes.drop(1).toIntArray())
}

/** Inverts the selection. Selected items will be not selected and vice versa.  */
fun TableViewSelectionModel<*>.selectInverse() {
   val selected = selectedIndices
   val size = tableView.items.size
   val inverse = ArrayList<Int>()
   for (i in 0 until size) if (!selected.contains(i)) inverse.add(i)
   clearAndSelect(inverse)
}

/** Selects no items. Equivalent to [clearSelection()] */
fun TableViewSelectionModel<*>.selectNone() {
   clearSelection()
}