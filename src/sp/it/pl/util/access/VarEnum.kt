package sp.it.pl.util.access

import sp.it.pl.util.access.fieldvalue.EnumerableValue
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.type.InstanceMap
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Accessor which can enumerate all its possible values - implementing [EnumerableValue].
 *
 * @param <T> type of the value
 */
open class VarEnum<T>: V<T>, EnumerableValue<T> {

    private val valueEnumerator: () -> Collection<T>

    constructor(vararg enumeration: T): super(enumeration[0]) {
        valueEnumerator = { enumeration.toList() }
    }

    constructor(value: T, enumerated: Collection<T>): super(value) {
        valueEnumerator = { enumerated }
    }

    @SafeVarargs
    constructor(value: T, enumerator: () -> Collection<T>, vararg appliers: Consumer<in T>): super(value, Consumer { v -> appliers.forEach { it(v) } }) {
        valueEnumerator = enumerator
    }

    override fun enumerateValues() = valueEnumerator()

    companion object {

        @JvmStatic fun <V> ofArray(value: V, enumerator: () -> Array<V>) =
                VarEnum(value, enumerator = { enumerator().toList() })

        @JvmStatic fun <V> ofStream(value: V, enumerator: () -> Stream<V>) =
                VarEnum(value, enumerator = { enumerator().toList() })

        @JvmStatic fun <V> ofStream(value: V, enumerator: () -> Stream<V>, vararg appliers: Consumer<in V>) =
                VarEnum(value, enumerator = { enumerator().toList() }, appliers = *appliers)

        @JvmStatic fun <V> ofSequence(value: V, enumerator: () -> Sequence<V>, vararg appliers: (V) -> Unit) =
                VarEnum(value, enumerator = { enumerator().toList() }, appliers = *appliers.map { a -> Consumer<V> { a(it) } }.toTypedArray())

        @JvmStatic fun <V> ofInstances(value: V, type: Class<V>, instanceSource: InstanceMap) =
                VarEnum(value, enumerator = { instanceSource.getInstances(type) })
    }

}