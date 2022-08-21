package sp.it.util.type

import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.collections.map.KClassMap
import sp.it.util.functional.asIs

private typealias Field<T> = ObjectField<T, *>

/** Map storing sets of attributes for classes, formally: [Set] of [ObjectField]. */
class ObjectFieldMap {

   private val fields = KClassMap<LinkedHashSet<Field<*>>>()
   private val cache = KClassMap<LinkedHashSet<Field<*>>>()
   private val cache2 = KClassMap<LinkedHashSet<Field<*>>>()

   fun <T: Any> add(c: KClass<T>, fields: Collection<Field<T>>): Unit = fields.forEach { add(c, it) }

   fun <T: Any> add(c: KClass<T>, vararg fields: Field<T>): Unit = fields.forEach { add(c, it) }

   fun <T: Any> add(c: KClass<T>, field: Field<T>) {
      fields.computeIfAbsent(c) { LinkedHashSet() } += field
      cache.keys.removeIf { c.isSuperclassOf(it) }
      cache2.keys.removeIf { c.isSuperclassOf(it) }
   }

   operator fun <T: Any> get(c: VType<T>): Set<Field<T>> = get(c.raw)

   operator fun <T: Any> get(c: KClass<T>): Set<Field<T>> = cache.computeIfAbsent(c) { fields.getElementsOfSuperV(c).flatten().toCollection(LinkedHashSet()) }.asIs()

   fun <T: Any> getExact(c: KClass<T>): Set<Field<T>> = cache2.computeIfAbsent(c) { fields.getElementsOf(c).flatten().toCollection(LinkedHashSet()) }.asIs()

   companion object {
      val DEFAULT = ObjectFieldMap()
   }

}