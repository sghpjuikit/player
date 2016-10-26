package util;

import java.util.Comparator;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TreeTableColumn;

/**
 * Sort type.
 *
 * @author Martin Polakovic
 */
public enum Sort {
	/** From minimal to maximal element. */
	ASCENDING,
	/** From maximal to minimal element. */
	DESCENDING,
	/** No sort. Order remains as is. */
	NONE;

	/**
	 * Modifies a comparator to uphold the sorting type. If this is descending, new reverse
	 * comparator is returned, otherwise no new comparator is built. By definition every comparator
	 * is ascending, hence the comparator itself is returned when this is ascending. When this is
	 * none, no order comparator (static instance) is returned.
	 * <p/>
	 * Applying this method multiple times will not produce any new comparator instances. {@link #ASCENDING}
	 * and {@link #NONE} will consistently return the same instance and {@link #DESCENDING} will flip between the reverse
	 * and original instance of the comparator, i.e., reverse of a reverse will be the same comparator
	 * object/instance. Formally:
	 * <p/>
	 * c==DESCENDING.cmp(DESCENDING.cmp(c)) is always true.
	 * <p/>
	 * Some code uses null comparator as a no comparator, hence this method accepts null. In such
	 * case, null is always returned (no order has no reverse order).
	 *
	 * @return null if c null, c if ascending, reverse to c if descending or no order
	 * comparator (which always returns 0) when none.
	 */
	@SuppressWarnings("unchecked")
	public <T> Comparator<T> cmp(Comparator<T> c) {
		if (c==null) return null;
		switch (this) {
			case ASCENDING: return c;
			// note the java implementation of ReversedComparator causes multiple calls to reversed()
			// order to preserve the reference of the original comparator, i.e., calling reversed()
			// twice will return the original object. This can be very important.
			case DESCENDING: return c.reversed();
			case NONE: return (Comparator) util.functional.Util.SAME;
			default: throw new SwitchException(this);
		}
	}

	/** @return a corresponding sort for {@link TableColumn.SortType}. */
	public static Sort of(TableColumn.SortType sort) {
		switch (sort) {
			case ASCENDING  : return Sort.ASCENDING;
			case DESCENDING : return Sort.DESCENDING;
			default : throw new SwitchException(sort);
		}
	}

	/** @return a corresponding sort for {@link TreeTableColumn.SortType}. */
	public static Sort of(TreeTableColumn.SortType sort) {
		switch (sort) {
			case ASCENDING  : return Sort.ASCENDING;
			case DESCENDING : return Sort.DESCENDING;
			default : throw new SwitchException(sort);
		}
	}

	/** @return sort of the given column based on its {@link TableColumn.SortType}. */
	public static Sort of(TableColumn<?,?> column) {
		return of(column.getSortType());
	}

	/** @return sort of the given column based on its {@link TreeTableColumn.SortType}. */
	public static Sort of(TreeTableColumn<?,?> column) {
		return of(column.getSortType());
	}
}