package sp.it.pl.layout.widget.controller.io

import sp.it.pl.util.type.isSubclassOf
import java.util.HashMap
import java.util.function.Consumer

class Inputs {
    private val m = HashMap<String, Input<*>>()

    inline fun <reified T> create(name: String, crossinline action: (T?) -> Unit) = create<T>(name, T::class.java, Consumer { action(it) })
    inline fun <reified T> create(name: String, initialValue: T?, crossinline action: (T?) -> Unit) = create<T>(name, T::class.java, initialValue, Consumer { action(it) })

    @Suppress("UNCHECKED_CAST")
    fun <T> create(name: String, type: Class<T>, action: Consumer<in T?>): Input<T?> {
        val i: Input<T?> = Input(name, type as Class<T?>, action)
        m[name] = i
        return i
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> create(name: String, type: Class<T>, initialValue: T?, action: Consumer<in T?>): Input<T?> {
        val i: Input<T?> = Input(name, type as Class<T?>, initialValue, action)
        m[name] = i
        return i
    }

    @Suppress("UNCHECKED_CAST")
    @Deprecated("unsafe")
    fun <T> getInputRaw(name: String): Input<T?>? = m[name] as Input<T?>?

    @Suppress("UNCHECKED_CAST")
    fun <T> findInput(type: Class<T>, name: String): Input<T?>? {
        val i = m[name]
        if (i!=null && !type.isSubclassOf(i.type)) throw ClassCastException()
        return i as Input<T?>?
    }

    fun <T> getInput(type: Class<T>, name: String): Input<T?> = findInput(type, name)!!

    inline fun <reified T> findInput(name: String) = findInput(T::class.java, name)

    inline fun <reified T> getInput(name: String) = findInput(T::class.java, name)!!

    operator fun contains(name: String) = m.containsKey(name)

    operator fun contains(i: Input<*>) = m.containsValue(i) // m.containsKey(i.name) <-- faster, but not correct

    fun getSize(): Int = m.size

    fun getInputs(): Collection<Input<*>> = m.values

}