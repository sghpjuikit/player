package sp.it.util.access

import javafx.beans.Observable
import javafx.beans.binding.Bindings
import javafx.beans.binding.BooleanBinding
import javafx.beans.binding.DoubleBinding
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.Property
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableDoubleValue
import javafx.beans.value.ObservableFloatValue
import javafx.beans.value.ObservableIntegerValue
import javafx.beans.value.ObservableLongValue
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import sp.it.util.collections.materialize
import sp.it.util.dev.Experimental
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.functional.Try
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachTo
import sp.it.util.reactive.onChange
import sp.it.util.reactive.sync
import sp.it.util.type.enumValues
import sp.it.util.type.volatile

val <T> KProperty0<T>.value: T
   get() = get()
var <T> KMutableProperty0<T>.value: T
   get() = get()
   set(value) = set(value)

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>): T = value
operator fun <T> Property<T>.setValue(thisRef: Any, property: KProperty<*>, value: T): Unit = setValue(value)
   infix fun <T> Property<T>.valueTry(v: Try<T, *>): Unit = v.ifOk { value = it }.toUnit()

operator fun ObservableDoubleValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun DoubleProperty.setValue(thisRef: Any, property: KProperty<*>, value: Double) = set(value)

operator fun ObservableFloatValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun FloatProperty.setValue(thisRef: Any, property: KProperty<*>, value: Float) = set(value)

operator fun ObservableLongValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun LongProperty.setValue(thisRef: Any, property: KProperty<*>, value: Long) = set(value)

operator fun ObservableIntegerValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun IntegerProperty.setValue(thisRef: Any, property: KProperty<*>, value: Int) = set(value)

operator fun ObservableBooleanValue.getValue(thisRef: Any, property: KProperty<*>) = get()
operator fun BooleanProperty.setValue(thisRef: Any, property: KProperty<*>, value: Boolean) = set(value)

/** Sets value to current value transformed with the specified function, like `value = f(value)`. */
inline fun <T> WritableValue<T>.transformValue(f: (T) -> T) {
   value = f(value)
}

/** Sets value to negated value of current value. */
fun WritableValue<Boolean>.toggle() = transformValue { !it }

/** Sets value to the enum value following or preceding the current value based on declaration order and the specified flag. Loop back to 1st/last value. */
fun <T: Enum<T>> WritableValue<T>.toggle(next: Boolean) = if (next) toggleNext() else togglePrevious()

/** Sets value to the enum value following or preceding the current value based order on by the specified comparator extractor and the specified flag. Loop back to 1st/last value. */
fun <T: Enum<T>, R: Comparable<R>> WritableValue<T>.toggle(next: Boolean, by: (T) -> R?) =
   if (next) toggleNext(by) else togglePrevious(by)

/** Sets value to the enum value following the current value based on declaration order. Loop back to 1st value. */
fun <T: Enum<T>> WritableValue<T>.toggleNext() = transformValue { Values.next(it) }

/** Sets value to the enum value following the current value based on order by the specified comparator extractor. Loop back to 1st value. */
fun <T: Enum<T>, R: Comparable<R>> WritableValue<T>.toggleNext(by: (T) -> R?) =
   transformValue { Values.next(it::class.enumValues.sortedBy(by), it) }

/** Sets value to the enum value preceding the current value based on declaration order. Loop back to last value. */
fun <T: Enum<T>> WritableValue<T>.togglePrevious() = transformValue { Values.previous(it) }

/** Sets value to the enum value preceding the current value based on order by the specified comparator extractor. Loop back to last value. */
fun <T: Enum<T>, R: Comparable<R>> WritableValue<T>.togglePrevious(by: (T) -> R?) =
   transformValue { Values.previous(it::class.enumValues.sortedBy(by), it) }

/** @return this property as a writable property */
fun <T> ObservableValue<T>.writable(): V<T> {
   val w = V<T>(value)
   attachTo(w)
   return w
}

/** @return this property as a read-only property */
fun <T> ObservableValue<T>.readOnly(): ObservableValue<T> = this

/** @return this property as a read-only property backed by [Volatile] property */
fun <T> ObservableValue<T>.readOnlyThreadSafe() = ObservableValueVolatileWrapper(this)

/** @return this observable list as a read-only backed by [Volatile] property containing the [List.materialize] value */
fun <LIST, E> LIST.readOnlyThreadSafe() where LIST: Observable, LIST: List<E> = ObservableListVolatileWrapper(this)

/** @return this observable list as a read-only backed by [Volatile] property containing the [Set.materialize] value */
fun <SET, E> SET.readOnlyThreadSafe() where SET: Observable, SET: Set<E> = ObservableSetVolatileWrapper(this)

/** @return this observable list as a read-only backed by [Volatile] property containing the [Map.materialize] value */
fun <MAP, K, V> MAP.readOnlyThreadSafe() where MAP: Observable, MAP: Map<K, V> = ObservableMapVolatileWrapper(this)

data class ObservableValueVolatileWrapper<T>(private val source: ObservableValue<T>) {
   private var value by volatile(source.value)
   operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

   init {
      failIfNotFxThread()
      source.attach { value = it }
   }
}

data class ObservableListVolatileWrapper<LIST, E>(private val source: LIST) where LIST: Observable, LIST: List<E> {
   private var value by volatile(source.materialize())
   operator fun getValue(thisRef: Any?, property: KProperty<*>): List<E> = value

   init {
      failIfNotFxThread()
      source.onChange { value = source.materialize() }
   }
}

data class ObservableSetVolatileWrapper<SET, E>(private val source: SET) where SET: Observable, SET: Set<E> {
   private var value by volatile(source.materialize())
   operator fun getValue(thisRef: Any?, property: KProperty<*>): Set<E> = value

