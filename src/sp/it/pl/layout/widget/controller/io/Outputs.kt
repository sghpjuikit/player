package sp.it.pl.layout.widget.controller.io

import org.reactfx.Subscription
import sp.it.pl.util.reactive.maintain
import sp.it.pl.util.type.isSuperclassOf
import sp.it.pl.util.type.typetoken.TypeToken
import java.util.HashMap
import java.util.UUID
import java.util.function.Consumer

class Outputs {
    private val m = HashMap<String, Output<*>>()

    fun <T> create(id: UUID, name: String, type: TypeToken<in T?>, value: T?): Output<T?> {
        val o = create(id, name, type.rawType, value)
        o.typeT = type
        return o
    }

    fun <T> create(id: UUID, name: String, type: Class<in T>, value: T?): Output<T?> {
        val o = Output(id, name, type)
        o.value = value
        m[name] = o
        return o
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> findOutput(type: Class<T>, name: String): Output<T?>? {
        val i = m[name]
        if (i!=null && type.isSuperclassOf(i.type)) throw ClassCastException()
        return i as Output<T?>?
    }

    fun <T> getOutput(type: Class<T>, name: String): Output<T?> = findOutput(type, name)!!

    inline fun <reified T> findOutput(name: String) = findOutput(T::class.java, name)

    inline fun <reified T> getOutput(name: String) = findOutput(T::class.java, name)!!

    operator fun contains(name: String) = m.containsKey(name)

    operator fun contains(i: Output<*>) = m.containsValue(i) // m.containsKey(i.id.name) <--faster, but not correct

    fun getSize(): Int = m.size

    fun getOutputs(): Collection<Output<*>> = m.values

    @Suppress("IfThenToElvis", "UNCHECKED_CAST")
    fun <T> monitor(name: String, action: Consumer<T>): Subscription {
        val o = m[name] as Output<T>?
        return if (o==null) Subscription.EMPTY else o.`val`.maintain(action)
    }

}