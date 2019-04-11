package sp.it.pl.layout.widget.controller.io

import sp.it.util.dev.failIf
import sp.it.util.type.Util.getRawType
import sp.it.util.type.isSuperclassOf
import sp.it.util.type.type
import java.lang.reflect.Type
import java.util.HashMap
import java.util.UUID

class Outputs {
    private val m = HashMap<String, Output<*>>()

    inline fun <reified T> create(id: UUID, name: String, value: T?): Output<T?> = create(id, name, type<T?>(), value)

    @Suppress("UNCHECKED_CAST")
    fun <T> create(id: UUID, name: String, type: Type, value: T?): Output<T?> {
        failIf(m[name]!=null) { "Output $name already exists" }

        val o = Output(id, name, getRawType(type) as Class<T?>)
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

}