package sp.it.util.collections.map.abstr

import sp.it.util.type.Util.getSuperClassesInc
import sp.it.util.type.getSuperKClassesInc
import kotlin.reflect.KClass

interface MapByKClass<E> {

   /** @return list of all values mapped to any of the keys (in that order) */
   fun getElementsOf(keys: Collection<KClass<*>>): Sequence<E>

   /** @return list of all values mapped to any of the keys (in that order) */
   fun getElementsOf(vararg keys: KClass<*>): Sequence<E>

   /**
    * Returns elements mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object.class
    *  * any of specified class' interfaces
    *
    * or empty list if no such mapping exists.
    */
   fun getElementsOfSuper(key: KClass<*>): Sequence<E> {
      val keys = getSuperClassesInc(key.javaObjectType).map { it.kotlin }
      return getElementsOf(keys)
   }

   /**
    * Returns elements mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object.class
    *  * any of specified class' interfaces
    *  * Void.class or Nothing.class
    *
    * or empty list if no such mapping exists.
    *
    *
    * Note: Void.class is useful for mapping objects based on their generic
    * type.
    */
   fun getElementsOfSuperV(key: KClass<*>): Sequence<E> {
      val keys = getSuperClassesInc(key.javaObjectType).map { it.kotlin }.toMutableList()
      if (Nothing::class!=key) keys.add(Nothing::class)
      if (Void::class!=key) keys.add(Void::class)
      return getElementsOf(keys)
   }

   /**
    * Returns first element mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object.class
    *  * any of specified class' interfaces
    *
    * or null if no such mapping exists.
    */
   fun getElementOfSuper(key: KClass<*>): E? = null
      ?: key.getSuperKClassesInc().flatMap { getElementsOf(it).asSequence() }.firstOrNull()

   /**
    * Returns first element mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object.class
    *  * any of specified class' interfaces
    *  * Void.class or Nothing.class
    *
    * or null if no such mapping exists.
    */
   fun getElementOfSuperV(key: KClass<*>): E? = null
      ?: key.getSuperKClassesInc().flatMap { getElementsOf(it).asSequence() }.firstOrNull()
      ?: getElementsOf(Nothing::class).firstOrNull()
      ?: getElementsOf(Void::class).firstOrNull()

}

