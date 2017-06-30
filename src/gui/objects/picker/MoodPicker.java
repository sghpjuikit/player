package gui.objects.picker;

import audio.tagging.Metadata;
import static main.App.APP;

/** Mood picker. */
public class MoodPicker extends Picker<String> {

	public MoodPicker() {
		super();
		itemSupply = () -> APP.db.getStringPool().getStrings(Metadata.Field.MOOD.name()).stream();
	}

}