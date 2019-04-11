package sp.it.pl.layout.widget.controller.io

import sp.it.util.dev.failIf
import sp.it.util.type.Util.getRawType
import sp.it.util.type.isSubclassOf
import sp.it.util.type.type
import java.lang.reflect.Type
import java.util.HashMap

class Inputs {
    private val m = HashMap<String, Input<*>>()

    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    inline fun <reified T> create(name: String, initialValue: T? = null, noinline action: (T?) -> Unit) = create(name, type<T?>(), initialValue, action)

    @Suppress("UNCHECKED_CAST")
    @JvmOverloads
    fun <T> create(name: String, type: Type, initialValue: T? = null, action: (T?) -> Unit): Input<T?> {
        failIf(m[name]!=null) { "Input $name already exists" }

        val i: Input<T?> = Input(name, getRawType(type) as Class<T?>, initialValue, action)
        i.typeRaw = type
        m[name] = i
        return i
    }

    fun getInputRaw(name: String): Input<*>? = m[name]

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