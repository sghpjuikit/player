package sp.it.pl.ui.objects.picker

import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.text.Font
import sp.it.pl.main.IconFA
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.v
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.vBox

class FontPicker(onOk: (Font) -> Unit) {
   val pickerContent: FontPickerContent
   val popup = PopWindow()
   val editable = v(true)

   init {
      pickerContent = FontPickerContent()
      popup.content.value = vBox(10.0, CENTER) {
         padding = Insets(15.0)
         lay(ALWAYS) += pickerContent
         lay += hBox(15.0, CENTER) {
            lay += Icon(IconFA.CHECK, 22.0).onClickDo { popup.hide(); if (editable.value) onOk(pickerContent.font) }.withText("Use")
            lay += Icon(IconFA.TIMES, 22.0).onClickDo { popup.hide() }.withText("Cancel")
         }
      }
   }
}