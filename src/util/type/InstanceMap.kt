package util.type

import util.collections.map.ClassListMap

/** Map of instance enumerations per type. Useful for customization by pluggable & extensible behaviors. */
class InstanceMap {
    private val m = ClassListMap<Any> { it.javaClass }

    /** Add instances of specified type. */
    fun <T> addInstance(type: Class<T>, vararg instances: T) = m.accumulate(type, *instances)

    /** Get instances of specified type. */
    @Suppress("UNCHECKED_CAST")
    fun <T> getInstances(type: Class<T>) =  m[type].orEmpty().asSequence().toList() as List<T>

}