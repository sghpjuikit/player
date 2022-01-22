package sp.it.pl.ui.objects.textfield

import sp.it.pl.audio.tagging.Metadata.Field.MOOD
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.autocomplete.AutoCompletion
import sp.it.pl.ui.objects.autocomplete.AutoCompletion.Companion.autoComplete

/** Text field for audio mood tagging values with a picker and auto-completion. */
class MoodItemNode: ValueTextField<String>() {

   init {
      isEditable = true
      autoComplete(this) { text ->
         APP.db.itemUniqueValuesByField[MOOD].orEmpty().filter { it.contains(text, true) }
      }
   }

   override fun onDialogAction() {
      AutoCompletion.of<String>(this)?.updateSuggestions("")
   }

}