package sp.it.util.access

import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue

/** @return observable value that never changes and is always set to the specified value */
fun <T> vAlways(v: T): ObservableValue<T> = AlwaysProperty(v)

/** @return observable value that never changes and is always set to the specified value */
fun vAlways(v: Boolean): ObservableValue<Boolean> = if (v) AlwaysTrueProperty else AlwaysFalseProperty

private object AlwaysTrueProperty: AlwaysProperty<Boolean>(true)
private object AlwaysFalseProperty: AlwaysProperty<Boolean>(false)

private open class AlwaysProperty<T>(val v: T): ObservableValue<T> {
   override fun removeListener(listener: ChangeListener<in T>) = Unit
   override fun removeListener(listener: InvalidationListener) = Unit
   override fun addListener(listener: ChangeListener<in T>) = Unit
   override fun addListener(listener: InvalidationListener) = Unit
   override fun getValue() = v
}