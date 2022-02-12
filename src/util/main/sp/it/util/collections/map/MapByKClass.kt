package sp.it.util.collections.map

import kotlin.reflect.KClass
import sp.it.util.type.superKClassesInc

interface MapByKClass<E> {

   /** @return list of all values mapped to any of the keys (in that order) */
   fun getElementsOf(keys: Collection<KClass<*>>): Sequence<E>

   /** @return list of all values mapped to any of the keys (in that order) */
   fun getElementsOf(vararg keys: KClass<*>): Sequence<E>

   /**
    * Returns elements mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object::class
    *  * any of specified class' interfaces
    *
    * or empty list if no such mapping exists.
    */
   fun getElementsOfSuper(key: KClass<*>): Sequence<E> {
      val keys = key.superKClassesInc().toList()
      return getElementsOf(keys)
   }

   /**
    * Returns elements mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object::class
    *  * any of specified class' interfaces
    *  * [Void].class or [Nothing].class
    *
    * or empty list if no such mapping exists.
    */
   fun getElementsOfSuperV(key: KClass<*>): Sequence<E> {
      val keys = key.superKClassesInc().toMutableList()
      if (Nothing::class!=key || Void::class!=key) keys += Nothing::class
      return getElementsOf(keys)
   }

   /**
    * Returns first element mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object::class
    *  * any of specified class' interfaces
    * or null if no such mapping exists.
    */
   fun getElementOfSuper(key: KClass<*>): E? = null
      ?: key.superKClassesInc().flatMap { getElementsOf(it).asSequence() }.firstOrNull()

   /**
    * Returns first element mapped to one of (in that order):
    *  * specified class
    *  * any of specified class' superclasses up to Object::class
    *  * any of specified class' interfaces
    *  * [Void].class or [Nothing].class
    * or null if no such mapping exists.
    */
   fun getElementOfSuperV(key: KClass<*>): E? = null
      ?: key.superKClassesInc().flatMap { getElementsOf(it).asSequence() }.firstOrNull()
      ?: getElementsOf(Nothing::class).firstOrNull()

}

