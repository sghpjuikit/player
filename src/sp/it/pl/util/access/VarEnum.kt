package sp.it.pl.util.access

import sp.it.pl.util.access.fieldvalue.EnumerableValue
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

    @JvmOverloads
    constructor(value: T, enumerated: Collection<T>, applier: Consumer<in T> = Consumer {}): super(value, applier) {
        valueEnumerator = { enumerated }
    }

    @JvmOverloads
    constructor(value: T, enumerator: () -> Collection<T>, applier: Consumer<in T> = Consumer {}): super(value, applier) {
        valueEnumerator = enumerator
    }

    override fun enumerateValues() = valueEnumerator()

    companion object {

        @JvmStatic fun <V> ofArray(value: V, enumerator: () -> Array<V>) =
                VarEnum(value, enumerator = { enumerator().toList() })

        @JvmStatic fun <V> ofStream(value: V, enumerator: () -> Stream<V>) =
                VarEnum(value, enumerator = { enumerator().toList() })

        @JvmStatic fun <V> ofStream(value: V, enumerator: () -> Stream<V>, applier: Consumer<in V>) =
                VarEnum(value, enumerator = { enumerator().toList() }, applier = applier)

        @JvmStatic fun <V> ofSequence(value: V, enumerator: () -> Sequence<V>, applier: (V) -> Unit) =
                VarEnum(value, enumerator = { enumerator().toList() }, applier = Consumer { applier(it) })

        @JvmStatic fun <V> ofInstances(value: V, type: Class<V>, instanceSource: InstanceMap) =
                VarEnum(value, enumerator = { instanceSource.getInstances(type) })

        inline fun <reified V> ofInstances(value: V, instanceSource: InstanceMap) = Companion.ofInstances(value, V::class.java, instanceSource)
    }

}