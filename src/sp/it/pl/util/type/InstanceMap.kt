package sp.it.pl.util.type

import sp.it.pl.util.collections.map.ClassListMap

/** Map of instance enumerations per type. Useful for customization by pluggable & extensible behaviors. */
class InstanceMap {
    private val m = ClassListMap<Any> { it.javaClass }

    /** Add instances of the specified type. */
    fun <T> addInstance(type: Class<T>, vararg instances: T) = m.accumulate(type, *instances)

    /** @return instances of the specified type */
    @Suppress("UNCHECKED_CAST")
    fun <T> getInstances(type: Class<T>): List<T> = m[type].orEmpty().asSequence().toList() as List<T>

    /** @return instances of the specified type */
    inline fun <reified T> getInstances(): List<T> = getInstances(T::class.java)

}