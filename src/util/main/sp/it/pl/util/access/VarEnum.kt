package sp.it.pl.util.access

import sp.it.pl.util.access.fieldvalue.EnumerableValue
import sp.it.pl.util.type.InstanceMap
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Accessor which can enumerate all its possible values - implementing [EnumerableValue].
 *
 * @param <T> type of the value
 */
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
        private fun <T> valOrFirst(v: T, enumerator: () -> Collection<T>) = v // if (v in enumerator()) v else enumerator().first()   // TODO: enable

        @JvmStatic fun <V> ofArray(value: V, enumerator: () -> Array<V>) =
                VarEnum(value, enumerator = { enumerator().toList() })

        @JvmStatic fun <V> ofStream(value: V, enumerator: () -> Stream<V>) =
                VarEnum(value, enumerator = { enumerator().toList() })

        fun <V> ofSequence(value: V, enumerator: () -> Sequence<V>) = VarEnum(value, enumerator = { enumerator().toList() })

        fun <V> ofInstances(value: V, type: Class<V>, instanceSource: InstanceMap) =
                VarEnum(value, enumerator = { instanceSource.getInstances(type) })

        inline fun <reified V> ofInstances(value: V, instanceSource: InstanceMap) = ofInstances(value, V::class.java, instanceSource)
    }

}