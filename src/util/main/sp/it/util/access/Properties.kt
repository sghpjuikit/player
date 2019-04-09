package sp.it.util.access

import javafx.beans.binding.Bindings
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
import sp.it.util.dev.Experimental
import sp.it.util.reactive.sync
import kotlin.reflect.KProperty

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>): T = value
operator fun <T> Property<T?>.setValue(thisRef: Any, property: KProperty<*>, value: T?) = setValue(value)

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

fun WritableValue<Boolean>.toggle() {
    value = !value
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
operator fun ObservableValue<Boolean>.not() = v(!this@not.value).apply {
    this@not sync { value = !it }
}
@Experimental("untested")
operator fun ObservableValue<Boolean>.plus(other: ObservableValue<Boolean>) = v(this@plus.value || other.value).apply {
    this@plus sync { value = it || other.value }
    other sync { value = this@plus.value || it }
}
@Experimental("untested")
operator fun ObservableValue<Boolean>.times(other: ObservableValue<Boolean>) = v(this@times.value && other.value).apply {
    this@times sync { value = it && other.value }
    other sync { value = this@times.value && it }
}