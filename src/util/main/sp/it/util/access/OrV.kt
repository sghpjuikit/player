package sp.it.util.access

import javafx.beans.InvalidationListener as IListener
import javafx.beans.property.BooleanProperty as BProperty
import javafx.beans.property.ObjectProperty as OProperty
import javafx.beans.value.ChangeListener as CListener
import javafx.beans.property.Property
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.beans.value.WritableValue
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.OrV.OrValue.Initial.Override
import sp.it.util.reactive.Unsubscribable

/** Value with a parent and a switch to optionally use the parent's value. */
class OrV<T>(parent: Property<T>, initialValue: OrValue.Initial<T> = Inherit()): ObservableValue<T>, WritableValue<T>, Unsubscribable {
   val parent: Property<T> = parent
   val real: OProperty<T> = SimpleObjectProperty(initialValue.computeInitialValue(parent))
   val override: BProperty = SimpleBooleanProperty(initialValue.computeInitialOverride())
   private val actual: OProperty<T> = SimpleObjectProperty(computeValue())
   private val listener = { _: Any? -> actual.value = computeValue() }

   init {
      this.override.addListener(listener)
      this.real.addListener(listener)
      this.parent.addListener(listener)
   }

   override fun addListener(listener: CListener<in T>) = actual.addListener(listener)

   override fun removeListener(listener: CListener<in T>) = actual.removeListener(listener)

   override fun getValue(): T = actual.value

   override fun setValue(v: T) {
      real.value = v
   }

   var valueOr: OrValue<T>
      get() = OrValue(override.value, real.value)
      set(it) { override.value = it.override; real.value = it.value }

   val valueOrInitial: OrValue.Initial<T>
      get() = if (override.value) Override(real.value) else Inherit()

   override fun addListener(listener: IListener) = actual.addListener(listener)

   override fun removeListener(listener: IListener) = actual.removeListener(listener)

   private fun computeValue() = if (override.value) real.value else parent.value

   override fun unsubscribe() {
      override.removeListener(listener)
      real.removeListener(listener)
      parent.removeListener(listener)
   }

   enum class State(val override: Boolean) {
      OVERRIDE(true),
      INHERIT(false);

      companion object {
         fun of(override: Boolean) = if (override) OVERRIDE else INHERIT
      }
   }

   data class OrValue<T>(val override: Boolean, val value: T) {
      sealed class Initial<out T> {
         abstract fun computeInitialValue(parent: Property<@UnsafeVariance T>): T
         abstract fun computeInitialOverride(): Boolean
                  fun computeInitial(parent: Property<@UnsafeVariance T>): OrValue<@UnsafeVariance T> = OrValue(computeInitialOverride(), computeInitialValue(parent))

         class Inherit<T>: Initial<T>() {
            override fun computeInitialValue(parent: Property<T>): T = parent.value
            override fun computeInitialOverride() = false
         }

         data class Override<T>(val value: T): Initial<T>() {
            override fun computeInitialValue(parent: Property<T>): T = value
            override fun computeInitialOverride() = true
         }
      }
   }

}