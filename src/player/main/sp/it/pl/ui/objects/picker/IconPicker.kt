package sp.it.pl.ui.objects.picker

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Side.RIGHT
import javafx.scene.layout.Priority.ALWAYS
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.ui.nodeinfo.IconPickerContent
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.access.v
import sp.it.util.functional.net
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.vBox

class IconPicker(onOk: (GlyphIcons?) -> Unit) {
   val pickerContent = IconPickerContent()
   val popup = PopWindow()
   val editable = v(true)

   init {
      popup.content.value = vBox(10.0, CENTER) {
         padding = Insets(15.0)
         lay(ALWAYS) += pickerContent
         lay += hBox(15.0, CENTER) {
            lay += Icon(IconMD.CHECK, 22.0).onClickDo { popup.hide(); if (editable.value) onOk(pickerContent.selection.value) }.net {
               editable sync { isDisable = !it }
               it.withText(RIGHT, "Use")
            }
            lay += Icon(IconMD.CHECK_ALL, 22.0).onClickDo { if (editable.value) onOk(pickerContent.selection.value) }.net {
               editable sync { isDisable = !it }
               it.withText(RIGHT, "Apply")
            }
            lay += Icon(IconMA.DO_NOT_DISTURB, 22.0).onClickDo { popup.hide() }.net {
               it.withText(RIGHT, "Cancel")
            }
         }
      }
   }
}