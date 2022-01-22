package sp.it.pl.ui.objects

import javafx.beans.property.Property
import javafx.scene.control.ComboBox
import javafx.scene.control.ListCell
import javafx.scene.control.ListView
import javafx.scene.control.skin.ComboBoxListViewSkin
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.util.Callback
import javafx.util.StringConverter
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.ui.objects.search.Search
import sp.it.util.access.V
import sp.it.util.functional.asIs
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp

/**
 * ComboBox with added functionalities.
 *  * null-safe converter [toStringConverter] and [emptyText] used as [toStringConverterFx] for [ComboBox.converterProperty]
 *  * [KeyCode.SPACE] shows popup
 *  * Searching & scrolling when typing, [search]
 *  * [readOnly]
 */
class SpitComboBox<T>(toStringConverter: (T) -> String, emptyText: String = textNoVal): ComboBox<T>() {
   /** Whether this combobox is not editable. Default false. Note: [ComboBox.editable] refers to [ComboBox.commitValue]. */
   val readOnly: Property<Boolean> = V(false)
   /** Text for when no value is selected. Default `"<none>"`  */
   val emptyText: String = emptyText
   /** String converter for cell value factory if non-null. Default is Object::toString  */
   val toStringConverter: (T) -> String = toStringConverter
   /** String converter for cell value factory. Delegates to [toStringConverter] and [emptyText]. Set to [converterProperty].  */
   val toStringConverterFx: StringConverter<T?> = object: StringConverter<T?>() {
      override fun toString(o: T?): String = if (o==null) emptyText else toStringConverter(o)
      override fun fromString(string: String): T? = null
   }
   /** Item search. Has no graphics.  */
   val search: Search = object: Search() {
      public override fun doSearch(query: String) {
         val items = skinPopupListView()

            // scroll to match
            if (!getItems().isEmpty()) {
               for (i in getItems().indices) {
                  val e = getItems()[i]
                  val es = toStringConverterFx.toString(e)
                  if (isMatchNth(es, query)) {
                  items.scrollTo(i)
                     // items.getSelectionModel().select(i); // TODO: make this work reasonably well
                     break
               }
            }
         }
      }

      public override fun isMatch(text: String, query: String) = text.contains(query, true)
   }

   init {
      converter = toStringConverterFx // we need to set the converter specifically or the combobox cell won't get updated sometimes
      cellFactory = Callback { ImprovedComboBoxListCell(this) }
      buttonCell = cellFactory.call(null)
      value = null

      // search
      search.installOn(this)

      // readonly
      onEventUp(MOUSE_RELEASED) { if (readOnly.value) it.consume() }
      onEventUp(MOUSE_PRESSED) { if (readOnly.value) requestFocus() }

      // SPACE shows popup
      onEventDown(KEY_PRESSED) {
         if (!it.isConsumed && (!readOnly.value || it.isShortcutDown) && it.code==SPACE && !isShowing && !items.isEmpty()) {
            show()
            skinPopupListView().requestFocus()
         }
      }
   }

   private fun skinPopupListView(): ListView<T> = skin.asIs<ComboBoxListViewSkin<T>>().popupContent.asIs()

   open class ImprovedComboBoxListCell<T>(comboBox: SpitComboBox<T>): ListCell<T>() {
      // do not extend ComboBoxListCell! causes problems!
      val converter: StringConverter<T?>

      override fun updateItem(item: T, empty: Boolean) {
         super.updateItem(item, empty)
         text = converter.toString(if (empty) null else item)
      }

      init {
         converter = comboBox.toStringConverterFx
      }
   }

}