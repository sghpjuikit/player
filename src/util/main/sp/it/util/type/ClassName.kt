package sp.it.util.type

import sp.it.util.collections.map.KClassMap
import sp.it.util.functional.asIs
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmName

/**
 * Hierarchical class - className map.
 */
class ClassName {

   private val namesNoSuper = KClassMap<String>()
   private val names = KClassMap<String>()
   private val cache = KClassMap<String>()

   /**
    * Initialization block.
    * All write operations can only be done within this block.
    *
    * This class is not thread-safe and should be populated before it is used.
    */
   operator fun invoke(block: ClassName.() -> Unit) = this.block()

   /** Registers name for specified class and all subclasses that do not have any name registered. */
   infix fun String.alias(type: KClass<*>) {
      names[type] = this
      cache.keys.removeIf { type.isSuperclassOf(it) }
   }

   /**
    * Registers name for exactly the specified class.
    *
    * Desirable mostly for  general classes, such as [java.lang.Object], [java.lang.Void] or [Nothing].
    */
   infix fun String.aliasExact(type: KClass<*>) {
      namesNoSuper[type] = this
      cache.keys.removeIf { type.isSuperclassOf(it) }
   }

   /**
    * Returns name of the class. Name computation is in order:
    *  *  registered name of class
    *  *  registered name of first superclass in inheritance order
    *  *  registered name of first interface (no order)
    *  *  human readable name derived from class
    *
    * Lazy and cached. O(1).
    */
   operator fun get(type: KClass<*>) = cache.computeIfAbsent(type) {
      null
         ?: namesNoSuper[it]
         ?: names.getElementOfSuperV(it)
         ?: of(it)
   }

   /** Returns [get] for the class of the specified instance or for [Nothing] class if instance is null. */
   fun getOf(instance: Any?): String = when (instance) {
      null -> get(Nothing::class)
      else -> get(instance::class)
   }

   companion object {

      private fun of(type: KClass<*>): String = when {
         type.java.isAnonymousClass -> of(type.java.superclass.asIs<Class<*>>().kotlin)  // anonymous classes have no human readable name, use superclass
         else -> type.simpleName ?: type.jvmName
      }

   }

}