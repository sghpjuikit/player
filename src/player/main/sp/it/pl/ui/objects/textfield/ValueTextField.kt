package sp.it.pl.ui.objects.textfield

import javafx.scene.control.TextField
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import kotlin.properties.Delegates.observable
import sp.it.pl.main.APP
import sp.it.pl.ui.showContextMenu
import sp.it.util.access.editable
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync

/**
 * Customized [TextField] that displays a nullable object value. Normally a non-editable text
 * field that brings up a popup picker for its item type. Useful as an editor with value selection feature.
 *
 * In addition, there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 *
 * @param <T> type of the value
 */
abstract class ValueTextField<T>(initialValue: T? = null, textValueConverter: (T?) -> String = APP.converter.ui::toS): SpitTextField() {

   /** Behavior executing when value changes */
   val onValueChange = Handler1<T?>()
   /** Value to string converter */
   val textValueConverter = textValueConverter
   /** Value */
   var value by observable<T?>(initialValue) { _, ov, nv ->
      if (ov!=nv) {
         val c = caretPosition
         val t = textValueConverter(nv)
         if (t!=text) text = t
         if (t!=promptText) promptText = text
         if (c!=caretPosition) positionCaret(c)
         onValueChange(nv)
      }
   }

   init {
      styleClass += STYLECLASS
      isEditable = false
      text = textValueConverter(initialValue)
      promptText = text

      // custom context menu
      contextMenuShower = { showContextMenu(this, it, this::getText, this::value) }

      right setToOne ArrowDialogButton().apply {
         editable sync { isDisable = !it }
         onEventDown(MOUSE_CLICKED, PRIMARY) { if (isEditable) onDialogAction() }
      }
   }

   /** Behavior to be executed on dialog button click. Normally, on success it sets [value]. */
   protected abstract fun onDialogAction()

   companion object {
      const val STYLECLASS = "value-text-field"
   }

}