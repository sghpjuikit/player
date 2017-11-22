package sp.it.pl.layout.widget.feature;

import java.util.List;
import sp.it.pl.audio.Item;
import static java.util.Collections.singletonList;
import static sp.it.pl.util.functional.Util.listRO;

/**
 * Capable of reading data to song tags
 */
@Feature(
	name = "Song metadata reader",
	description = "Capable of displaying song metadata",
	type = SongReader.class
)
public interface SongReader {

	/**
	 * Passes item into this reader.
	 *
	 * @param item or null to display no data if supported
	 * @see #read(java.util.List)
	 */
	default void read(Item item) {
		read(item==null ? listRO() : singletonList(item));
	}

	/**
	 * Passes items into this reader.
	 * Displays metadata on items and displays them.
	 *
	 * @param items non null list pf items or empty list to display no data if supported.
	 */
	void read(List<? extends Item> items);

}