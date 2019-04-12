package sp.it.pl.layout.widget.controller.io

import sp.it.pl.layout.area.IOLayer
import sp.it.util.dev.Idempotent
import sp.it.util.dev.failIf
import sp.it.util.functional.asIf
import sp.it.util.reactive.Subscription
import sp.it.util.type.Util
import sp.it.util.type.isSubclassOf
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.HashMap
import java.util.UUID

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
    open fun isAssignable(output: Output<*>): Boolean = when {
        type==List::class.java && output.type==List::class.java -> {
            output.typeRaw.listType().isSubclassOf(typeRaw.listType())
        }
        type==List::class.java -> {
            isAssignable(output.type, typeRaw.listType())
        }
        output.type==List::class.java -> false
        else -> isAssignable(output.type, type)
    }

    private fun isAssignable(type1: Class<*>, type2: Class<*>) = type1.isSubclassOf(type2) || type2.isSubclassOf(type1)

    private fun isAssignable(value: Any?): Boolean = when {
        value==null -> true
        type==List::class.java -> typeRaw.listType().isInstance(value)
        else -> type.isInstance(value)
    }

    private fun monitor(output: Output<out T>): Subscription {
        failIf(!isAssignable(output)) { "Input<$type> can not bind to put<${output.type}>" }

        return output.sync { v ->
            if (v!=null) {
                when {
                    type==List::class.java && output.type==List::class.java -> {
                        valueAny = v
                    }
                    type==List::class.java -> {
                        if (typeRaw.listType().isInstance(v))
                            valueAny = v
                    }
                    output.type==List::class.java -> {}
                    else -> {
                        if (type.isInstance(v))
                            valueAny = v
                    }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    var valueAny: Any?
        get() = value
        set(it) {
            value = when (type) {
                List::class.java -> when(it) {
                    is List<*> -> it as T?
                    else -> listOf(it) as T?
                }
                else -> it as T?
            }
        }

    /** Sets value of this input to that of the specified output immediately and on every output value change. */
    @Idempotent
    fun bind(output: Output<out T>): Subscription {
        // Normally we would use this::setValue, but we want to allow generalized binding, which supports subtyping
        // and selective type filtering
        // sources.computeIfAbsent(output, o -> o.monitor(this::setValue));
        sources.computeIfAbsent(output) { monitor(it) }
        IOLayer.addConnectionE(this, output)
        return Subscription { unbind(output) }
    }

    @Suppress("UNCHECKED_CAST")
    @Idempotent
    fun <R> bind(output: Output<out R?>, mapper: (R) -> T): Subscription {
        // Normally we would use this::setValue, but we want to allow generalized binding, which supports subtyping
        // and selective type filtering
        // sources.computeIfAbsent(output, o -> o.monitor(this::setValue));
        sources.computeIfAbsent(output as Output<T>) { it.sync { value = if (it==null) null else mapper(it as R) } }
        IOLayer.addConnectionE(this, output)
        return Subscription { unbind(output) }
    }

    /**
     * @param exceptWithId id of outputs to be not considered as bindings even if this is bound to any of them
     * @return true iff at least one [Output] is bound to this input using [bind]. ]
     */
    @JvmOverloads
    fun isBound(exceptWithId: UUID? = null): Boolean = sources.keys.any { it.id.carrier_id!=exceptWithId }

    @Idempotent
    fun unbind(output: Output<*>) {
        sources.remove(output)?.unsubscribe()
        IOLayer.remConnectionE(this, output)
    }

    fun unbindAll() {
        sources.values.forEach { it.unsubscribe() }
        sources.clear()
    }

    fun getSources(): Set<Output<out T>> = sources.keys

    override fun toString() = "$name, $type"

    companion object {
        private fun Type?.listType() = Util.getRawType(this.asIf<ParameterizedType>()!!.actualTypeArguments[0])
    }
}