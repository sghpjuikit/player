package sp.it.util.type

import java.lang.reflect.Type

/** Map of instances per type. Useful for customization by pluggable & extensible behaviors. */
open class InstanceMap {
    private val m = HashMap<List<Class<*>>, MutableList<Any>>()

    /** Add instances of the specified type. */
    fun <T: Any> addInstances(type: List<Class<*>>, instances: Collection<T>) = m.computeIfAbsent(type) { ArrayList() }.addAll(instances)

    /** Add instances of the type represented by the flattened list of specified classes. */
    fun <T: Any> addInstances(type: Type, instances: Collection<T>) = addInstances(type.flattenToRawTypes().toList(), instances)

    /** Add instances of the type represented by the specified generic type argument. */
    inline fun <reified T: Any> addInstances(vararg instances: T) = addInstances(typeLiteral<T>(), instances.toList())

    /** @return instances of the type represented by the flattened list of specified classes */
    @Suppress("UNCHECKED_CAST")
    fun <T> getInstances(type: List<Class<*>>): List<T> = m[type].orEmpty() as List<T>

    /** @return instances of the specified type */
    fun <T> getInstances(type: Type): List<T> = getInstances(type.flattenToRawTypes().toList())

    /**
     * Note on nullability:
     *
     * The nullability of the specified generic type argument will be respected, thus if it is nullable, the sequence
     * will contain null.
     *
     * @return instances of the type represented byt the specified generic type argument */
    inline fun <reified T: Any?> getInstances(): Sequence<T> = getInstances<T>(typeLiteral<T>()).asSequence().let {
        val isNullable = null is T
        if (isNullable) it.plus(null as T)
        else it
    }

}