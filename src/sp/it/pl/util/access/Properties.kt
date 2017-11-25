/*
 * Impl based on TornadoFX
 *
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 */

package sp.it.pl.util.access

import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.beans.property.FloatProperty
import javafx.beans.property.IntegerProperty
import javafx.beans.property.LongProperty
import javafx.beans.property.Property
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleFloatProperty
import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableDoubleValue
import javafx.beans.value.ObservableFloatValue
import javafx.beans.value.ObservableIntegerValue
import javafx.beans.value.ObservableLongValue
import javafx.beans.value.ObservableNumberValue
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableDoubleValue
import javafx.beans.value.WritableFloatValue
import javafx.beans.value.WritableIntegerValue
import javafx.beans.value.WritableLongValue
import javafx.beans.value.WritableValue
import kotlin.reflect.KProperty

operator fun <T> ObservableValue<T>.getValue(thisRef: Any, property: KProperty<*>) = value
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

fun WritableValue<Boolean>.not() {
    value = !value
}

operator fun ObservableDoubleValue.plus(other: Number): DoubleProperty = SimpleDoubleProperty(get()+other.toDouble())
operator fun ObservableDoubleValue.plus(other: ObservableNumberValue): DoubleProperty = SimpleDoubleProperty(get()+other.doubleValue())
operator fun WritableDoubleValue.plusAssign(other: Number) = set(get()+other.toDouble())
operator fun WritableDoubleValue.plusAssign(other: ObservableNumberValue) = set(get()+other.doubleValue())

operator fun DoubleProperty.inc(): DoubleProperty {
    set(get()+1.0)
    return this
}

operator fun ObservableDoubleValue.minus(other: Number): DoubleProperty = SimpleDoubleProperty(get()-other.toDouble())
operator fun ObservableDoubleValue.minus(other: ObservableNumberValue): DoubleProperty = SimpleDoubleProperty(get()-other.doubleValue())
operator fun WritableDoubleValue.minusAssign(other: Number) = set(get()-other.toDouble())
operator fun WritableDoubleValue.minusAssign(other: ObservableNumberValue) = set(get()-other.doubleValue())

operator fun ObservableDoubleValue.unaryMinus(): DoubleProperty = SimpleDoubleProperty(-get())

operator fun DoubleProperty.dec(): DoubleProperty {
    set(get()-1.0)
    return this
}

operator fun ObservableDoubleValue.times(other: Number): DoubleProperty = SimpleDoubleProperty(get()*other.toDouble())
operator fun ObservableDoubleValue.times(other: ObservableNumberValue): DoubleProperty = SimpleDoubleProperty(get()*other.doubleValue())
operator fun WritableDoubleValue.timesAssign(other: Number) = set(get()*other.toDouble())
operator fun WritableDoubleValue.timesAssign(other: ObservableNumberValue) = set(get()*other.doubleValue())

operator fun ObservableDoubleValue.div(other: Number): DoubleProperty = SimpleDoubleProperty(get()/other.toDouble())
operator fun ObservableDoubleValue.div(other: ObservableNumberValue): DoubleProperty = SimpleDoubleProperty(get()/other.doubleValue())
operator fun WritableDoubleValue.divAssign(other: Number) = set(get()/other.toDouble())
operator fun WritableDoubleValue.divAssign(other: ObservableNumberValue) = set(get()/other.doubleValue())

operator fun ObservableDoubleValue.rem(other: Number): DoubleProperty = SimpleDoubleProperty(get()%other.toDouble())
operator fun ObservableDoubleValue.rem(other: ObservableNumberValue): DoubleProperty = SimpleDoubleProperty(get()%other.doubleValue())
operator fun WritableDoubleValue.remAssign(other: Number) = set(get()%other.toDouble())
operator fun WritableDoubleValue.remAssign(other: ObservableNumberValue) = set(get()%other.doubleValue())

operator fun ObservableDoubleValue.compareTo(other: Number): Int {
    if (get()>other.toDouble())
        return 1
    else if (get()<other.toDouble())
        return -1
    else
        return 0
}

operator fun ObservableDoubleValue.compareTo(other: ObservableNumberValue): Int {
    if (get()>other.doubleValue())
        return 1
    else if (get()<other.doubleValue())
        return -1
    else
        return 0
}


operator fun ObservableFloatValue.plus(other: Number): FloatProperty
        = SimpleFloatProperty(get()+other.toFloat())

operator fun ObservableFloatValue.plus(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get()+other.floatValue())

operator fun WritableFloatValue.plusAssign(other: Number)
        = set(get()+other.toFloat())

operator fun WritableFloatValue.plusAssign(other: ObservableNumberValue)
        = set(get()+other.floatValue())

