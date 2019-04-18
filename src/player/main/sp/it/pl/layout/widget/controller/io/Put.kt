package sp.it.pl.layout.widget.controller.io

import sp.it.util.reactive.Subscription
import java.lang.reflect.Type
import java.util.HashSet

open class Put<T>: XPut<T> {
    @JvmField val name: String
    @JvmField val type: Class<T>
    @JvmField var typeRaw: Type? = null
    @JvmField protected val monitors: MutableSet<(T) -> Unit>
    private var `val`: T

    constructor(type: Class<T>, name: String, initialValue: T) {
        this.name = name
        this.type = type
        this.`val` = initialValue
        this.monitors = HashSet()
    }

    var value: T
        get() = `val`
        set(v) {
            if (value!=v) {
                `val` = v
                monitors.forEach { it(v) }
            }
        }

    fun sync(action: (T) -> Unit): Subscription {
        action(value)
        return attach(action)
    }

    fun attach(action: (T) -> Unit): Subscription {
        monitors += action
        return Subscription { monitors -= action }
    }

}