package sp.it.pl.gui.itemnode.textfield

import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.MOOD
import sp.it.pl.gui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.gui.objects.picker.MoodPicker
import sp.it.pl.gui.objects.window.NodeShow.RIGHT_CENTER
import sp.it.pl.gui.objects.window.popup.PopWindow
import sp.it.pl.main.APP
import sp.it.pl.main.emScaled
import sp.it.util.access.v
import sp.it.util.ui.prefSize
import sp.it.util.ui.x

/** Text field for audio mood tagging values with a picker and auto-completion. */
class MoodItemNode: ValueTextField<String>({ APP.converter.general.toS(it) }) {

   /** The position for the picker to show on. */
   val pickerPosition = v(RIGHT_CENTER)

   init {
      styleClass += STYLECLASS
      isEditable = true
      autoComplete(this) { text ->
         APP.db.itemUniqueValuesByField[MOOD].orEmpty().filter { it.contains(text, true) }
      }
   }

   override fun onDialogAction() {
      PopWindow().apply {
         isAutohide.value = true
         content.value = MoodPicker().apply {
            root.prefSize = 500.emScaled x 300.emScaled
            onCancel = { hide() }
            onSelect = {
               value = it
               hide()
            }
            buildContent()
         }.root

         show(pickerPosition.value.invoke(this@MoodItemNode))
      }
   }

   companion object {
      const val STYLECLASS = "mood-text-field"
   }
}