package sp.it.pl.gui.itemnode.textfield

import javafx.scene.control.TextField
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.pl.main.APP
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.onEventDown
import kotlin.properties.Delegates.observable

/**
 * Customized [TextField] that displays a nullable object value. Normally a non-editable text
 * field that brings up a popup picker for its item type. Useful as an editor with value selection feature.
 *
 * In addition there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 *
 * @param <T> type of the value
 */
abstract class ValueTextField<T>(textValueConverter: (T?) -> String = APP.converter.ui::toS): DecoratedTextField() {

   /** Behavior executing when value changes. */
   val onValueChange = Handler1<T?>()
   /** Value to string converter. */
   val textValueConverter = textValueConverter
   /** Value. */
   var value by observable<T?>(null) { _, ov, nv ->
      if (ov!=nv) {
         text = textValueConverter(nv)
         promptText = text
         onValueChange(nv)
      }
   }

   init {
      styleClass += STYLECLASS
      isEditable = false
      text = textValueConverter(null)
      promptText = text

      right.value = ArrowDialogButton().apply {
         onEventDown(MOUSE_CLICKED, PRIMARY) { onDialogAction() }
      }
   }

   /** Behavior to be executed on dialog button click. Normally, on success it sets [value]. */
   protected abstract fun onDialogAction()

   companion object {
      const val STYLECLASS = "value-text-field"
   }

}