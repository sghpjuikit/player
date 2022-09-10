package sp.it.util.access

import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.UnaryOperator
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import sp.it.util.functional.invoke
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachChanges
import sp.it.util.reactive.sync

/** Var/variable. An object wrapper based on [javafx.beans.property.SimpleObjectProperty]. */
open class V<T>(value: T): SimpleObjectProperty<T>(value) {

   // helps Kotlin with null-safety inference
   @Suppress("RedundantOverride", "EmptyMethod")
   override fun getValue(): T = super.getValue()

   // helps Kotlin with null-safety inference
   @Suppress("EmptyMethod")  // helps Kotlin with null-safety inference
   override fun setValue(v: T): Unit = super.setValue(v)

   // helps Kotlin with null-safety inference
   override fun <U: Any?> map(mapper: Function<in T, out U>?): ObservableValue<U> = super.map(mapper)

   // helps Kotlin with null-safety inference
   override fun <U: Any?> flatMap(mapper: Function<in T, out ObservableValue<out U>>?): ObservableValue<U> = super.flatMap(mapper)

   /** Java convenience method. Invokes [setValue] on this with the transformed value using the specified mapper. */
   fun setValueOf(op: UnaryOperator<T>) = super.setValue(op(value))

   /** Java convenience method. [attach] that takes [Consumer]. */
   fun attachC(action: Consumer<in T>) = attach { action(it) }

   /** Java convenience method. [sync] that takes [Consumer]. */
   fun syncC(action: Consumer<in T>) = sync { action(it) }

   /** Java convenience method. [attachChanges] that takes [BiConsumer]. */
   fun attachChangesC(action: BiConsumer<in T, in T>) = attachChanges { ov, nv -> action(ov, nv) }

   /** Java convenience method. Invokes [attach] on this with the specified action and returns this. */
   fun initAttachC(action: Consumer<in T>) = apply { attach { action(it) } }

   /** Java convenience method. Invokes [sync] on this with the specified action and returns this. */
   fun initSyncC(action: Consumer<in T>) = apply { sync { action(it) } }

}

/** @return new [V] value disallowing null values, initialized to the specified value */
fun <T: Any> v(value: T): V<T> = V(value)

/** @return new [V] value allowing null values, initialized to the specified value */
fun <T: Any> vn(value: T?): V<T?> = V(value)

/** @return new [V] value with specified nullability, initialized to the specified value, same as `V(value)` */
fun <T: Any?> vx(value: T): V<T> = V(value)