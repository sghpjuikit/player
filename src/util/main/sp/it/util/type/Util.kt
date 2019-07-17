package sp.it.util.type

import sp.it.util.dev.fail
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
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
inline fun <reified T> typeLiteral() = object: TypeToken<T>() {}.type

/** @return class representing this type, i.e., type stripped of its generic type parameters */
fun Type.toRaw(): Class<*> = let { type ->
   when (type) {
      is Class<*> -> type
      is ParameterizedType -> {
         val rawType = type.rawType
         if (rawType is Class<*>) rawType else fail { "Unable to determine raw type of parameterized type=$type, it's rawType=$rawType is not instance of ${Class::class.java}" }
      }
      is GenericArrayType -> {
         val componentType = type.genericComponentType
         Array.newInstance(componentType.toRaw(), 0).javaClass
      }
      is WildcardType -> {
         if (type.lowerBounds.isEmpty()) type.upperBounds[0].toRaw()
         else type.lowerBounds[0].toRaw()
      }
      is TypeVariable<*> -> type.bounds[0].toRaw()
      else -> fail { "Unable to determine raw type of type=$type, unsupported kind" }
   }
}

/**
 * Flattens a type to individual type fragments represented by jvm classes, removing variance (wildcards) and nullability.
 *
 * Examples:
 *
 * `Any` -> [Object.class]
 * `Any?` -> [Object.class]
 * `List<*>` -> [List.class, Object.class]
 * `List<Int?>` -> [List.class, Integer.class]
 * `List<out Int>` -> [List.class, Integer.class]
 * `List<in Int?>` -> [List.class, Integer.class]
 * `MutableList<Int>` -> [List.class, Integer.class]
 * `ArrayList<Int>` -> [ArrayList.class, Integer.class]
 *
 * @return sequence of classes representing the specified type and its generic type arguments
 */
fun Type.flattenToRawTypes(): Sequence<Class<*>> = when {
   this is WildcardType -> (if (lowerBounds.isNullOrEmpty()) upperBounds else lowerBounds).asSequence().flatMap { it.flattenToRawTypes() }
   this is ParameterizedType -> sequenceOf(toRaw()) + actualTypeArguments.asSequence().flatMap { it.flattenToRawTypes() }
   this is Class<*> -> sequenceOf(this)
   else -> throw Exception(toString())
}

/** Set specified property of this object to null. Use for disposal of read-only properties and avoiding memory leaks. */
infix fun Any.nullify(property: KProperty<*>) {
   property.javaField?.isAccessible = true
   property.javaField?.set(this, null)
}