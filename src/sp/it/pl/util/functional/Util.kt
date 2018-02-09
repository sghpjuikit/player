package sp.it.pl.util.functional

import javafx.collections.ObservableList
import sp.it.pl.util.type.union
import java.util.Comparator
import java.util.Optional
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.DoubleConsumer
import java.util.function.Function
import java.util.function.LongConsumer
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.coroutines.experimental.buildSequence
import kotlin.reflect.KClass

operator fun <T> Consumer<T>.invoke(t: T) = accept(t)

operator fun DoubleConsumer.invoke(t: Double) = accept(t)

operator fun LongConsumer.invoke(t: Long) = accept(t)

operator fun <T,U> BiConsumer<T,U>.invoke(t: T, u: U) = accept(t, u)

operator fun <T,U> Function<T,U>.invoke(t: T) = apply(t)

operator fun <T> Supplier<T>.invoke() = get()

operator fun Runnable.invoke() = run()

/** @return kotlin consumer that invokes java consumer */
fun <T> consumer(consumer: Consumer<T>): (T) -> Unit = { consumer(it) }

/** @return return value in the optional or null if empty */
fun <T> Optional<T>.orNull(): T? = orElse(null)

/** @return return value in the optional or null if empty */
fun <R,E> Try<R,E>.orNull(): R? = getOr(null)

/** @return return value in the optional or null if empty */
infix fun <R,E> Try<R,E>.orNull(onError: (E) -> Unit): R? = ifError(onError).getOr(null)

fun <R> runTry(block: () -> R): Try<R,Throwable> = Try.tryS(Supplier { block() }, Throwable::class.java)

infix fun <R,E> Try<R,E>.onE(handle: (E) -> Unit) = ifError(handle)!!

/** Invokes the block if this is true and returns this value. */
inline fun Boolean.ifTrue(block: (Boolean) -> Unit) = apply { if (this) block(this) }

/** Invokes the block if this is false and returns this value. */
inline fun Boolean.ifFalse(block: (Boolean) -> Unit) = apply { if (!this) block(this) }

/** @return return value from the first supplier that supplied non null or null if no such supplier */
fun <T> supplyFirst(vararg suppliers: () -> T?): T? = seqOf(*suppliers).map { it() }.find { it!=null }

/** @return a sequence of the specified values */
fun <T> seqOf(vararg elements: T) = sequenceOf(*elements)

/** @return lazy recursive sequence in depth-first order */
fun <E> E.seqRec(children: (E) -> Iterable<E>): Sequence<E> = buildSequence {
    yield(this@seqRec)
    children(this@seqRec).forEach { it.seqRec(children).forEach { yield(it) } }
    // eager version
    // sequenceOf(this) + children(this).asSequence().flatMap { it.seqRec(children) }
}

/** @return stream that yields elements of this stream sorted by value selected by specified [selector] function. */
inline fun <T, R : Comparable<R>> Stream<T>.sortedBy(crossinline selector: (T) -> R?) = sorted(compareBy(selector))!!

/** @return null-safe comparator wrapper putting nulls at the end */
fun <T> Comparator<T>.nullsLast(): Comparator<T?> = Comparator.nullsLast(this) as Comparator<T?>

/** @return null-safe comparator wrapper putting nulls at the the start */
fun <T> Comparator<T>.nullsFirst(): Comparator<T?> = Comparator.nullsFirst(this) as Comparator<T?>

fun <E: Any> Collection<E?>.getElementType(): Class<*> {
    return asSequence().filterNotNull()
            .map { it::class as KClass<*> }.distinct()
            .fold(null as KClass<*>?) { commonType, type -> commonType?.union(type) ?: type }
            ?.java ?: Void::class.java
}

inline infix fun <reified T: Any?> ObservableList<T>.clearSet(elements: Collection<T>) {
    setAll(*elements.toTypedArray())
}

inline infix fun <reified T: Any?> ObservableList<T>.clearSet(elements: Sequence<T>) = this clearSet elements.toList()