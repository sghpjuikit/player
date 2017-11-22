package sp.it.pl.gui.objects.picker;

import sp.it.pl.audio.tagging.Metadata;
import static sp.it.pl.main.App.APP;

/** Mood picker. */
public class MoodPicker extends Picker<String> {

	public MoodPicker() {
		super();
		itemSupply = () -> APP.db.getStringPool().getStrings(Metadata.Field.MOOD.name()).stream();
	}

}