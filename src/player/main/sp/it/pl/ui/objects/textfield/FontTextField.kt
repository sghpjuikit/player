package sp.it.pl.ui.objects.textfield

import javafx.scene.text.Font
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.picker.FontPicker
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.util.async.runLater
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncFrom

/** [ValueTextField] for [Font]. */
class FontTextField: ValueTextField<Font>() {
   private var picker: FontPicker? = null
   private var valueChanging = Suppressor()

   init {
      styleClass += STYLECLASS
      isEditable = true
      textProperty() attach {
         if (!valueChanging.isSuppressed) {
            valueChanging.isSuppressed = true
            runLater {
               APP.converter.general.ofS<Font>(it).ifOk {
                  value = it
                  valueChanging.isSuppressed = false
               }
            }
         }
      }
   }

   override fun onDialogAction() {
      val d = Disposer()
      val pc = picker ?: FontPicker { valueChanging.suppressing { value = it } }.apply {
         picker = this
         pickerContent.font = this@FontTextField.value ?: Font.getDefault()
         editable syncFrom this@FontTextField.editableProperty() on d
         popup.onHiding += {
            d()
            picker = null
         }
      }

      pc.popup.show(RIGHT_CENTER(this))
   }

   companion object {
      const val STYLECLASS = "font-text-field"
   }

}