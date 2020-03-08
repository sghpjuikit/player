package sp.it.pl.ui.itemnode.textfield

import javafx.scene.text.Font
import sp.it.pl.ui.objects.picker.FontSelectorDialog
import sp.it.pl.ui.objects.window.NodeShow.RIGHT_CENTER

/** Text field for [Font] with a picker. */
class FontTextField: ValueTextField<Font>() {

   init {
      styleClass += STYLECLASS
   }

   override fun onDialogAction() {
      FontSelectorDialog(value) { value = it }.popup.show(RIGHT_CENTER(this))
   }

   companion object {
      const val STYLECLASS = "font-text-field"
   }

}