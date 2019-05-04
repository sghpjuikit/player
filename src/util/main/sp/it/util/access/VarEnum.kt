package sp.it.util.access

/** [V] that is [EnumerableValue]. */
open class VarEnum<T>: V<T>, EnumerableValue<T> {

    private val valueEnumerator: () -> Collection<T>

    constructor(vararg enumeration: T): super(enumeration.first()) {
        valueEnumerator = { enumeration.toList() }
    }

    constructor(value: T, enumerated: Collection<T>): super(valOrFirst(value, { enumerated })) {
        valueEnumerator = { enumerated }
    }

    constructor(value: T, enumerator: () -> Collection<T>): super(valOrFirst(value, enumerator)) {
        valueEnumerator = enumerator
    }

    override fun enumerateValues() = valueEnumerator()

    companion object {

        @Suppress("UNUSED_PARAMETER")
        private fun <T> valOrFirst(v: T, enumerator: () -> Collection<T>) = v

    }

}