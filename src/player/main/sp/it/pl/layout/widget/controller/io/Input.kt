package sp.it.pl.layout.widget.controller.io

import sp.it.pl.layout.area.IOLayer
import sp.it.util.reactive.Subscription
import java.util.HashMap

open class Input<T>: Put<T?> {

    private val sources = HashMap<Output<out T>, Subscription>()

    constructor(name: String, type: Class<T?>, initialValue: T? = null, action: (T?) -> Unit): super(type, name, initialValue) {
        attach(action)
    }

    /**
     * Return true if this input can receive values from given output. Equivalent to
     *
     * `getType().isAssignableFrom(output.getType())`
     */
    open fun canBind(output: Output<*>): Boolean {
        return output.type.isAssignableFrom(type) || type.isAssignableFrom(output.type)
    }

    /**
     * Binds to the output.
     * Sets its value immediately and then every time it changes.
     * Binding multiple times has no effect.
     */
    @Suppress("UNCHECKED_CAST")
    fun bind(output: Output<out T>): Subscription {
        // Normally we would use this::setValue, but we want to allow generalized binding, which supports subtyping
        // and selective type filtering
        // sources.computeIfAbsent(output, o -> o.monitor(this::setValue));
        sources.computeIfAbsent(output) { (it as Output<T>).monitor(this) }
        IOLayer.addConnectionE(this, output)
        return Subscription { unbind(output) }
    }

    fun unbind(output: Output<out T>) {
        sources.remove(output)?.unsubscribe()
        IOLayer.remConnectionE(this, output)
    }

    fun unbindAll() {
        sources.values.forEach { it.unsubscribe() }
        sources.clear()
    }

    fun getSources(): Set<Output<out T>> = sources.keys

    override fun toString() = "$name, $type"

}