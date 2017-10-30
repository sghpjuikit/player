package util.access

import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ChangeListener
import org.reactfx.Subscription
import java.util.function.BiConsumer
import java.util.function.Consumer

fun <T> v(value: T) = V(value)
fun <T> v(value: T, onChange: (T) -> Unit) = V(value, Consumer { onChange(it) } )
fun <T> vn(value: T? = null) = V(value)
fun <T> vn(value: T?, onChange: (T?) -> Unit) = V(value, Consumer { onChange(it) } )

/**
 * Var/variable - simple object wrapper similar to [javafx.beans.property.Property], but
 * simpler (no binding) and with the ability to apply value change.
 *
 *
 * Does not permit null values.
 */
open class V<T> : SimpleObjectProperty<T>, ApplicableValue<T> {

    var applier: Consumer<T>

    /**
     * Sets applier. Applier is a code that applies the value in any way.
     *
     * @param applier or null to disable applying
     */
    @JvmOverloads
    constructor(value: T, applier: Consumer<T> = Consumer<T> {}) {
        this.value = value
        this.applier = applier
    }

    constructor(value: T, applier: Runnable) : this(value, Consumer { applier.run() })

    override fun applyValue(`val`: T) {
        applier.accept(`val`)
    }

    fun onChange(action: Consumer<in T>): Subscription {
        val l = ChangeListener<T> { _, _, nv -> action.accept(nv) }
        addListener(l)
        return Subscription { removeListener(l) }
    }

    fun onChange(action: BiConsumer<in T, in T>): Subscription {
        val l = ChangeListener<T> { _, ov, nv -> action.accept(ov, nv) }
        addListener(l)
        return Subscription { removeListener(l) }
    }

    fun maintain(action: Consumer<in T>): Subscription {
        val l = ChangeListener<T> { _, _, nv -> action.accept(nv) }
        addListener(l)
        action.accept(value)
        return Subscription { removeListener(l) }
    }

    fun onInvalid(action: Runnable): Subscription {
        val l = ChangeListener<T> {  _, _, _ -> action.run() }
        addListener(l)
        return Subscription { removeListener(l) }
    }

}

open class VNullable<T>: V<T?> {
    @JvmOverloads
    constructor(value: T?, applier: Consumer<T?> = Consumer {}) : super(value, applier)
    constructor(value: T?, applier: Runnable) : super(value, applier)
}