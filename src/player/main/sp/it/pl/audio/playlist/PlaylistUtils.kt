package sp.it.pl.audio.playlist

import kotlin.math.absoluteValue
import sp.it.util.collections.materialize
import sp.it.util.math.min

/**
 * Moves/shifts all specified items by the specified distance.
 * Moved items retain their relative index positions.
 * Moved items do not rotate the list, but stop moving at the start/end of the list.
 *
 * @param items all items. Empty list does nothing.
 * @param indexes of items to move. Empty list does nothing.
 * @param by distance to move by. Negative moves back. Zero does nothing.
 * @return updated indexes of moved songs.
 */
fun <E> moveItemsBy(items: List<E>, indexes: List<Int>, by: Int): Pair<List<E>, List<Int>> {
   if (items.isEmpty() || indexes.isEmpty() || by==0) return items to indexes

   val indexesOld = indexes.materialize()
   val itemsActual = ArrayList(items)
   val byActual = when {
      by < 0 -> -(by.absoluteValue min indexes.min())
      by > 0 -> by min (items.lastIndex - indexes.max())
      else -> by
   }

   if (byActual<0)
      for (index in indexesOld)
         itemsActual.add(index + byActual, itemsActual.removeAt(index))

   if (byActual>0)
      for (index in indexesOld.reversed())
         itemsActual.add(index + byActual, itemsActual.removeAt(index))

   return itemsActual to indexesOld.map { it+byActual }
}