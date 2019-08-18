package sp.it.util.collections.map

import sp.it.util.collections.map.abstr.MapByKClass
import java.util.HashMap
import kotlin.reflect.KClass

/** Mutable [KClassMap]. */
class KClassMap<E>(private val map: MutableMap<KClass<*>, E> = HashMap()): MutableMap<KClass<*>, E> by map, MapByKClass<E> {

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

}
