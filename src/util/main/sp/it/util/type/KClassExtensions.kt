package sp.it.util.type

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import sp.it.util.dev.fail
import sp.it.util.dev.failIf
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry

/** True iff this class is an enum. [Class.isEnum] does not work for enums with class method bodies. See [Class.enumValues]. */
val Class<*>.isEnumClass: Boolean
   @Suppress("DEPRECATION")
   get() = isEnum || Enum::class.java.isAssignableFrom(this)

/** True iff this class is an enum. See [KClass.enumValues]. */
val KClass<*>.isEnum: Boolean
   get() = java.isEnumClass

/**
 * Enum constants in declared order. [Class.getEnumConstants] does not work for enums with class method bodies. See [Class.isEnumClass].
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
 * Enum constants in declared order. [Class.getEnumConstants] does not work for enums with class method bodies. See [KClass.isEnum].
 * @throws java.lang.AssertionError if class not an enum
 */
val <T: Any> KClass<T>.enumValues: Array<T>
   get() = java.enumValues

/** True iff this class is a singleton, i.e., [KClass.objectInstance] is not null. */
val KClass<*>.isObject: Boolean
   get() = runTry { objectInstance!=null }.orNull() == true // TODO: runTry is workaround for https://youtrack.jetbrains.com/issue/KT-41373 && https://youtrack.jetbrains.com/issue/KT-22792

/** Singletons (objects) subclassing the specified class as sealed class. */
val <T: Any> KClass<T>.sealedSubObjects: List<T>
   get() = sealedSubclasses.mapNotNull { it.objectInstance }

/** @return this class or if anonymous, the superclass or superinterface or [Any] */
fun KClass<*>.resolveAnonymous(): KClass<*> {
   val cj = java
   return if (cj.isAnonymousClass) cj.superclass?.kotlin ?: cj.interfaces.firstOrNull()?.kotlin ?: Any::class
   else this
}

/** @return data class component properties in declaration order (unlike [KClass.declaredMemberProperties]). Fails for non data class. */
fun <T: Any> KClass<T>.dataComponentProperties(): List<KProperty1<T, *>> {
   failIf(!isData) { "Class $this must be data class to return its components" }
   val ps = primaryConstructor!!.parameters.withIndex().associate { (i, p) -> p.name to i }
   return declaredMemberProperties.filter { it.name in ps }.sortedBy { ps[it.name] }
}