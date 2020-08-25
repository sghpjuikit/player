package sp.it.pl.ui.objects.table

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.TableCell
import javafx.scene.text.TextAlignment.LEFT
import javafx.scene.text.TextAlignment.RIGHT
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.type.isSubclassOf

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
         padding = Insets.EMPTY
      }

      override fun updateItem(item: X, empty: Boolean) {
         super.updateItem(item, empty)
         val row = if (empty) null else tableRow
         val rowItem = row?.item
         text = if (rowItem==null) "" else f.toS(rowItem, item, "")
      }
   }
}