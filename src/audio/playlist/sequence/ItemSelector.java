package audio.playlist.sequence;

import java.util.List;

/**
 * Selector of next or previous item in the list.
 */
public class ItemSelector<E> {
	private final Selection<E> next;
	private final Selection<E> prev;

	public ItemSelector(Selection<E> previous, Selection<E> next) {
		this.prev = previous;
		this.next = next;
	}

	public E same(int size, int index, E current_item, List<E> list) {
		return current_item;
	}

	public E next(int size, int index, E current_item, List<E> list) {
		return next.select(size, index, current_item, list);
	}

	public E previous(int size, int index, E current_item, List<E> list) {
		return prev.select(size, index, current_item, list);
	}
}