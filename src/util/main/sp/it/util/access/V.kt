package sp.it.util.access

import javafx.beans.property.SimpleObjectProperty
import sp.it.util.functional.invoke
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachChanges
import sp.it.util.reactive.sync
import java.util.function.BiConsumer
import java.util.function.Consumer

/** Var/variable. An object wrapper based on [javafx.beans.property.SimpleObjectProperty]. */
open class V<T>(value: T): SimpleObjectProperty<T>(value), AccessibleValue<T> {

    @Suppress("RedundantOverride")  // helps Kotlin with null-safety inference
    override fun getValue(): T = super.getValue()

    @Suppress("RedundantOverride")  // helps Kotlin with null-safety inference
    override fun setValue(v: T) = super.setValue(v)

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

/** @return new [V] value allowing null values, initialized to the specified value or null if no specified */
fun <T: Any?> vn(value: T? = null): V<T?> = V(value)