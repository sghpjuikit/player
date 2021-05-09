package sp.it.util.access

import javafx.beans.InvalidationListener
import javafx.beans.value.ChangeListener
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue

/** @return writable value that reads from this value and sets to the specified setter. Convenience for legacy code. */
fun <T> ObservableValue<T>.toWritable(setter: (T) -> Unit) = WithSetterObservableValue(this, setter)

data class WithSetterObservableValue<T>(private val source: ObservableValue<T>, private val setter: (T) -> Unit): ObservableValue<T>, WritableValue<T> {
   override fun addListener(listener: ChangeListener<in T>) = source.addListener(listener)
   override fun addListener(listener: InvalidationListener) = source.addListener(listener)
   override fun removeListener(listener: ChangeListener<in T>) = source.removeListener(listener)
   override fun removeListener(listener: InvalidationListener) = source.removeListener(listener)
   override fun getValue(): T = source.value
   override fun setValue(value: T) = setter(value)
}