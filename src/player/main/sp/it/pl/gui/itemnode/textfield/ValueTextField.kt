package sp.it.pl.gui.itemnode.textfield

import javafx.beans.value.WritableValue
import javafx.scene.control.TextField
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.gui.objects.textfield.DecoratedTextField
import sp.it.util.functional.invoke
import sp.it.util.reactive.onEventDown
import java.util.function.BiConsumer

/**
 * Customized [TextField] that displays a nullable object value. Normally a non-editable text
 * field that brings up a popup picker for its item type. Useful as an editor with value selection feature.
 *
 * In addition there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 *
 * @param <T> type of the value
 */
abstract class ValueTextField<T>: DecoratedTextField, WritableValue<T> {

   constructor(textValueConverter: (T) -> String): super() {
      this.textValueConverter = textValueConverter

      styleClass += STYLECLASS
      isEditable = false
      text = nullText
      promptText = nullText

      right.value = ArrowDialogButton().apply {
         onEventDown(MOUSE_CLICKED, PRIMARY) { onDialogAction() }
      }
   }

   /** Value. */
   protected var vl: T? = null
   /** Behavior executing when value changes. */
   var onValueChange: BiConsumer<T?, T?> = BiConsumer { _, _ -> }
   /** Value to string converter. */
   private val textValueConverter: (T) -> String
   /** No value text. */
   private val nullText = "<none>"

   override fun setValue(value: T?) {
      if (vl==value) return

      val valueOld = vl
      vl = value
      text = value?.let { textValueConverter(it) } ?: nullText
      promptText = text
      onValueChange(valueOld, value)
   }

   /** @return currently displayed value */
   override fun getValue(): T? = vl

   /** Behavior to be executed on dialog button click. Should invoke of an [.setValue]. */
   protected abstract fun onDialogAction()

   companion object {
      const val STYLECLASS = "value-text-field"
   }

}