package util.functional

import java.util.*
import kotlin.coroutines.experimental.buildSequence


/** @return lazy recursive sequence of in depth-first order */
fun <E> E.seqRec(children : (E) -> Iterable<E>): Iterable<E> = buildSequence {
    yield(this@seqRec)
    children(this@seqRec).forEach { it.seqRec(children) }
}.asIterable()

/** @return return value in the optional or null if empty */
fun <T> Optional<T>.orNull(): T? = orElse(null)

/** @return return value in the optional or null if empty */
fun <R,E> Try<R,E>.orNull(): R? = getOr(null)

/** @return return value from the first supplier that supplied non null or null if no such supplier */
fun <T> supplyFirst(vararg suppliers: () -> T?): T? = sequenceOf(*suppliers).map { it() }.find { it!=null }