package sp.it.util.access

import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import javafx.beans.InvalidationListener as IListener
import javafx.beans.property.BooleanProperty as BProperty
import javafx.beans.property.ObjectProperty as OProperty
import javafx.beans.value.ChangeListener as CListener

/** Value with a parent and a switch to optionally use the parent's value. */
class OrV<T>: ObservableValue<T>, WritableValue<T> {
   val parent: Property<T>
   val real: OProperty<T>
   val override: BProperty
   private val current: OProperty<T>

   constructor(parentValue: Property<T>, initialValue: T = parentValue.value, overrideValue: Boolean = false) {
      parent = parentValue
      override = SimpleBooleanProperty(overrideValue)
      real = SimpleObjectProperty(initialValue)
      current = SimpleObjectProperty(computeValue())

      val l = CListener<Any?> { _, _, _ -> change() }
      override.addListener(l)
      real.addListener(l)
      parent.addListener(l)
   }

   override fun addListener(listener: CListener<in T>) = current.addListener(listener)

   override fun removeListener(listener: CListener<in T>) = current.removeListener(listener)

   override fun getValue(): T = current.value

   override fun setValue(v: T) {
      real.value = v
   }

   override fun addListener(listener: IListener) = current.addListener(listener)

   override fun removeListener(listener: IListener) = current.removeListener(listener)

   private fun computeValue() = if (override.value) real.value else parent.value

   private fun change() {
      val t = computeValue()
      if (t!=current.value) current.value = t
   }

}