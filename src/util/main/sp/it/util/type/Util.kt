package sp.it.util.type

import sp.it.util.dev.Experimental
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicReference
import java.util.logging.Level
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaField

fun Class<*>.isSuperclassOf(type: Class<*>) = isAssignableFrom(type)

fun Class<*>.isSubclassOf(type: Class<*>) = type.isSuperclassOf(this)

fun KClass<*>.isSuperclassOf(type: Class<*>) = isSuperclassOf(type.kotlin)

fun KClass<*>.isSubclassOf(type: Class<*>) = isSubclassOf(type.kotlin)

inline fun <reified T> KClass<*>.isSuperclassOf() = isSuperclassOf(T::class)

inline fun <reified T> KClass<*>.isSubclassOf() = isSubclassOf(T::class)

inline fun <reified T> Class<*>.isSuperclassOf() = isSuperclassOf(T::class.java)

inline fun <reified T> Class<*>.isSubclassOf() = isSubclassOf(T::class.java)

/** @return the most specific common supertype of the specified types */
infix fun KClass<*>.union(type: KClass<*>): KClass<*> = when {
    this==Any::class || type==Any::class -> Any::class
    this==type -> this
    this.isSuperclassOf(type) -> this
    type.isSubclassOf(type) -> type
    else -> Any::class
}

inline fun <reified T: Annotation> Class<*>.findAnnotation(): T? = getAnnotation(T::class.java)

inline fun <reified T: Annotation> Field.findAnnotation(): T? = getAnnotation(T::class.java)

/** Set [java.util.logging.Logger] of specified package to specified level. Helpful for stubborn libraries. */
fun setLoggingLevelForPackage(logPackage: Package, logLevel: Level) {
    java.util.logging.Logger.getLogger(logPackage.name).apply {
        level = logLevel
        useParentHandlers = false
    }
}

/** @return thread-safe [ReadWriteProperty] backed by [AtomicReference] */
fun <T> atomic(initialValue: T) = object: ReadWriteProperty<Any?, T> {

    private val ref = AtomicReference<T>(initialValue)

    override fun getValue(thisRef: Any?, property: KProperty<*>) = ref.get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = ref.set(value)

}

/** @return class representing the generic type argument of this method */
inline fun <reified T: Any> classLiteral() = T::class

/** @return type representing the generic type argument of this method */
inline fun <reified T> type() = object: TypeToken<T>() {}.type

/** Set specified property of this object to null. Use for disposal of read-only properties and avoiding memory leaks. */
@Experimental("Uses reflection and its usefulness must be thoroughly evaluated")
infix fun Any.nullify(property: KProperty<*>) {
    property.javaField?.isAccessible = true
    property.javaField?.set(this, null)
}