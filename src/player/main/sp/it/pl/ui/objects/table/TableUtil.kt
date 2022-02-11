package sp.it.pl.ui.objects.table

import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Group
import javafx.scene.Parent
import javafx.scene.control.IndexedCell
import javafx.scene.control.Label
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableColumnBase
import javafx.scene.control.TableRow
import javafx.scene.control.TableView
import javafx.scene.control.skin.TableColumnHeader
import javafx.scene.control.skin.TableHeaderRow
import javafx.scene.control.skin.VirtualFlow
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment.LEFT
import javafx.scene.text.TextAlignment.RIGHT
import sp.it.pl.ui.objects.table.PlaylistTable.CELL_PADDING
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.functional.asIs
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.type.Util.getFieldValue
import sp.it.util.type.Util.invokeMethodP1
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.lookupChildAs

/**
 * Use as cell factory for columns created in column factory.
 * * sets text using [ObjectField.toS]
 * * sets alignment to [CENTER_LEFT] for [String] and [CENTER_RIGHT] otherwise
 * * sets text alignment to [LEFT] for [String] and [RIGHT] otherwise
 */
fun <T, X> ObjectField<T, X>.buildFieldedCell(): TableCell<T, X> = let { f ->
   object: TableCell<T, X>() {
      init {
         alignment = if (f.type.isSubclassOf<String>()) CENTER_LEFT else CENTER_RIGHT
         textAlignment = if (f.type.isSubclassOf<String>()) LEFT else RIGHT
         padding = CELL_PADDING
      }

      override fun updateItem(item: X, empty: Boolean) {
         super.updateItem(item, empty)
         val row = if (empty) null else tableRow
         val rowItem = row?.item
         text = if (rowItem==null) "" else f.toS(rowItem, item, "")
      }
   }
}

/** Table header or null if not yet initialized */
val TableView<*>.headerOrNull: TableHeaderRow?
   get() = lookup("TableHeaderRow").asIs()

/** Font of the given column header or null if not yet initialized */
val TableColumn<*, *>.fontOrNull: Font?
   get() {
      val headerRow = tableView?.headerOrNull
      val headerCell = if (headerRow==null) null else invokeMethodP1(headerRow, "getColumnHeaderFor", TableColumnBase::class.java, this) as TableColumnHeader?
      val headerCellLabel = if (headerCell==null) null else getFieldValue<Label>(headerCell, "label")
      return headerCellLabel?.font
   }

/** @return [TableRow]s of this table. If the table skin is not initialized, it may be empty */
fun <T> TableView<T>.rows(): List<TableRow<T>> {
   return runTry {
      lookupChildAs<VirtualFlow<IndexedCell<T>>>()
         .childrenUnmodifiable[0].asIs<Parent>()
         .childrenUnmodifiable[0].asIs<Group>().children
   }.orNull().orEmpty().asIs()
}

