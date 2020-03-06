package sp.it.util.type

import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

/** @return class representing the generic type argument of this method (obtained using T:class) */
inline fun <reified T: Any> kClass() = T::class

/** @return java class representing the generic type argument of this method (obtained using T:class.[KClass.java]) */
inline fun <reified T: Any> jClass() = kClass<T>().java

/** @return type representing the generic type argument of this method (obtained using [typeOf]) */
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T> kType() = typeOf<T>()

/** @return type representing the generic type argument of this method as [VType] */
inline fun <reified T> type(): VType<T> = VType(kType<T>())

/** @return java type representing the generic type argument of this method (obtained using [kType].[KType.javaType]) */
inline fun <reified T> jType() = object: TypeToken<T>() {}.type

/** @return java type representing the generic type argument of this method as [TypeToken] */
inline fun <reified T> jTypeToken() = object: TypeToken<T>() {}.type

/**
 * Super type token for Neal Gafter's "super type token" pattern.
 * Used to get exact generic type argument in runtime.
 *
 * [typeOf] effectively renders this into a legacy code.
 * Use: `object: TypeToken<MyType<SupportsNesting>>() {}
 *
 */
abstract class TypeToken<T> {
   val type: Type get() = this::class.java.genericSuperclass.asIf<ParameterizedType?>()!!.actualTypeArguments[0]!!
}

/**
 * Generic [KType].
 * Useful as a type safe reified type carrier.
 */
data class VType<out T>(/** Kotlin type representing this type */ val type: KType) {
   constructor(c: Class<T>, isNullable: Boolean): this(c.asIs<Class<Any>>().kotlin.createType(nullable = isNullable))

   val isNullable = type.isMarkedNullable

   override fun toString() = type.toString()
}

/** @return nullable version of this type */
fun <T> VType<T>.nullable(): VType<T?> = VType(type.withNullability(true))

/** @return notnull version of this type */
fun <T: Any> VType<T?>.notnull(): VType<T> = VType(type.withNullability(false))

/** @return raw class representing this type also called erased type ([KType.jvmErasure]) */
val <T> VType<T>.jvmErasure: KClass<*> get() = type.jvmErasure

/** @return raw java class representing this type also called erased type ([KType.jvmErasure]) */
val <T> VType<T>.rawJ: Class<T> get() = type.jvmErasure.javaObjectType.asIs()

/** @return java type representing this type (some information, like nullability, will be lost) */
val <T> VType<T>.typeJ: Type get() = type.javaType

/** @return raw class representing this type also called erased type ([KType.jvmErasure]) */
val <T: Any> VType<T?>.raw: KClass<T> get() = type.jvmErasure.asIs()

/** @return raw class representing this type also called erased type ([KType.jvmErasure]) */
val KType.raw: KClass<*> get() = jvmErasure

/** @return reified version of [KType.isSupertypeOf] */
inline fun <reified T> KType.isSupertypeOf() = isSupertypeOf(kType<T>())

/** @return reified version of [KType.isSubtypeOf] */
inline fun <reified T> KType.isSubtypeOf(): Boolean = isSubtypeOf(kType<T>())

/** @return whether erased type of this type [KClass.isSubclassOf] the specified class */
fun VType<*>.isSuperclassOf(type: KClass<*>) = raw.isSuperclassOf(type)

/** @return reified version of [VType.isSuperclassOf] */
inline fun <reified T> VType<*>.isSuperclassOf() = isSuperclassOf(T::class)

/** @return whether erased type of this type [KClass.isSuperclassOf] the specified class */
fun VType<*>.isSubclassOf(type: KClass<*>) = raw.isSubclassOf(type)

/** @return reified version of [VType.isSubclassOf] */
inline fun <reified T> VType<*>.isSubclassOf() = isSubclassOf(T::class)