operator fun FloatProperty.inc(): FloatProperty {
    set(get()+1.0f)
    return this
}

operator fun ObservableFloatValue.minus(other: Number): FloatProperty
        = SimpleFloatProperty(get()-other.toFloat())

operator fun ObservableFloatValue.minus(other: ObservableNumberValue): FloatProperty = SimpleFloatProperty(get()-other.floatValue())

operator fun WritableFloatValue.minusAssign(other: Number)
        = set(get()-other.toFloat())

operator fun WritableFloatValue.minusAssign(other: ObservableNumberValue)
        = set(get()-other.floatValue())

operator fun ObservableFloatValue.unaryMinus(): FloatProperty
        = SimpleFloatProperty(-get())

operator fun FloatProperty.dec(): FloatProperty {
    set(get()-1.0f)
    return this
}

operator fun ObservableFloatValue.times(other: Number): FloatProperty
        = SimpleFloatProperty(get()*other.toFloat())

operator fun ObservableFloatValue.times(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get()*other.floatValue())

operator fun WritableFloatValue.timesAssign(other: Number)
        = set(get()*other.toFloat())

operator fun WritableFloatValue.timesAssign(other: ObservableNumberValue)
        = set(get()*other.floatValue())


operator fun ObservableFloatValue.div(other: Number): FloatProperty
        = SimpleFloatProperty(get()/other.toFloat())

operator fun ObservableFloatValue.div(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get()/other.floatValue())

operator fun WritableFloatValue.divAssign(other: Number)
        = set(get()/other.toFloat())

operator fun WritableFloatValue.divAssign(other: ObservableNumberValue)
        = set(get()/other.floatValue())


operator fun ObservableFloatValue.rem(other: Number): FloatProperty
        = SimpleFloatProperty(get()%other.toFloat())

operator fun ObservableFloatValue.rem(other: ObservableNumberValue): FloatProperty
        = SimpleFloatProperty(get()%other.floatValue())

operator fun WritableFloatValue.remAssign(other: Number)
        = set(get()%other.toFloat())

operator fun WritableFloatValue.remAssign(other: ObservableNumberValue)
        = set(get()%other.floatValue())

operator fun ObservableFloatValue.compareTo(other: Number): Int {
    if (get()>other.toFloat())
        return 1
    else if (get()<other.toFloat())
        return -1
    else
        return 0
}

operator fun ObservableFloatValue.compareTo(other: ObservableNumberValue): Int {
    if (get()>other.floatValue())
        return 1
    else if (get()<other.floatValue())
        return -1
    else
        return 0
}


operator fun ObservableIntegerValue.plus(other: Int): IntegerProperty
        = SimpleIntegerProperty(get()+other)

operator fun ObservableIntegerValue.plus(other: Long): LongProperty
        = SimpleLongProperty(get()+other)

operator fun ObservableIntegerValue.plus(other: Float): FloatProperty
        = SimpleFloatProperty(get()+other)

operator fun ObservableIntegerValue.plus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()+other)

operator fun ObservableIntegerValue.plus(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get()+other.get())

operator fun ObservableIntegerValue.plus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()+other.get())

operator fun ObservableIntegerValue.plus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()+other.get())

operator fun ObservableIntegerValue.plus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()+other.get())


operator fun WritableIntegerValue.plusAssign(other: Number)
        = set(get()+other.toInt())

operator fun WritableIntegerValue.plusAssign(other: ObservableNumberValue)
        = set(get()+other.intValue())


operator fun IntegerProperty.inc(): IntegerProperty {
    set(get()+1)
    return this
}

operator fun ObservableIntegerValue.minus(other: Int): IntegerProperty
        = SimpleIntegerProperty(get()-other)

operator fun ObservableIntegerValue.minus(other: Long): LongProperty
        = SimpleLongProperty(get()-other)

operator fun ObservableIntegerValue.minus(other: Float): FloatProperty
        = SimpleFloatProperty(get()-other)

operator fun ObservableIntegerValue.minus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()-other)

operator fun ObservableIntegerValue.minus(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get()-other.get())

operator fun ObservableIntegerValue.minus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()-other.get())

operator fun ObservableIntegerValue.minus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()-other.get())

operator fun ObservableIntegerValue.minus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()-other.get())


operator fun WritableIntegerValue.minusAssign(other: Number)
        = set(get()-other.toInt())

operator fun WritableIntegerValue.minusAssign(other: ObservableNumberValue)
        = set(get()-other.intValue())


operator fun ObservableIntegerValue.unaryMinus(): IntegerProperty
        = SimpleIntegerProperty(-get())


operator fun IntegerProperty.dec(): IntegerProperty {
    set(get()-1)
    return this
}

operator fun ObservableIntegerValue.times(other: Int): IntegerProperty
        = SimpleIntegerProperty(get()*other)

