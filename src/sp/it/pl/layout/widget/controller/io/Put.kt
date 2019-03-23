package sp.it.pl.layout.widget.controller.io

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import sp.it.pl.util.reactive.Subscription
import java.util.HashSet

open class Put<T>: XPut<T> {
    @JvmField val type: Class<T>
    @JvmField protected val `val`: ObjectProperty<T>
    @JvmField protected val monitors: MutableSet<(T) -> Unit>

    constructor(type: Class<T>, init_val: T) {
        this.type = type
        this.`val` = SimpleObjectProperty(init_val)
        this.monitors = HashSet()
    }

    var value: T
        get() = `val`.get()
        set(v) {
            `val`.value = v
            monitors.forEach { m -> m(v) }
        }

    fun sync(action: (T) -> Unit): Subscription {
        monitors += action
        action(value)
        return Subscription { monitors -= action }
    }

    fun attach(action: (T) -> Unit): Subscription {
        monitors += action
        return Subscription { monitors -= action }
    }

}