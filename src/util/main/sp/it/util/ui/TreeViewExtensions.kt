package sp.it.util.ui

import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import sp.it.util.functional.traverse
import sp.it.util.math.max
import sp.it.util.math.min

/** @return true iff this is direct parent of the specified tree item */
fun <T> TreeItem<T>.isParentOf(child: TreeItem<T>) = child.parent===this

/** @return true iff this is direct or indirect parent of the specified tree item */
fun <T> TreeItem<T>.isAnyParentOf(child: TreeItem<T>) = generateSequence(child.parent) { it.parent }.any { it===this }

/** @return true iff this is direct child of the specified tree item */
fun <T> TreeItem<T>.isChildOf(parent: TreeItem<T>) = parent.isParentOf(this)

/** @return true iff this is direct or indirect child of the specified tree item */
fun <T> TreeItem<T>.isAnyChildOf(parent: TreeItem<T>) = parent.isAnyParentOf(this)

val <T> TreeItem<T>.root: TreeItem<T> get() = traverse { it.parent }.last()

fun <T> TreeItem<T>.expandToRoot() = generateSequence(this) { it.parent }.forEach { it.setExpanded(true) }

fun <T> TreeItem<T>.expandToRootAndSelect(tree: TreeView<in T>) = tree.expandToRootAndSelect(this)

@Suppress("UNCHECKED_CAST")
fun <T> TreeView<T>.expandToRootAndSelect(item: TreeItem<out T>) {
   item.expandToRoot()
   this.scrollToCenter(item as TreeItem<T>)
   selectionModel.clearAndSelect(getRow(item))
}

/** Scrolls to the row, so it is visible in the vertical center of the table. Does nothing if index out of bounds.  */
fun <T> TreeView<T>.scrollToCenter(i: Int) {
   var index = i
   val items = expandedItemCount
   if (index<0 || index>=items) return

   val fixedCellHeightNotSet = fixedCellSize<=0
   if (fixedCellHeightNotSet) {
      scrollTo(i)
      // TODO: improve
   } else {
      val rows = height/fixedCellSize
      index -= (rows/2).toInt()
      index = 0 max index min items - rows.toInt() + 1
      scrollTo(index)
   }
}

/** Scrolls to the item, so it is visible in the vertical center of the table. Does nothing if item not in table.  */
fun <T> TreeView<T>.scrollToCenter(item: TreeItem<T>) {
   item.expandToRoot()
   generateSequence(item) { it.parent }.toList().asReversed()
   scrollToCenter(getRow(item))
}