package sp.it.pl.layout.widget.feature;

import java.util.List;
import sp.it.pl.audio.Song;
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
	 * Passes song into this reader.
	 *
	 * @param song or null to display no data if supported
	 * @see #read(java.util.List)
	 */
	default void read(Song song) {
		read(song==null ? listRO() : singletonList(song));
	}

	/**
	 * Passes items into this reader.
	 * Displays metadata on items and displays them.
	 *
	 * @param items non null list pf items or empty list to display no data if supported.
	 */
	void read(List<? extends Song> items);

}