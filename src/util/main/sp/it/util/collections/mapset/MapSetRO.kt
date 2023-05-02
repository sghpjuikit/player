package sp.it.util.collections.mapset

import java.util.function.Supplier

/** Thread-safe, immutable [MapSet] */
class MapSetRO<K: Any, E: Any>(backingMap: Map<K, E>, keyMapper: (E) -> K): Set<E> {

   /**
    * Function transforming element to its key. Used for all collection
    * operations, e.g, add(), addAll(), remove(), etc. Two elements are mapped
    * to the same key if this function produces the same key for them.
    *
    * Note, that if the function returned a constant, this set would become singleton set. Using [Object.hashCode]
    * would result in same mapping strategy as that used in [HashSet] and [HashMap].
    */
   val keyMapper: (E) -> K = keyMapper
   private val m: Map<K, E> = backingMap

   constructor(c: Collection<E>, keyMapper: (E) -> K): this(mapOf(*c.map { keyMapper(it) to it }.toTypedArray()), keyMapper)

   fun backingMap(): Map<K, E> = m

   override val size get() = m.size

   override fun isEmpty() = m.isEmpty()

   /** Equivalent to [containsValue]. */
   override fun contains(element: E) = containsValue(element)

   fun containsKey(key: K): Boolean = key in m

   fun containsValue(element: E): Boolean = keyMapper(element) in m

   /** Equivalent to [containsAllValues]. */
   override fun containsAll(elements: Collection<E>) = containsAllValues(elements)

   fun containsAllKeys(keys: Collection<K>) = keys.all { containsKey(it) }

   fun containsAllValues(elements: Collection<E>) = elements.all { containsValue(it) }

   override fun iterator() = m.values.iterator()

   operator fun get(key: K): E? = m[key]

   operator fun get(key: K, key2: K): E? = if (key in m) m[key] else get(key2)

   fun getOr(key: K, or: E): E = m.getOrDefault(key, or)

   fun getOrSupply(key: K, or: Supplier<E>): E = if (key in m) m[key]!! else or.get()

   /** @return [Map.entries] */
   val entries: Set<Map.Entry<K, E>> get() = m.entries

   /** @return [Map.keys] */
   val keys: Set<K> get() = m.keys

   /** @return [Map.values] */
   val values: Collection<E> get() = m.values

   fun ifHasK(k: K, action: (E) -> Unit) {
      if (containsKey(k))
         action(m[k]!!)
   }

   fun ifHasE(element: E, action: (E) -> Unit) = ifHasK(keyMapper(element), action)

}