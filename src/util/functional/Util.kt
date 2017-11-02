package util.functional

import kotlin.coroutines.experimental.buildSequence


/** @return lazy recursive sequence of in depth-first order */
fun <E> E.seqRec(children : (E) -> Iterable<E>): Iterable<E> = buildSequence {
    yield(this@seqRec)
    children(this@seqRec).forEach { it.seqRec(children) }
}.asIterable()