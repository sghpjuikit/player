package sp.it.util.collections.map

import java.util.function.BiConsumer
import java.util.function.Supplier

/**
 * Collection based implementation of [AccumulationMap] - values mapped to the same key are
 * added in a collection (of provided type).
 */
open class KCollectionMap<E, K, C: MutableCollection<E>>(cacheFactory: Supplier<out C>, keyMapper: (E) -> K): AccumulationMap<E, K, C>(keyMapper, cacheFactory, BiConsumer { e, cache -> cache.add(e) }) {

   /** De-accumulates (removes) given element from this map.  */
   fun deAccumulate(e: E) {
      val k = keyMapper(e)
      val c = get(k)
      c?.remove(e)
   }

   /**
    * Multi key get returning the combined content of the cache buckets.
    *
    * @return list containing all elements of all cache buckets / accumulation containers assigned to keys in the given
    * collection.
    */
   fun getElementsOf(vararg keys: K): Sequence<E> = keys.asSequence().flatMap { this[it]?.asSequence().orEmpty() }

   fun getElementsOf(keys: Collection<K>): Sequence<E> = keys.asSequence().flatMap { this[it]?.asSequence().orEmpty() }

}