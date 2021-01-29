package sp.it.util.type

import kotlin.reflect.KClass
import sp.it.util.dev.fail

/** True iff this class is a enum. [Class.isEnum] does not work for enums with class method bodies. See [Class.enumValues]. */
val Class<*>.isEnumClass: Boolean
   @Suppress("DEPRECATION")
   get() = isEnum || Enum::class.java.isAssignableFrom(this)

/** True iff this class is a enum. See [KClass.enumValues]. */
val KClass<*>.isEnum: Boolean
   get() = java.isEnumClass

/**
 * Enum constants in declared order. [Class.enumConstants] does not work for enums with class method bodies. See [Class.isEnumClass].
 * @throws java.lang.AssertionError if class not an enum
 */
val <T> Class<T>.enumValues: Array<T>
   @Suppress("UNCHECKED_CAST", "DEPRECATION")
   get() = when {
      isEnum -> enumConstants
      isEnumClass -> enclosingClass?.enumConstants as Array<T>? ?: fail { "Class=$this is not an Enum." }
      else -> fail { "Class=$this is not an Enum." }
   }

/**
 * Enum constants in declared order. [Class.enumConstants] does not work for enums with class method bodies. See [KClass.isEnum].
 * @throws java.lang.AssertionError if class not an enum
 */
val <T: Any> KClass<T>.enumValues: Array<T>
   get() = java.enumValues

/** True iff this class is a singleton, i.e., [KClass.objectInstance] is not null. */
val KClass<*>.isObject: Boolean
   get() = objectInstance!=null

/** Singletons (objects) subclassing the specified class as sealed class. */
val <T: Any> KClass<T>.sealedSubObjects: List<T>
   get() = sealedSubclasses.mapNotNull { it.objectInstance }