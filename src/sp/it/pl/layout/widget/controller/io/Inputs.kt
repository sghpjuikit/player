package sp.it.pl.layout.widget.controller.io

import sp.it.pl.util.type.isSubclassOf
import java.util.HashMap
import java.util.function.Consumer

class Inputs {
    private val m = HashMap<String, Input<*>>()

    fun <T> create(name: String, type: Class<in T>, action: Consumer<in T>): Input<T> {
        val i = Input(name, type, action)
        m[name] = i
        return i
    }

    fun <T> create(name: String, type: Class<in T>, init_val: T, action: Consumer<in T>): Input<T> {
        val i = Input(name, type, init_val, action)
        m[name] = i
        return i
    }

    @Suppress("UNCHECKED_CAST")
    @Deprecated("unsafe")
    fun <T> getInputRaw(name: String): Input<T>? = m[name] as Input<T>?

    @Suppress("UNCHECKED_CAST")
    fun <T> findInput(type: Class<T>, name: String): Input<T>? {
        val i = m[name]
        if (i!=null && type.isSubclassOf(i.type)) throw ClassCastException()
        return i as Input<T>?
    }

    fun <T> getInput(type: Class<T>, name: String): Input<T> = findInput(type, name)!!

    inline fun <reified T> findInput(name: String) = findInput(T::class.java, name)

    inline fun <reified T> getInput(name: String) = findInput(T::class.java, name)!!

    operator fun contains(name: String) = m.containsKey(name)

    operator fun contains(i: Input<*>) = m.containsValue(i) // m.containsKey(i.name) <-- faster, but not correct

    fun getSize(): Int = m.size

    fun getInputs(): Collection<Input<*>> = m.values

}