package sp.it.pl.gui.itemnode.textfield

import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.gui.objects.autocomplete.AutoCompletion.Companion.autoComplete
import sp.it.pl.gui.objects.picker.MoodPicker
import sp.it.pl.gui.objects.window.NodeShow
import sp.it.pl.gui.objects.window.popup.PopWindow
import sp.it.pl.main.APP
import sp.it.util.access.v

/** Text field for audio mood tagging values with a picker and auto-completion. */
class MoodItemNode: ValueTextField<String>({ APP.converter.general.toS(it) }) {

   /** The position for the picker to show on. */
   val pickerPosition = v(NodeShow.RIGHT_CENTER)

   init {
      styleClass += STYLECLASS
      isEditable = true
      autoComplete(this) { text ->
         APP.db.itemUniqueValuesByField[Metadata.Field.MOOD].orEmpty().filter { it.contains(text, true) }
      }
   }

   override fun onDialogAction() {
      PopWindow().apply {
         isAutohide.value = true
         content.value = MoodPicker().apply {
            root.setPrefSize(800.0, 600.0)
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