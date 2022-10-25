package sp.it.pl.ui.objects.textfield

import javafx.scene.control.ColorPicker
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import sp.it.pl.main.APP
import sp.it.pl.main.emScaled
import sp.it.util.access.editable
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncTo
import sp.it.util.ui.minPrefMaxWidth

/** [ValueTextField] for [Color] using [ColorPicker]. */
class ColorTextField(initialValue: Color? = null): ValueTextField<Color>(initialValue) {
   private var picker = ColorPicker().apply { minPrefMaxWidth = 40.emScaled }
   private var valueChanging = Suppressor()

   init {
      styleClass += "color-text-field"
      isEditable = true
      right setToOne picker

      textProperty() attach {
         if (!valueChanging.isSuppressed) {
            valueChanging.isSuppressed = true
            APP.converter.general.ofS<Color>(it).ifOk { value = it }
            valueChanging.isSuppressed = false
         }
      }

      onValueChange += { picker.value = it }
      picker.valueProperty() attach { valueChanging.suppressing { value = it } }

      // readonly
      editable syncTo picker.editableProperty()
      picker.onEventUp(MouseEvent.ANY) { if (!isEditable) it.consume() }
   }

   override fun onDialogAction() {}
}