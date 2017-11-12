package util.functional

import java.util.*
import kotlin.coroutines.experimental.buildSequence


/** @return lazy recursive sequence of in depth-first order */
fun <E> E.seqRec(children : (E) -> Iterable<E>): Iterable<E> = buildSequence {
    yield(this@seqRec)
    children(this@seqRec).forEach { it.seqRec(children) }
}.asIterable()

/** @return return value in the optional or null if empty */
fun <T> Optional<T>.orNull() = orElse(null)!!