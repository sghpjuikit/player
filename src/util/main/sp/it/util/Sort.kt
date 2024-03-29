package sp.it.util

import javafx.scene.control.TableColumn
import javafx.scene.control.TreeTableColumn

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
   @Suppress("UNCHECKED_CAST")
   fun <T> of(c: Comparator<T>): Comparator<T> = when (this) {
      ASCENDING -> c
      DESCENDING -> c.reversed() // preserves reference of the original comparator, see impl. (ReversedComparator)
      NONE -> sp.it.util.functional.Util.SAME as Comparator<T>
   }

   companion object {

      /** @return a corresponding sort for [TableColumn.SortType] */
      @JvmStatic fun of(sort: TableColumn.SortType) = when (sort) {
         TableColumn.SortType.ASCENDING -> ASCENDING
         TableColumn.SortType.DESCENDING -> DESCENDING
      }

      /** @return a corresponding sort for [TreeTableColumn.SortType] */
      @JvmStatic fun of(sort: TreeTableColumn.SortType) = when (sort) {
         TreeTableColumn.SortType.ASCENDING -> ASCENDING
         TreeTableColumn.SortType.DESCENDING -> DESCENDING
      }

      /** @return sort of the given column based on its [TableColumn.SortType] */
      @JvmStatic fun of(column: TableColumn<*, *>) = of(column.sortType)

      /** @return sort of the given column based on its [TreeTableColumn.SortType] */
      @JvmStatic fun of(column: TreeTableColumn<*, *>) = of(column.sortType)

   }

}

/** @see Sort.of */
fun <T> Comparator<T>.inSort(sort: Sort) = sort.of(this)