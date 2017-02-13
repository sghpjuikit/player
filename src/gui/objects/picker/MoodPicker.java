package gui.objects.picker;

import audio.tagging.Metadata;
import services.database.Db;

/** Mood picker. */
public class MoodPicker extends Picker<String> {

	public MoodPicker() {
		super();
		itemSupply = () -> Db.string_pool.getStrings(Metadata.Field.MOOD.name()).stream();
	}

}