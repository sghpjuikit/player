package sp.it.pl.util.access

import javafx.beans.property.SimpleObjectProperty
import org.reactfx.Subscription
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.changes
import java.util.function.BiConsumer
import java.util.function.Consumer

fun <T> v(value: T): V<T> = V(value)
fun <T> v(value: T, onChange: (T) -> Unit): V<T> = V(value, Consumer { onChange(it) } )
fun <T> vn(value: T? = null): V<T?> = V(value)
fun <T> vn(value: T?, onChange: (T?) -> Unit): V<T?> = V(value, Consumer { onChange(it) } )

/**
 * Var/variable - simple object wrapper similar to [javafx.beans.property.Property], but
 * simpler (no binding) and with the ability to apply value change.
 *
 * Does not permit null values.
 */
open class V<T> : SimpleObjectProperty<T>, ApplicableValue<T> {

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

    constructor(value: T, applier: Runnable) : this(value, Consumer { applier() })

    @Suppress("RedundantOverride")  // helps Kotlin with null-safety inference
    override fun getValue(): T = super.getValue()

    @Suppress("RedundantOverride")  // helps Kotlin with null-safety inference
    override fun setValue(v: T) = super.setValue(v)

    override fun applyValue(value: T) {
        applier(value)
    }

    fun onChange(action: Consumer<in T>) = attach { action(it) }

    fun initOnChange(action: Consumer<in T>) = apply { attach { action(it) } }

    fun onChange(action: BiConsumer<in T, in T>) = changes { ov, nv -> action(ov, nv) }

    fun maintain(action: Consumer<in T>): Subscription {
        action(value)
        return attach { action(it) }
    }

    fun onInvalid(action: Runnable) = attach { action() }

}

open class VNullable<T>: V<T?> {

    @JvmOverloads
    constructor(value: T?, applier: Consumer<T?> = Consumer {}) : super(value, applier)

}