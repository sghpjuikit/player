package sp.it.pl.util.type

import java.lang.reflect.Field

fun <T,U> Class<T>.isSuperclassOf(type: Class<U>) = isAssignableFrom(type)

fun <T,U> Class<T>.isSubclassOf(type: Class<U>) = type.isSuperclassOf(this)

inline fun <reified T : Annotation> Field.findAnnotation(): T? = getAnnotation(T::class.java)