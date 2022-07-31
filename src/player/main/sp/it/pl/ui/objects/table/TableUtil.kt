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
import javafx.scene.layout.Region.USE_COMPUTED_SIZE
import javafx.scene.text.Font
import javafx.scene.text.TextAlignment.LEFT
import javafx.scene.text.TextAlignment.RIGHT
import sp.it.pl.ui.objects.table.PlaylistTable.CELL_PADDING
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.dev.Experimental
import sp.it.util.functional.asIs
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.type.Util.getFieldValue
import sp.it.util.type.Util.invokeMethodP1
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.Util.computeTextWidth
import sp.it.util.ui.lookupChildAs
import sp.it.util.ui.width

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

/** Font of the given column header or null if not yet initialized */
fun TableColumn<*, *>.insetsOrZero(): Double {
   val headerRow = tableView?.headerOrNull
   val headerCell = if (headerRow==null) null else invokeMethodP1(headerRow, "getColumnHeaderFor", TableColumnBase::class.java, this) as TableColumnHeader?
   val headerCellLabel = if (headerCell==null) null else getFieldValue<Label>(headerCell, "label")
   return (headerCellLabel?.insets?.width ?: 0.0)
}

/** @return [TableRow]s of this table. If the table skin is not initialized, it may be empty */
fun <T> TableView<T>.rows(): List<TableRow<T>> {
   return runTry {
      lookupChildAs<VirtualFlow<IndexedCell<T>>>()
         .childrenUnmodifiable[0].asIs<Parent>()
         .childrenUnmodifiable[0].asIs<Group>().children
   }.orNull().orEmpty().asIs()
}

@Experimental("Unclear when to call - requires populated and showing table - should be TableResizePolicy")
fun <T> TableView<T>.autoResizeColumns() {
   val rows = rows().associate { it.index to it.childrenUnmodifiable.filterIsInstance<TableCell<Any?,Any?>>().associateBy { it.tableColumn } }
   runTry {
      columns.forEach { column ->
         column.prefWidth = USE_COMPUTED_SIZE // having prefWidth set would interfere with width calculation, reset it
         column.prefWidth = maxOf(
            10.0,
            computeTextWidth(column.fontOrNull, column.text.orEmpty()) + column.insetsOrZero(),
            rows.keys.maxOfOrNull { rows[it]!![column.asIs()]!!.let { c -> computeTextWidth(c.font, c.text.orEmpty()) + (c.insets?.width ?: 0.0) } } ?: 0.0
         )
      }
   }
}