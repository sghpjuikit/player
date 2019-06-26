package sp.it.pl.gui.objects.picker

import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.main.APP

/** Mood picker. */
class MoodPicker: Picker<String>() {
   init {
      itemSupply = { APP.db.itemUniqueValuesByField[Metadata.Field.MOOD].orEmpty().asSequence() }
   }
}