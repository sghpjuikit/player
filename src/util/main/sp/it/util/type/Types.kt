package sp.it.util.type

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
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs

private fun nothing(): Nothing = fail { "" }
private fun nothingNullable(): Nothing? = null

/** @return class representing the generic type argument of this method (obtained using T:class) */
inline fun <reified T: Any> kClass() = T::class

/** @return java class representing the generic type argument of this method (obtained using T:class.[KClass.java]) */
inline fun <reified T: Any> jClass() = kClass<T>().java

/** @return type representing the generic type argument of this method (obtained using [typeOf]) */
inline fun <reified T> kType() = typeOf<T>()

/** @return type [Any] as [KType] */
fun kTypeAnyNonNull(): KType = kType<Any>()

/** @return type [Any]? as [KType] */
fun kTypeAnyNullable(): KType = kType<Any?>()

/** @return type [Nothing] as [KType] */
fun kTypeNothingNonNull(): KType = typeNothingNonNull().type

/** @return type [Nothing]? as [KType] */
fun kTypeNothingNullable(): KType = typeNothingNullable().type

/** @return type representing the generic type argument of this method as [VType] */
inline fun <reified T> type(): VType<T> = VType(kType<T>())

/** @return type [Nothing] as [VType] */
fun typeNothingNonNull(): VType<Nothing> = VType(::nothing.returnType)

/** @return type [Nothing]? as [VType] */
fun typeNothingNullable(): VType<Nothing?> = VType(::nothingNullable.returnType)

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
 */
abstract class TypeToken<T> {
   val type: Type get() = this::class.java.genericSuperclass.asIf<ParameterizedType?>()!!.actualTypeArguments[0]!!
}

/**
 * Generic [KType].
 * Useful as a type safe reified type carrier.
 */
data class VType<out T>(/** Kotlin type representing this type */ val type: KType) {
   constructor(c: Class<T & Any>, isNullable: Boolean): this(c.asIs<Class<Any>>().kotlin.createType(nullable = isNullable))

   val isNullable = type.isMarkedNullable

   override fun toString() = type.toString()
}

/** @return nullable version of this type */
fun <T> VType<T>.nullable(): VType<T?> = if (isNullable) asIs() else VType(type.withNullability(true))

/** @return notnull version of this type */
fun <T> VType<T>.notnull(): VType<T & Any> = if (!isNullable) asIs() else VType(type.withNullability(false))

/** @return raw java class representing this type also called erased type ([KType.jvmErasure]) */
val <T> VType<T>.rawJ: Class<T> get() = type.jvmErasure.javaObjectType.asIs()

/** @return java type representing this type (some information, like nullability, will be lost) */
val <T> VType<T>.typeJ: Type get() = type.javaType

/** @return raw class representing this type also called erased type ([KType.jvmErasure]) */
val <T> VType<T>.raw: KClass<T & Any> get() = type.jvmErasure.asIs()

/** @return raw class representing this type also called erased type ([KType.jvmErasure]) */
val KType.raw: KClass<*> get() = jvmErasure

/** @return reified version of [KType.isSupertypeOf] */
inline fun <reified SUBTYPE> KType.isSupertypeOf() = isSupertypeOf(kType<SUBTYPE>())

/** @return reified version of [KType.isSubtypeOf] */
inline fun <reified SUPERTYPE> KType.isSubtypeOf() = isSubtypeOf(kType<SUPERTYPE>())

/** @return whether this type [KType.isSubtypeOf] the specified type */
infix fun VType<*>.isSubtypeOf(supertype: VType<*>) = type.isSubtypeOf(supertype.type)

/** @return whether this type [KType.isSubtypeOf] the specified type */
inline fun <reified SUPERTYPE> VType<*>.isSubtypeOf() = type.isSubtypeOf<SUPERTYPE>()

/** @return whether this type [KType.isSupertypeOf] the specified type */
infix fun VType<*>.isSupertypeOf(subtype: VType<*>) = type.isSupertypeOf(subtype.type)

/** @return whether this type [KType.isSupertypeOf] the specified type */
inline fun <reified SUBTYPE> VType<*>.isSupertypeOf() = type.isSupertypeOf<SUBTYPE>()

/** @return whether erased type of this type [KClass.isSubclassOf] the specified class */
infix fun VType<*>.isSuperclassOf(subclass: KClass<*>) = raw.isSuperclassOf(subclass)

/** @return reified version of [VType.isSuperclassOf] */
inline fun <reified SUBCLASS> VType<*>.isSuperclassOf() = isSuperclassOf(SUBCLASS::class)

/** @return whether erased type of this type [KClass.isSuperclassOf] the specified class */
infix fun VType<*>.isSubclassOf(subclass: KClass<*>) = raw.isSubclassOf(subclass)

/** @return reified version of [VType.isSubclassOf] */
inline fun <reified SUBCLASS> VType<*>.isSubclassOf() = isSubclassOf(SUBCLASS::class)

/** @return allowed enum values defined by [KClass.enumValues] of [raw] with null if this type os nullable */
@Suppress("UNCHECKED_CAST")
fun <T: Enum<T>, TN: T?> VType<TN>.enumValues(): List<TN> =
   if (isNullable) (raw.asIs<KClass<T>>().enumValues.toList() as List<TN>) + (null as TN)
   else raw.asIs<KClass<T>>().enumValues.toList().asIs()