package sp.it.pl.util.access

import javafx.beans.property.SimpleObjectProperty
import org.reactfx.Subscription
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.attachChanges
import sp.it.pl.util.reactive.sync
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Var/variable - simple object wrapper similar to [javafx.beans.property.Property], but
 * simpler (no binding) and with the ability to apply value change.
 *
 * Does not permit null values.
 */
open class V<T>: SimpleObjectProperty<T>, ApplicableValue<T> {

    var applier: Consumer<in T>

    /**
     * Sets applier. Applier is a code that applies the value in any way.
     *
     * @param applier or null to disable applying
     */
    @JvmOverloads
    constructor(value: T, applier: Consumer<in T> = Consumer {}) {
        this.value = value
        this.applier = applier
    }

    @Suppress("RedundantOverride")  // helps Kotlin with null-safety inference
    override fun getValue(): T = super.getValue()

    @Suppress("RedundantOverride")  // helps Kotlin with null-safety inference
    override fun setValue(v: T) = super.setValue(v)

    override fun applyValue(value: T) {
        applier(value)
    }

    fun onChange(action: Consumer<in T>) = attach { action(it) }

    fun initAttachC(action: Consumer<in T>) = apply { attach { action(it) } }

    fun initSyncC(action: Consumer<in T>) = apply { sync { action(it) } }

    fun onChange(action: BiConsumer<in T, in T>) = attachChanges { ov, nv -> action(ov, nv) }

    fun maintain(action: Consumer<in T>): Subscription {
        action(value)
        return attach { action(it) }
    }

}

fun <T: Any> v(value: T): V<T> = V(value)

fun <T: Any?> vn(value: T? = null): V<T?> = V(value)

fun <T, W: V<T>> W.initAttach(action: (T) -> Unit) = apply { attach { action(it) } }

fun <T, W: V<T>> W.initSync(action: (T) -> Unit) = apply { sync(action) }