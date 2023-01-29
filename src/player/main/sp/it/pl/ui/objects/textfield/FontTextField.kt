package sp.it.pl.ui.objects.textfield

import javafx.scene.text.Font
import sp.it.pl.ui.objects.picker.FontPicker
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncFrom
import sp.it.util.type.type

/** [ValueTextField] for [Font]. */
class FontTextField(initialValue: Font? = null): ValueTextFieldBi<Font>(initialValue, type()) {
   private var picker: FontPicker? = null
   private var valueChanging = Suppressor()

   init {
      styleClass += STYLECLASS
      isEditable = true
      textProperty() attach {
         if (!valueChanging.isSuppressed) {
            valueChanging.isSuppressed = true
            valueConverter.ofS(it).ifOk { value = it }
            valueChanging.isSuppressed = false
         }
      }
   }

   override fun onDialogAction() {
      val pc = picker ?: FontPicker { valueChanging.suppressing { value = it } }.apply {
         picker = this
         editable syncFrom this@FontTextField.editableProperty() on popup.onHiding
         popup.onShown += {
            pickerContent.font = this@FontTextField.value ?: Font.getDefault()
         }
         popup.onHiding += {
            picker = null
            this@FontTextField.requestFocus()
         }
      }

      pc.popup.show(RIGHT_CENTER(this))
   }

   companion object {
      const val STYLECLASS = "font-text-field"
   }

}