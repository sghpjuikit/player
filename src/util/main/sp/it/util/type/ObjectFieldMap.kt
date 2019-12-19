package sp.it.util.type

import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.collections.map.ClassMap
import sp.it.util.functional.asIs
import java.util.HashSet

private typealias Field<T> = ObjectField<T, *>

/** Map storing sets of attributes for classes, formally: [Set] of [ObjectField]. */
class ObjectFieldMap {

   private val fields = ClassMap<HashSet<Field<*>>>()
   private val cache = ClassMap<HashSet<Field<*>>>()
   private val cache2 = ClassMap<HashSet<Field<*>>>()

   fun <T> add(c: Class<T>, fields: Collection<Field<T>>): Unit = fields.forEach { add(c, it) }

   fun <T> add(c: Class<T>, vararg fields: Field<T>): Unit = fields.forEach { add(c, it) }

   fun <T> add(c: Class<T>, field: Field<T>) {
      fields.computeIfAbsent(c) { HashSet() } += field
      cache.keys.removeIf { c.isAssignableFrom(it) }
      cache2.keys.removeIf { c.isAssignableFrom(it) }
   }

   operator fun <T> get(c: Class<T>): Set<Field<T>> = cache.computeIfAbsent(c) { fields.getElementsOfSuperV(c).flatten().toHashSet() }.asIs()

   fun <T> getExact(c: Class<T>): Set<Field<T>> = cache2.computeIfAbsent(c) { fields.getElementsOf(c).flatten().toHashSet() }.asIs()

   companion object {
      val DEFAULT = ObjectFieldMap()
   }

}