operator fun ObservableIntegerValue.times(other: Long): LongProperty
        = SimpleLongProperty(get()*other)

operator fun ObservableIntegerValue.times(other: Float): FloatProperty
        = SimpleFloatProperty(get()*other)

operator fun ObservableIntegerValue.times(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()*other)

operator fun ObservableIntegerValue.times(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get()*other.get())

operator fun ObservableIntegerValue.times(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()*other.get())

operator fun ObservableIntegerValue.times(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()*other.get())

operator fun ObservableIntegerValue.times(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()*other.get())


operator fun WritableIntegerValue.timesAssign(other: Number)
        = set(get()*other.toInt())

operator fun WritableIntegerValue.timesAssign(other: ObservableNumberValue)
        = set(get()*other.intValue())


operator fun ObservableIntegerValue.div(other: Int): IntegerProperty
        = SimpleIntegerProperty(get()/other)

operator fun ObservableIntegerValue.div(other: Long): LongProperty
        = SimpleLongProperty(get()/other)

operator fun ObservableIntegerValue.div(other: Float): FloatProperty
        = SimpleFloatProperty(get()/other)

operator fun ObservableIntegerValue.div(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()/other)

operator fun ObservableIntegerValue.div(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get()/other.get())

operator fun ObservableIntegerValue.div(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()/other.get())

operator fun ObservableIntegerValue.div(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()/other.get())

operator fun ObservableIntegerValue.div(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()/other.get())


operator fun WritableIntegerValue.divAssign(other: Number)
        = set(get()/other.toInt())

operator fun WritableIntegerValue.divAssign(other: ObservableNumberValue)
        = set(get()/other.intValue())


operator fun ObservableIntegerValue.rem(other: Int): IntegerProperty
        = SimpleIntegerProperty(get()%other)

operator fun ObservableIntegerValue.rem(other: Long): LongProperty
        = SimpleLongProperty(get()%other)

operator fun ObservableIntegerValue.rem(other: Float): FloatProperty
        = SimpleFloatProperty(get()%other)

operator fun ObservableIntegerValue.rem(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()%other)

operator fun ObservableIntegerValue.rem(other: ObservableIntegerValue): IntegerProperty
        = SimpleIntegerProperty(get()%other.get())

operator fun ObservableIntegerValue.rem(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()%other.get())

operator fun ObservableIntegerValue.rem(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()%other.get())

operator fun ObservableIntegerValue.rem(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()%other.get())


operator fun WritableIntegerValue.remAssign(other: Number)
        = set(get()%other.toInt())

operator fun WritableIntegerValue.remAssign(other: ObservableNumberValue)
        = set(get()%other.intValue())


operator fun ObservableIntegerValue.rangeTo(other: ObservableIntegerValue): Sequence<IntegerProperty> {
    val sequence = mutableListOf<IntegerProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleIntegerProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableIntegerValue.rangeTo(other: Int): Sequence<IntegerProperty> {
    val sequence = mutableListOf<IntegerProperty>()
    for (i in get()..other) {
        sequence += SimpleIntegerProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableIntegerValue.rangeTo(other: ObservableLongValue): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableIntegerValue.rangeTo(other: Long): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}


operator fun ObservableIntegerValue.compareTo(other: Number): Int {
    if (get()>other.toDouble())
        return 1
    else if (get()<other.toDouble())
        return -1
    else
        return 0
}

operator fun ObservableIntegerValue.compareTo(other: ObservableNumberValue): Int {
    if (get()>other.doubleValue())
        return 1
    else if (get()<other.doubleValue())
        return -1
    else
        return 0
}


operator fun ObservableLongValue.plus(other: Int): LongProperty
        = SimpleLongProperty(get()+other.toLong())

operator fun ObservableLongValue.plus(other: Long): LongProperty
        = SimpleLongProperty(get()+other)

operator fun ObservableLongValue.plus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()+other)

operator fun ObservableLongValue.plus(other: Float): FloatProperty
        = SimpleFloatProperty(get()+other)

operator fun ObservableLongValue.plus(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get()+other.intValue())

operator fun ObservableLongValue.plus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()+other.longValue())

operator fun ObservableLongValue.plus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()+other.doubleValue())

operator fun ObservableLongValue.plus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()+other.floatValue())


operator fun WritableLongValue.plusAssign(other: Number)
        = set(get()+other.toLong())

operator fun WritableLongValue.plusAssign(other: ObservableNumberValue)
        = set(get()+other.longValue())


operator fun LongProperty.inc(): LongProperty {
    set(get()+1)
    return this
}

operator fun ObservableLongValue.minus(other: Int): LongProperty
        = SimpleLongProperty(get()-other.toLong())

