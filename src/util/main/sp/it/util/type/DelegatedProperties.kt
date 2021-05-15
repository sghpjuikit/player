package sp.it.util.type

import java.util.concurrent.atomic.AtomicReference
import javafx.scene.Node
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KCallable
import kotlin.reflect.KProperty
import sp.it.util.functional.asIs
import sp.it.util.functional.toUnit

/**
 * Read only constant property that returns the specified constant value.
 * Use [ConstantReadOnlyPropertyDelegateProvider] when computing of the value requires the property
 */
class ConstantReadOnlyProperty<T, V>(val value: V): ReadOnlyProperty<T, V> {
   override fun getValue(thisRef: T, property: KProperty<*>): V = value
}

/**
 * Read only constant property that returns constant value created at the time of delegation by the specified supplier
 * Use [ConstantReadOnlyProperty] when computing of the value does not require the property
 */
class ConstantReadOnlyPropertyDelegateProvider<T, V>(val value: (ref: T, property: KProperty<*>) -> V): PropertyDelegateProvider<T, ReadOnlyProperty<T, V>> {
   override fun provideDelegate(thisRef: T, property: KProperty<*>) = ConstantReadOnlyProperty<T, V>(value(thisRef, property))
}

/** @return thread-safe [ReadWriteProperty] backed by [AtomicReference] */
fun <T> atomic(initialValue: T) = object: ReadWriteProperty<Any?, T> {
   private val ref = AtomicReference(initialValue)
   override fun getValue(thisRef: Any?, property: KProperty<*>) = ref.get()
   override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = ref.set(value)
}

/** @return volatile [ReadWriteProperty] backed by [Volatile] annotated property */
fun <T> volatile(initialValue: T) = object: ReadWriteProperty<Any?, T> {
   @Volatile private var ref = initialValue
   override fun getValue(thisRef: Any?, property: KProperty<*>) = ref
   override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
      ref = value
   }
}

/**
 * @return delegated property that delegates to this callable and then maps the result with the specified mapper.
 * This callable and the mapper are called on every read of the returned property.
 */
fun <SOURCE, T> KCallable<SOURCE>.map(mapper: (SOURCE) -> T) = ReadOnlyProperty<Any?, T> { _, _ -> call().let(mapper) }

/**
 * @param key key into the property map. If null or unspecified, name of the property will be used as the key.
 * @param or supplier for value when read of the returned property does not see any value associated with the key in the map
 * @return [ReadWriteProperty] that delegates to [Node.properties] of this node by the specified key.
 * Every read casts and therefore asserts the value's proper runtime type (hence the reified parameter).
 * Every write writes over any previously set value.
 */
inline fun <reified T: Any> Node.property(key: String? = null, crossinline or: () -> T) = object: ReadWriteProperty<Any?, T> {
   override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = properties.put(key ?: property.name, value).toUnit()
   override fun getValue(thisRef: Any?, property: KProperty<*>) = properties[key ?: property.name]?.asIs<T>() ?: or()
}

/**
 * @param key key into the property map. If null or unspecified, name of the property will be used as the key.
 * @param or supplier for value when read of the returned property does not see any value associated with the key in the map
 * @return [ReadWriteProperty] that delegates to [Node.properties] of the node by the specified key.
 * Every read casts and therefore asserts the value's proper runtime type (hence the reified parameter).
 * Every write writes over any previously set value.
 */
inline fun <reified T: Any> property(key: String? = null, crossinline or: Node.() -> T) = object: ReadWriteProperty<Node, T> {
   override fun setValue(thisRef: Node, property: KProperty<*>, value: T) = thisRef.properties.put(key ?: property.name, value).toUnit()
   override fun getValue(thisRef: Node, property: KProperty<*>) = thisRef.properties[key ?: property.name]?.asIs<T>() ?: thisRef.or()
}

inline fun <reified T: Any> propertyNullable(key: String? = null) = object: ReadWriteProperty<Node, T?> {
   override fun setValue(thisRef: Node, property: KProperty<*>, value: T?) = thisRef.properties.put(key ?: property.name, value).toUnit()
   override fun getValue(thisRef: Node, property: KProperty<*>) = thisRef.properties[key ?: property.name]?.asIs<T>()
}