package sp.it.pl.gui.objects.picker

import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.main.AppUtil.APP
import java.util.function.Supplier

/** Mood picker. */
class MoodPicker: Picker<String>() {
    init {
        itemSupply = Supplier { APP.db.itemUniqueValuesByField[Metadata.Field.MOOD].orEmpty().stream() }
    }
}