operator fun ObservableLongValue.minus(other: Long): LongProperty
        = SimpleLongProperty(get()-other)

operator fun ObservableLongValue.minus(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()-other)

operator fun ObservableLongValue.minus(other: Float): FloatProperty
        = SimpleFloatProperty(get()-other)

operator fun ObservableLongValue.minus(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get()-other.intValue())

operator fun ObservableLongValue.minus(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()-other.longValue())

operator fun ObservableLongValue.minus(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()-other.doubleValue())

operator fun ObservableLongValue.minus(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()-other.floatValue())


operator fun WritableLongValue.minusAssign(other: Number)
        = set(get()-other.toLong())

operator fun WritableLongValue.minusAssign(other: ObservableNumberValue)
        = set(get()-other.longValue())


operator fun ObservableLongValue.unaryMinus(): LongProperty
        = SimpleLongProperty(-get())


operator fun LongProperty.dec(): LongProperty {
    set(get()-1)
    return this
}

operator fun ObservableLongValue.times(other: Int): LongProperty
        = SimpleLongProperty(get()*other.toLong())

operator fun ObservableLongValue.times(other: Long): LongProperty
        = SimpleLongProperty(get()*other)

operator fun ObservableLongValue.times(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()*other)

operator fun ObservableLongValue.times(other: Float): FloatProperty
        = SimpleFloatProperty(get()*other)

operator fun ObservableLongValue.times(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get()*other.intValue())

operator fun ObservableLongValue.times(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()*other.longValue())

operator fun ObservableLongValue.times(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()*other.doubleValue())

operator fun ObservableLongValue.times(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()*other.floatValue())


operator fun WritableLongValue.timesAssign(other: Number)
        = set(get()*other.toLong())

operator fun WritableLongValue.timesAssign(other: ObservableNumberValue)
        = set(get()*other.longValue())


operator fun ObservableLongValue.div(other: Int): LongProperty
        = SimpleLongProperty(get()/other.toLong())

operator fun ObservableLongValue.div(other: Long): LongProperty
        = SimpleLongProperty(get()/other)

operator fun ObservableLongValue.div(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()/other)

operator fun ObservableLongValue.div(other: Float): FloatProperty
        = SimpleFloatProperty(get()/other)

operator fun ObservableLongValue.div(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get()/other.intValue())

operator fun ObservableLongValue.div(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()/other.longValue())

operator fun ObservableLongValue.div(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()/other.doubleValue())

operator fun ObservableLongValue.div(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()/other.floatValue())


operator fun WritableLongValue.divAssign(other: Number)
        = set(get()/other.toLong())

operator fun WritableLongValue.divAssign(other: ObservableNumberValue)
        = set(get()/other.longValue())


operator fun ObservableLongValue.rem(other: Int): LongProperty
        = SimpleLongProperty(get()%other.toLong())

operator fun ObservableLongValue.rem(other: Long): LongProperty
        = SimpleLongProperty(get()%other)

operator fun ObservableLongValue.rem(other: Double): DoubleProperty
        = SimpleDoubleProperty(get()%other)

operator fun ObservableLongValue.rem(other: Float): FloatProperty
        = SimpleFloatProperty(get()%other)

operator fun ObservableLongValue.rem(other: ObservableIntegerValue): LongProperty
        = SimpleLongProperty(get()%other.intValue())

operator fun ObservableLongValue.rem(other: ObservableLongValue): LongProperty
        = SimpleLongProperty(get()%other.longValue())

operator fun ObservableLongValue.rem(other: ObservableDoubleValue): DoubleProperty
        = SimpleDoubleProperty(get()%other.doubleValue())

operator fun ObservableLongValue.rem(other: ObservableFloatValue): FloatProperty
        = SimpleFloatProperty(get()%other.floatValue())


operator fun WritableLongValue.remAssign(other: Number)
        = set(get()%other.toLong())

operator fun WritableLongValue.remAssign(other: ObservableNumberValue)
        = set(get()%other.longValue())


operator fun ObservableLongValue.rangeTo(other: ObservableLongValue): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.rangeTo(other: Long): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.rangeTo(other: ObservableIntegerValue): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other.get()) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.rangeTo(other: Int): Sequence<LongProperty> {
    val sequence = mutableListOf<LongProperty>()
    for (i in get()..other) {
        sequence += SimpleLongProperty(i)
    }
    return sequence.asSequence()
}

operator fun ObservableLongValue.compareTo(other: Number): Int {
    if (get()>other.toDouble())
        return 1
    else if (get()<other.toDouble())
        return -1
    else
        return 0
}

operator fun ObservableLongValue.compareTo(other: ObservableNumberValue): Int {
    if (get()>other.doubleValue())
        return 1
    else if (get()<other.doubleValue())
        return -1
    else
        return 0
}
