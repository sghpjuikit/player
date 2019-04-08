package sp.it.pl.util.type

import sp.it.pl.util.functional.asIf
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

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
    val type: Type get() = this::class.java.genericSuperclass?.asIf<ParameterizedType>()!!.actualTypeArguments[0]!!
}