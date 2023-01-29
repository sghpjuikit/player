package sp.it.pl.ui.objects.textfield

import de.jensd.fx.glyphs.GlyphIcons
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.picker.IconPicker
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.util.access.editable
import sp.it.util.collections.setTo
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Ok
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncFrom
import sp.it.util.type.type

/** [ValueTextField] for [GlyphIcons]. */
class IconTextField(initialValue: GlyphIcons? = null): ValueTextFieldBi<GlyphIcons>(initialValue, type()) {
   private var picker: IconPicker? = null
   private var valueChanging = Suppressor()
   private val iconIcon = Icon().styleclass("icon-icon")

   init {
      styleClass += STYLECLASS
      isEditable = false
      updateIconIcon(Ok(initialValue))
      textProperty() attach {
         if (!valueChanging.isSuppressed) {
            valueChanging.isSuppressed = true
            valueConverter.ofS(it).ifOk { value = it }.apply { updateIconIcon(this) }
            valueChanging.isSuppressed = false
         } else {
            valueConverter.ofS(it).apply { updateIconIcon(this) }
         }
      }
   }

   private fun updateIconIcon(value: Try<GlyphIcons?, String>) {
      if (value is Ok && value.value!=null) {
         iconIcon.icon(value.value)
         right setTo right.filter { it !== iconIcon }.toMutableList().apply { add(indexOfFirst { it is ArrowDialogButton }, iconIcon) }
      } else {
         right setTo right.filter { it !== iconIcon }
      }
   }

   override fun onDialogAction() {
      val pc = picker ?: IconPicker { valueChanging.suppressing { value = it } }.apply {
         picker = this
         editable syncFrom this@IconTextField.editable on popup.onHiding
         popup.onShown += {
            pickerContent.selection.value = this@IconTextField.value
         }
         popup.onHiding += {
            picker = null
            this@IconTextField.requestFocus()
         }
      }

      pc.popup.show(RIGHT_CENTER(this))
   }

   companion object {
      const val STYLECLASS = "icon-text-field"
   }

}