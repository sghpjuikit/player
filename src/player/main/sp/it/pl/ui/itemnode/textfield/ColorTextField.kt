package sp.it.pl.ui.itemnode.textfield

import javafx.scene.control.ColorPicker
import javafx.scene.paint.Color
import sp.it.pl.main.APP
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.editable
import sp.it.util.async.runLater
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncTo
import sp.it.util.ui.minPrefMaxWidth

/** [ValueTextField] for [Color] using [ColorPicker]. */
class ColorTextField: ValueTextField<Color>() {
   private var popup: PopWindow? = null
   private var colorPicker = ColorPicker().apply { minPrefMaxWidth = 40.emScaled }
   private var valueChanging = Suppressor()

   init {
      styleClass += "color-text-field"
      isEditable = true
      right.value = colorPicker

      textProperty() attach {
         valueChanging.suppressed {
            APP.converter.general.ofS<Color>(it).ifOk {
               runLater {  // TODO: remove, this is workaround for writing rgba(0,0,0,0) and this change causing internal text field skin exception
                  value = it
               }
            }
         }
      }

      onValueChange += { colorPicker.value = it ?: Color.WHITE }
      colorPicker.valueProperty() attach { valueChanging.suppressing { value = it } }
      editable syncTo colorPicker.editableProperty()
   }

   override fun onDialogAction() {}
}