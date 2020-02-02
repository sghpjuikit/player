package sp.it.util.type

import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf

/**
 * Used to get exact generic type argument in runtime.
 *
 * Note that reified inline functions do not help here as any generic argument of the function's generic argument is
 * lost due to `T::class` returning [kotlin.reflect.KClass] instead of [kotlin.reflect.KType].
 *
 * Use: `object: TypeToken<MyType<SupportsNesting>>() {}
 *
 */
abstract class TypeToken<T> {
   val type: Type get() = this::class.java.genericSuperclass.asIf<ParameterizedType?>()!!.actualTypeArguments[0]!!
}

/** @return class representing the generic type argument of this method */
inline fun <reified T: Any> classLiteral() = T::class

/** @return type representing the generic type argument of this method */
inline fun <reified T> typeLiteral() = object: TypeToken<T>() {}.type

/** @return type representing the generic type argument of this method */
@UseExperimental(ExperimentalStdlibApi::class)
inline fun <reified T> type(): VType<T> = VType(typeOf<T>())

data class VType<out T>(val type: KType) {
   constructor(c: Class<T>, isNullable: Boolean): this(c.asIs<Class<Any>>().kotlin.createType(nullable = isNullable))

   val isNullable = type.isMarkedNullable
}


fun <T> VType<T>.toNonNull(): VType<T?> = type.asIs()
val <T> VType<T>.jvmErasure: KClass<*> get() = type.jvmErasure
val <T> VType<T>.rawJ: Class<T> get() = type.jvmErasure.javaObjectType.asIs()
val <T> VType<T>.typeJ: Type get() = type.javaType
val <T: Any> VType<T?>.raw: KClass<T> get() = type.jvmErasure.asIs()
val KType.raw: KClass<*> get() = jvmErasure