package sp.it.util.access

import sp.it.util.access.fieldvalue.EnumerableValue
import sp.it.util.type.InstanceMap
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * [V] restricted to finite number of specific values that can be enumerated.
 *
 * @param <T> type of the value
 */
open class VarEnum<T>: V<T>, EnumerableValue<T> {

    private val valueEnumerator: () -> Collection<T>

    // TODO: turn into factory
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

        /**
         * @param value initial value
         * @param enumerator time sensitive value range supplier
         * @return [VarEnum] with the specified initial value and value range represented by the specified enumerator
         */
        fun <V> ofArray(value: V, enumerator: () -> Array<V>) = VarEnum(value, { enumerator().toList() })

        /**
         * @param value initial value
         * @param enumerator time sensitive value range supplier
         * @return [VarEnum] with the specified initial value and value range represented by the specified enumerator
         */
        fun <V> ofStream(value: V, enumerator: () -> Stream<V>) = VarEnum(value, { enumerator().toList() })

        /**
         * @param value initial value
         * @param enumerator time sensitive value range supplier
         * @return [VarEnum] with the specified initial value and value range represented by the specified enumerator
         */
        fun <V> ofSequence(value: V, enumerator: () -> Sequence<V>) = VarEnum(value, { enumerator().toList() })

        /**
         * Note on nullability:
         *
         * The specified generic type argument will be used as a key to get the value range. Nullability is respected,
         * thus if it is nullable, the value range given by the instance source will also contain null as a possible
         * value.
         *
         * @param value initial value
         * @param instanceSource instance source that maps values to types and which will be used to find the exact
         * value range, which is the association in the source for the type represented by the specified generic type
         * argument
         * @return [VarEnum] with the specified initial value and value range represented by the specified instance map for
         * the type represented by the specified generic type argument
         */
        inline fun <reified V> ofInstances(value: V, instanceSource: InstanceMap) = VarEnum(value, { instanceSource.getInstances<V>().toList() })

    }

}