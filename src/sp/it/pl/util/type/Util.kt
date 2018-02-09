package sp.it.pl.util.type

import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

fun <T,U> Class<T>.isSuperclassOf(type: Class<U>) = isAssignableFrom(type)

fun <T,U> Class<T>.isSubclassOf(type: Class<U>) = type.isSuperclassOf(this)

infix fun KClass<*>.union(type: KClass<*>): KClass<*> = when {
    this==Any::class || type==Any::class -> Any::class
    this==type -> this
    this.isSuperclassOf(type) -> this
    type.isSubclassOf(type) -> type
    else -> Any::class
}

inline fun <reified T : Annotation> Field.findAnnotation(): T? = getAnnotation(T::class.java)