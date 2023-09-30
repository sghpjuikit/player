package sp.it.util.collections.map

import java.util.function.Function
import kotlin.reflect.KClass

/** Mutable [KClassMap]. */
class KClassMap<E>(private val map: MutableMap<KClass<*>, E> = HashMap()): MutableMap<KClass<*>, E> by map, MapByKClass<E> {

   /** Inline reified [MutableMap.put] */
   inline fun <reified KEY> put(value: E) = put(KEY::class, value)

   @Suppress("UNCHECKED_CAST")
   override fun getElementsOf(keys: Collection<KClass<*>>) = sequence {
      for (c in keys)
         if (containsKey(c))
            yield(get(c) as E)
   }

   @Suppress("UNCHECKED_CAST")
   override fun getElementsOf(vararg keys: KClass<*>) = sequence {
      for (c in keys)
         if (containsKey(c))
            yield(get(c) as E)
   }

   // TODO: remove fix for Kotlin compiler 2.0
   override fun computeIfAbsent(key: KClass<*>, mappingFunction: Function<in KClass<*>, out E>): E {
      return map.computeIfAbsent(key, mappingFunction)
   }
}