   init {
      failIfNotFxThread()
      source.onChange { value = source.materialize() }
   }
}

data class ObservableMapVolatileWrapper<MAP, K, V>(private val source: MAP) where MAP: Observable, MAP: Map<K, V> {
   private var value by volatile(source.materialize())
   operator fun getValue(thisRef: Any?, property: KProperty<*>): Map<K, V> = value

   init {
      failIfNotFxThread()
      source.onChange { value = source.materialize() }
   }
}

operator fun ObservableNumberValue.plus(other: ObservableNumberValue) = Bindings.add(this, other) as DoubleBinding
operator fun ObservableNumberValue.plus(other: Double) = Bindings.add(this, other)!!
operator fun ObservableNumberValue.plus(other: Float) = Bindings.add(this, other) as DoubleBinding
operator fun ObservableNumberValue.plus(other: Long) = Bindings.add(this, other) as DoubleBinding
operator fun ObservableNumberValue.plus(other: Int) = Bindings.add(this, other) as DoubleBinding
operator fun ObservableNumberValue.minus(other: ObservableNumberValue) = Bindings.subtract(this, other) as DoubleBinding
operator fun ObservableNumberValue.minus(other: Double) = Bindings.subtract(this, other)!!
operator fun ObservableNumberValue.minus(other: Float) = Bindings.subtract(this, other) as DoubleBinding
operator fun ObservableNumberValue.minus(other: Long) = Bindings.subtract(this, other) as DoubleBinding
operator fun ObservableNumberValue.minus(other: Int) = Bindings.subtract(this, other) as DoubleBinding
operator fun ObservableNumberValue.times(other: ObservableNumberValue) = Bindings.multiply(this, other) as DoubleBinding
operator fun ObservableNumberValue.times(other: Double) = Bindings.multiply(this, other)!!
operator fun ObservableNumberValue.times(other: Float) = Bindings.multiply(this, other) as DoubleBinding
operator fun ObservableNumberValue.times(other: Long) = Bindings.multiply(this, other) as DoubleBinding
operator fun ObservableNumberValue.times(other: Int) = Bindings.multiply(this, other) as DoubleBinding
operator fun ObservableNumberValue.div(other: ObservableNumberValue) = Bindings.divide(this, other) as DoubleBinding
operator fun ObservableNumberValue.div(other: Double) = Bindings.divide(this, other)!!
operator fun ObservableNumberValue.div(other: Float) = Bindings.divide(this, other) as DoubleBinding
operator fun ObservableNumberValue.div(other: Long) = Bindings.divide(this, other) as DoubleBinding
operator fun ObservableNumberValue.div(other: Int) = Bindings.divide(this, other) as DoubleBinding

operator fun Double.plus(other: ObservableNumberValue) = Bindings.add(this, other)!!
operator fun Float.plus(other: ObservableNumberValue) = Bindings.add(this, other) as DoubleBinding
operator fun Long.plus(other: ObservableNumberValue) = Bindings.add(this, other) as DoubleBinding
operator fun Int.plus(other: ObservableNumberValue) = Bindings.add(this, other) as DoubleBinding
operator fun Double.minus(other: ObservableNumberValue) = Bindings.subtract(this, other)!!
operator fun Float.minus(other: ObservableNumberValue) = Bindings.subtract(this, other) as DoubleBinding
operator fun Long.minus(other: ObservableNumberValue) = Bindings.subtract(this, other) as DoubleBinding
operator fun Int.minus(other: ObservableNumberValue) = Bindings.subtract(this, other) as DoubleBinding
operator fun Double.times(other: ObservableNumberValue) = Bindings.multiply(this, other)!!
operator fun Float.times(other: ObservableNumberValue) = Bindings.multiply(this, other) as DoubleBinding
operator fun Long.times(other: ObservableNumberValue) = Bindings.multiply(this, other) as DoubleBinding
operator fun Int.times(other: ObservableNumberValue) = Bindings.multiply(this, other) as DoubleBinding
operator fun Double.div(other: ObservableNumberValue) = Bindings.divide(this, other)!!
operator fun Float.div(other: ObservableNumberValue) = Bindings.divide(this, other) as DoubleBinding
operator fun Long.div(other: ObservableNumberValue) = Bindings.divide(this, other) as DoubleBinding
operator fun Int.div(other: ObservableNumberValue) = Bindings.divide(this, other) as DoubleBinding

@Experimental("untested")
operator fun ObservableValue<Boolean>.not(): ObservableValue<Boolean> = v(!this@not.value).apply {
   this@not sync { value = !it }
}

@Experimental("untested")
operator fun ObservableValue<Boolean>.plus(other: ObservableValue<Boolean>): ObservableValue<Boolean> =
   v(this@plus.value || other.value).apply {
      this@plus sync { value = it || other.value }
      other sync { value = this@plus.value || it }
   }

@Experimental("untested")
operator fun ObservableValue<Boolean>.times(other: ObservableValue<Boolean>): ObservableValue<Boolean> =
   v(this@times.value && other.value).apply {
      this@times sync { value = it && other.value }
      other sync { value = this@times.value && it }
   }

@Experimental("uses WeakReference and may behave unexpectedly")
fun and(vararg values: ObservableValue<Boolean>): BooleanBinding = object: BooleanBinding() {
   init {
      bind(*values)
   }

   override fun computeValue() = values.any { it.value }
}

@Experimental("uses WeakReference and may behave unexpectedly")
fun or(vararg values: ObservableValue<Boolean>): BooleanBinding = object: BooleanBinding() {
   init {
      bind(*values)
   }

   override fun computeValue() = values.none { it.value }
}


