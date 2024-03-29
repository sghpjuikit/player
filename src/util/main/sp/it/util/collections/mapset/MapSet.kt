package sp.it.util.collections.mapset

import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.collections.MutableMap.MutableEntry

/**
 * [Set] backed by [Map] using provided key mapper for identity check instead of [Object.equals] [Object.hashCode].
 *
 * **This class is *not* a general-purpose `Set` implementation!  While this class implements the `Set` interface, it
 * intentionally violates `Set's` general contract, which mandates the use of the `equals` method when comparing
 * objects. This class is designed for use only in cases wherein reference-equality
 * semantics are required. Developer should be careful when passing it as such, e.g., by returning
 * it from a method with a [Set] only signature.**
 *
 * The underlying map hashing the elements by key is [HashMap] by default,
 * but this is not forced. If desired, pass an arbitrary map into a constructor.
 *
 * Similarly to other sets, this can be used to easily filter duplicates from
 * collection, but leveraging arbitrary element identity.
 * Use like this: `new MapSet<KEY,ELEMENT>({ it.identity() }, elements)`
 */
class MapSet<K: Any, E: Any>(backingMap: MutableMap<K, E>, keyMapper: (E) -> K): MutableSet<E> {

   /**
    * Function transforming element to its key. Used for all collection
    * operations, e.g, add(), addAll(), remove(), etc. Two elements are mapped
    * to the same key if this function produces the same key for them.
    *
    * Note, that if the function returned a constant, this set would become singleton set. Using [Object.hashCode]
    * would result in same mapping strategy as that used in [HashSet] and [HashMap].
    */
   val keyMapper: (E) -> K = keyMapper
   private val m: MutableMap<K, E> = backingMap

   constructor(keyMapper: (E) -> K): this(HashMap<K, E>(), keyMapper)

   constructor(c: Collection<E>, keyMapper: (E) -> K): this(HashMap<K, E>(), keyMapper, c)

   @SafeVarargs
   constructor(keyMapper: (E) -> K, vararg c: E): this(HashMap<K, E>(), keyMapper, *c)

   constructor(backingMap: MutableMap<K, E>, keyMapper: (E) -> K, c: Collection<E>): this(backingMap, keyMapper) { addAll(c) }

   @SafeVarargs
   constructor(backingMap: MutableMap<K, E>, keyMapper: (E) -> K, vararg c: E): this(backingMap, keyMapper) { addAll(*c) }

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

   fun getOrPut(key: K, or: Supplier<E>): E = m.computeIfAbsent(key) { or.get() }

   /** Equivalent to [removeValue]. */
   override fun remove(element: E) = removeValue(element)

   fun removeKey(key: K): Boolean =
      if (containsKey(key)) {
         m.remove(key)
         true
      } else {
         false
      }

   fun removeValue(element: E): Boolean = removeKey(keyMapper(element))

   /** Equivalent to [removeAllValues]. */
   override fun removeAll(elements: Collection<E>): Boolean {
      var modified = false
      for (e in elements)
         modified = modified or removeValue(e)
      return modified
   }

   fun removeAllKeys(keys: Collection<K>): Boolean {
      var modified = false
      for (k in keys)
         modified = modified or removeKey(k)
      return modified
   }

   fun removeAllValues(elements: Collection<E>): Boolean {
      var modified = false
      for (e in elements)
         modified = modified or removeValue(e)
      return modified
   }

   /** Adds item to this keymap if not yet contained. Element identity is obtained using [keyMapper]. */
   override fun add(element: E): Boolean {
      val k = keyMapper(element)
      if (k in m) return false
      m[k] = element
      return true
   }

   /** Adds all not yet contained items to this MapSet. Element identity is obtained using [keyMapper]. */
   override fun addAll(elements: Collection<E>): Boolean {
      var modified = false
      for (e in elements)
         modified = modified or add(e)
      return modified
   }

   /** Adds all not yet contained items to this MapSet. Element identity is obtained using [keyMapper]. */
   fun addAll(elements: Stream<out E>) {
      elements.forEach { add(it) }
   }

   /** Adds all not yet contained items to this MapSet. Element identity is obtained using [keyMapper]. */
   fun addAll(vararg elements: E): Boolean {
      var modified = false
      for (e in elements)
         modified = modified or add(e)
      return modified
   }

   /**
    * [add] with [MutableMap.put] semantics, i.e., addOrReplace.
    *
    * @return true iff the key was not already present in the map
    */
   infix fun put(element: E): Boolean {
      val k = keyMapper(element)
      val e = k !in m
      m[k] = element
      return e
   }

   /** [addAll] with [MutableMap.putAll] semantics, i.e., addOrReplace.  */
   infix fun putAll(elements: Collection<E>) {
      elements.forEach { put(it) }
   }

   /** Equivalent to [retainAllValues]. */
   override fun retainAll(elements: Collection<E>) = retainAllValues(elements)

   fun retainAllKeys(keys: Collection<K>) = m.keys.retainAll(keys)

   fun retainAllValues(elements: Collection<E>) = retainAllKeys(elements.map(keyMapper))

   override fun clear() = m.clear()

   /** @return key-element pairs */
   fun streamE(): Stream<MutableEntry<K, E>> = m.entries.stream()

   /** @return keys */
   fun streamK(): Stream<K> = m.keys.stream()

   /** @return elements */
   fun streamV(): Stream<E> = m.values.stream()

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

   fun computeIfAbsent(key: K, mappingFunction: (K) -> E) = m.computeIfAbsent(key, mappingFunction)

   companion object {

      @JvmStatic fun <E: Any> mapHashSet(): MapSet<Int, E> = MapSet({ it.hashCode() })

      @JvmStatic fun <E: Any> mapHashSet(backingMap: MutableMap<Int, E>): MapSet<Int, E> = MapSet(backingMap, { it.hashCode() })

      @JvmStatic fun <E: Any> mapHashSet(backingMap: MutableMap<Int, E>, c: Collection<E>): MapSet<Int, E> = MapSet(backingMap, { it.hashCode() }, c)

      @JvmStatic fun <E: Any> mapHashSet(backingMap: MutableMap<Int, E>, vararg c: E): MapSet<Int, E> = MapSet(backingMap, { it.hashCode() }, *c)
   }

}