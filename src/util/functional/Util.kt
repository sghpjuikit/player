package util.functional

import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.DoubleConsumer
import java.util.function.Function
import java.util.function.LongConsumer
import java.util.function.Supplier
import kotlin.coroutines.experimental.buildSequence

operator fun <T> Consumer<T>.invoke(t: T) = accept(t)

operator fun DoubleConsumer.invoke(t: Double) = accept(t)

operator fun LongConsumer.invoke(t: Long) = accept(t)

operator fun <T,U> BiConsumer<T,U>.invoke(t: T, u: U) = accept(t, u)

operator fun <T,U> Function<T,U>.invoke(t: T) = apply(t)

operator fun <T> Supplier<T>.invoke() = get()

operator fun Runnable.invoke() = run()

/** @return return value in the optional or null if empty */
fun <T> Optional<T>.orNull(): T? = orElse(null)

/** @return return value in the optional or null if empty */
fun <R,E> Try<R,E>.orNull(onError: (E) -> Unit = {}): R? = ifError(onError).getOr(null)

/** Invokes the block if this is true and returns this value. */
inline fun Boolean.ifTrue(block: (Boolean) -> Unit) = apply { if (this) block(this) }

/** Invokes the block if this is false and returns this value. */
inline fun Boolean.ifFalse(block: (Boolean) -> Unit) = apply { if (!this) block(this) }

/** @return return value from the first supplier that supplied non null or null if no such supplier */
fun <T> supplyFirst(vararg suppliers: () -> T?): T? = seqOf(*suppliers).map { it() }.find { it!=null }

/** @return a sequence of the specified values */
fun <T> seqOf(vararg elements: T) = sequenceOf(*elements)

/** @return lazy recursive sequence of in depth-first order */
fun <E> E.seqRec(children : (E) -> Iterable<E>): Iterable<E> = buildSequence {
    yield(this@seqRec)
    children(this@seqRec).forEach { it.seqRec(children) }
}.asIterable()
