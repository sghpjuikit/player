package util

import javafx.scene.control.TableColumn
import javafx.scene.control.TreeTableColumn
import java.util.*


/** @see Sort.of */
fun <T> Comparator<T>?.inSort(sort: Sort) = sort.of(this)

/** Sort type. */
enum class Sort {
    /** From minimal to maximal element. */
    ASCENDING,
    /** From maximal to minimal element. */
    DESCENDING,
    /** No sort. Order remains as is. */
    NONE;

    /**
     * Modifies the comparator to uphold this sorting type. If this is descending, new reverse comparator is returned,
     * otherwise no new comparator is built. By definition every comparator is ascending, hence the comparator itself is
     * returned when this is ascending. When this is none, no order comparator (static instance) is returned.
     *
     * Applying this method multiple times will not produce any new comparator instances. [.ASCENDING] and [.NONE]
     * consistently return the same instance and [.DESCENDING] will flip between the reverse and
     * original instance of the comparator, i.e., reverse of a reverse will be the same comparator object/instance.
     * Formally:
     *
     * c==c.cmp(DESCENDING).cmp(DESCENDING) is always true.
     *
     * Some code uses null comparator as a no comparator, hence this method accepts null. In such case, null is always
     * returned (no order has no reverse order).
     *
     * @return null if c null, c if ascending, reverse to c if descending or no order comparator (which always returns
     * 0) when none.
     */
    fun <T> of(c: Comparator<T>?): Comparator<T>? {
        if (c==null) return null
        return when (this) {
            ASCENDING -> c
            // We need to preserve reference
            // Rely on java impl.: ReversedComparator preserves the reference of the original comparator
            DESCENDING -> c.reversed()
            NONE -> util.functional.Util.SAME as Comparator<T>
        }
    }

    companion object {

        /** @return a corresponding sort for [TableColumn.SortType] */
        @JvmStatic fun of(sort: TableColumn.SortType) = when (sort) {
            TableColumn.SortType.ASCENDING -> Sort.ASCENDING
            TableColumn.SortType.DESCENDING -> Sort.DESCENDING
            else -> throw SwitchException(sort)
        }

        /** @return a corresponding sort for [TreeTableColumn.SortType] */
        @JvmStatic fun of(sort: TreeTableColumn.SortType) = when (sort) {
            TreeTableColumn.SortType.ASCENDING -> Sort.ASCENDING
            TreeTableColumn.SortType.DESCENDING -> Sort.DESCENDING
            else -> throw SwitchException(sort)
        }

        /** @return sort of the given column based on its [TableColumn.SortType] */
        @JvmStatic fun of(column: TableColumn<*,*>) = of(column.sortType)

        /** @return sort of the given column based on its [TreeTableColumn.SortType] */
        @JvmStatic fun of(column: TreeTableColumn<*, *>) = of(column.sortType)

    }
}