package sp.it.pl.audio.playlist.sequence;

import java.util.List;

/**
 * Selection logic for selection of next and previous item in a list. Used in
 * combination with {@link ItemSelector}.
 */
public interface Selection<E> {

	/**
	 * Selects an item from list.
	 *
	 * @param size Size of the list.
	 * @param index Index of currently selected item.
	 * @param item Currently selected item.
	 * @param list List the selection operates on.
	 */
	E select(int size, int index, E item, List<E> list);

}