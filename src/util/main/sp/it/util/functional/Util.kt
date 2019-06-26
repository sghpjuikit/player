package sp.it.util.functional

import javafx.util.Callback
import sp.it.util.async.executor.EventReducer
import sp.it.util.dev.Experimental
import java.util.Comparator
import java.util.Optional
import java.util.concurrent.Executor
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.DoubleConsumer
import java.util.function.Function
import java.util.function.LongConsumer
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Stream
import kotlin.streams.toList

val Executor.kt: (Runnable) -> Unit get() = this::execute

val Runnable.kt: () -> Unit get() = this::run

val <T> Consumer<T>.kt: (T) -> Unit get() = this::accept

val <T> Supplier<T>.kt: () -> T get() = this::get

operator fun <T> Consumer<T>.invoke(t: T) = accept(t)

operator fun DoubleConsumer.invoke(t: Double) = accept(t)

operator fun LongConsumer.invoke(t: Long) = accept(t)

operator fun <T, U> BiConsumer<T, U>.invoke(t: T, u: U) = accept(t, u)

operator fun <T, U> Function<T, U>.invoke(t: T) = apply(t)

operator fun <T, U> Callback<T, U>.invoke(t: T): U = call(t)

operator fun <T> Predicate<T>.invoke(t: T) = test(t)

operator fun <T> Supplier<T>.invoke() = get()

operator fun Runnable.invoke() = run()

operator fun EventReducer<Void>.invoke() = push(null)

/** @return [Unit] effectively ignoring this value */
fun Any.toUnit() = Unit

/** @return result of function composition `this * then` */
infix fun <A, B, C> ((A) -> B).compose(then: (B) -> C): (A) -> C = { then(this(it)) }

/** @return partially applied this with the 1st parameter fixed to the specified value */
fun <A, B, C> ((A, B) -> C).invoke(a: A): (B) -> C = { this(a, it) }

/** @return kotlin consumer that invokes java consumer */
fun <T> consumer(consumer: Consumer<T>): (T) -> Unit = { consumer(it) }

/** @return kotlin runnable that invokes java runnable */
fun <T> runnable(runnable: Runnable): () -> Unit = { runnable() }

/** @return value if it has been initialized or null otherwise */
fun <T> Lazy<T>.orNull() = if (isInitialized()) value else null

/** @return value or null if empty (if the value is nullable, this destroys the information of null's origin) */
fun <T> Optional<T>.orNull(): T? = orElse(null)

/** @return ok with the value of this optional or error if this is empty optional */
fun <T: Any> Optional<T>.toTry(): Try<T, Nothing?> = map { Try.ok(it) }.orNull() ?: Try.error()

/** Equivalent to [Optional.ofNullable]. */
fun <T: Any> T?.toOptional(): Optional<T> = Optional.ofNullable(this)

/** @return this result represented as a [Try] */
fun <T> Result<T>.toTry(): Try<T, Throwable> = fold({ Try.ok(it) }, { Try.error(it) })

/** @return value or null if error (if the value is nullable, this destroys the information of null's origin) */
fun <R, E> Try<R, E>.orNull(): R? = getOr(null)

/** @return value or null if empty (if the value is nullable, this destroys the information of null's origin) */
infix fun <R, E> Try<R, E>.orNull(onError: (E) -> Unit): R? = ifError(onError).getOr(null)

/**
 * Run the specified block if the condition is true
 * @return the result or null (if the value is nullable, this destroys the information of null's origin)
 */
fun <R> runIf(condition: Boolean, block: () -> R): R? = if (condition) block() else null

/**
 * Run the specified block if the condition is false
 * @return null or the result (if the value is nullable, this destroys the information of null's origin)
 */
fun <R> runUnless(condition: Boolean, block: () -> R): R? = if (condition) block() else null

/** @return specified supplier if test is true or null otherwise */
fun <T> supplyIf(test: Boolean, block: () -> T): (() -> T)? = if (test) block else null

/** @return specified supplier if test is false or null otherwise */
fun <T> supplyUnless(testNegated: Boolean, block: () -> T): (() -> T)? = supplyIf(!testNegated, block)

/**
 * Type-safe [let] with non null -> non null transformation and safe null propagation.
 *
 * The difference from [let] is in semantics.
 *
 * Consider mapping over value in the following nullability models.
 *
 * 1 sealed class Maybe = None | Some(T: Any?). This functional monadic approach modeling `emptiness` remains orthogonal
 * to nullability - nothing prevents Some containing a nullable value, i.e., Some(null). This is in fact a necessity,
 * once we realize mapping Some(T) always results in Some(R) and never None, even if the mapping returns a null. True,
 * this problem can be avoided using flatMap() instead of map(). But this forces the mapper to be aware of its context
 *  and brings it inside the monadic boundaries, which is what we wanted to avoid in the first place - nullability is
 *  no longer transparent. We can either wrap the mapping result R into Maybe(R) in the mapper lambda or adjust
 *  this in signature of some unrelated method. The latter introduces unwanted and fragile dependencies. The former
 *  involves boilerplate and internal inconsistency. If returning nullable result requires us to always use Maybe(R),
 *  why returning non-null result does not require use of Some(R). Further, if mapper changes signature from non null
 *  to nullable result, we introduce a bug the monad can not catch.
 * The model operates on:
 * * None -> None
 * * Some(T) -> Some(R)
 * The model's map() allows:
 * * None -> None         yes
 * * Some(T) -> Some(R)   yes
 * * Some(T) -> None      no (and pointless, but can be achieved using flatMap())
 * * None -> Some(R)      no
 * This model is imperfect for capturing nullability, because it requires entire system to use it (which brings
 * considerable readability/maintenance/performance overhead) or otherwise suffer from nullability-emptiness mismatch at
 * the boundary. It is however suitable for capturing emptiness alone.
 *
 * 2 [Optional]. Optional differs in that it tries to model nullability itself and thus forbids Some(null).
 * The analogy with Maybe is that None == Optional.empty() and Some(T: Any) = Optional.of(T).
 * Optional betrays monadic model with the idea of null propagation. If the mapping results in null, empty optional is
 * returned. Empty optional (i.e. null) can be considered a terminal value sink, and [Optional.map] a chain a reduction,
 * which reduces into a non null value only if none of the mappers returned null.
 * This prevents further mapping in the chain and also makes the semantics of the empty optional ambiguous,
 * as its origin is lost. The benefit is cognitive simplicity in that mapping never has to account for null input - the
 * idea of null propagation makes sense when dealing with nullability (as opposed to dealing with 'emptiness').
 * While in monadic model map() and flatMap() have different semantics, in this model map() has semantics of monadic
 * flatMap() and flatMap() itself is merely convenient overload.
 * The model operates on:
 * * None -> None
 * * Some(T) -> Maybe(R)   (source of non-determinism)
 * The model's map() allows:
 * * None -> Some(R)      no
 * * None -> None         yes
 * * Some(T) -> Some(R)   yes (but not deterministic, only if mapping does not return null)
 * * Some(T) -> None      yes (but not deterministic, only if mapping returns null)
 * This model is inconsistent.
 *
 * 3 [let] instead delegates nullability responsibility to type system.
 * The analogy with Maybe is that None == null and Some(T: Any) = T and Maybe(T: Any?) = T?.
 * This allows flexible tuning of the exact mapping behavior at call site, using the optional ?.let invocations.
 * The model operates on:
 * * Maybe(T) -> Maybe(R)
 * The model's map() allows:
 * * None -> None         yes (but not deterministic, only if mapping returns null)
 * * Some(T) -> Some(R)   yes (but not deterministic, only if mapping does not return null)
 * * Some(T) -> None      yes (but not deterministic, only if mapping returns null)
 * * None -> Some(R)      yes (but not deterministic, only if mapping does not return null)
 *
 * Unlike Optional model, the lack of determinism here is consistent (and recoverable), and again a consequence of
 * nullability being a broader concept than 'emptiness'. I.e., the monadic model's 'emptiness' semantics can still be
 * defined on top of it:
 * * None -> None         == ?: null or ?.let { it }
 * * None -> Some(R)      == ?: R!! or ?.let { when it==null -> R!! else -> it }
 * * Some(T) -> Some(R)   == .let { R!! }
 * * Some(T) -> None      == .let { null }
 *
 * First, mapping only on None (in chain) is conceptually pointless, None -> None leads to no gain and None -> Some(T)
 * is simply a recovery operation (almost always only at the tail of the chain), which ?: operator fulfills very well.
 *
 * Second, notice how .let {} expression does forbid operating on None, and consequently also on Maybe(T). In order
 * to allow that, ?.let {} is necessary. This means the mapper always knows whether to expect null or not, yet the fact
 * remains transparent to it. Also notice how ?.let {} unifies the nullability with monadic world. The correctness of
 * the monadic model is only guaranteed so long as we are within monadic bounds - never using the actual null!, however
 * here, the correctness is guaranteed at compile time, without ever breaking the illusion of being outside of the
 * monadic boundaries - they simply disappear. This is because the entire codebase is within them, something only
 * possible because of the zero overhead of nullable type system.
 *
 * Third, notice that ?.let {} carries the Optional's semantics of null propagation, but .let {} carries entirely new
 * semantics of null propagation
 * * In Optional model, the terminal null sink makes sure only non null mapping takes place. Developer hence must expect
 * that in mapping chain ...map().map().map()... not all mappers may be called. Every time the mapper returns Maybe(R),
 * we lose the ability to reason about the meaning of null, but retain the awareness than no null will ever be mapped.
 * * With monadic approach all mappers in the mapping chain are always called and always with the result of previous
 * mapper. The Some(T) and None branch are completely separate branches and only intersect with explicit use of
 * flatMap(). In the absence of null sink we retain the null semantics, but lose ability to distinguish non null
 * and nullable inputs (without breaking through the monadic boundary using flatMap()).
 * * With .let {}, all mappers are invoked, but safely. But the Some(T) branch and None branch are one and the same -
 * there is only maybe(T) branch. This makes null just like any other value and resembles Some(null) in monadic model.
 * We still lose null semantics however if two subsequent mappers return null.
 * * With ?.let {}, the model introduces null sink (i.e. None branch) as the mapper only executes on Some(T) branch.
 * This is equivalent to Optional model, we lose the null semantics.
 *
 * No matter, it seems [let] can not preserve null semantics. Only monadic model seems to get this right, but only at
 * the cost of making the mapping inputs ambiguous. The reason let is unable to restrict nullability of the result
 * of the mapping is because the relationship between nullable and non null types is asymmetric (one is subtype of
 * the other), single .let {} method can never fully restrict all intentions of the mapping return type - the type
 * system only allows return type Maybe(T).
 *
 * This carries a risk that a mapper that returned Some(R) is changed to return Maybe(R), which may cause change in
 * behavior by allowing the null result to be handled incorrectly by null handling mechanism intended for something
 * else - simply because the null has propagated. Sometimes this is intended behavior, but not always. Consider:
 * `nullable?.let { suddenlyReturnsNull() } ?: "5"`
 *
 * The behavior we desire is to not compile. What if we want let {} to return Some(N), i.e., forbid returning null to
 * preserve its semantics? For that we must be able to differentiate between null propagation strategies, in the same
 * way we already made mappers aware of nullability of their input at the compilation level using .let {} versus
 * ?.let {}. Because the input is contravariant, changing no-null.let {} into nullable.let {} is guaranteed to fail,
 * however the return type is covariant and as such requires separate method with specifically non null output
 * signature.
 *
 * This is such method.
 *
 * It retains the null sink, but unlike .let {} and ?.let {} never sinks into it by disallowing null mapping result. So
 * while let {} has semantics of a flatMap() in the monadic model, [net] has semantics of map() in monadic model, but
 * because now nullability and `emptiness` are one and the same, map(), which always stays on the Some(T) branch, can
 * not return null.
 *
 * The model now becomes:
 * * Maybe(T) -> Maybe(R)  ==  .let {}
 * * Some(T) -> Maybe(R)   ==  ?.let {}
 * * Some(T) -> Some(R)    ==  ?.net {}
 * * Maybe(T) -> Some(R)   ==  .net {}
 *
 */
inline fun <T, R: Any> T.net(block: (T) -> R): R = let(block)

@Suppress("FunctionName")
@Experimental("in trial period")
inline infix fun <T, R> T.let_(block: (T) -> R): R = let(block)

@Suppress("FunctionName")
@Experimental("in trial period")
inline infix fun <T> T.apply_(block: T.() -> Unit): T = apply(block)

/** @return this as instance of the specified type (equivalent to this as T) */
inline fun <reified T: Any> Any?.asIs(): T = this as T

/** @return this as instance of the specified type (equivalent to this as? T) */
inline fun <reified T: Any> Any?.asIf(): T? = this as? T

/** Invokes the block if this is the specified type. */
inline fun <reified T> Any?.ifIs(block: (T) -> Unit) = apply { if (this is T) block(this) }

/** Invokes the block if this is null and returns this value. */
inline fun <T> T?.ifNull(block: () -> Unit) = apply { if (this==null) block() }

/** Invokes the block if this is null and returns this value. */
inline fun <T> T?.ifNotNull(block: (T) -> Unit) = apply { if (this!=null) block(this) }

/** Invokes the block if this is true and returns this value. */
inline fun Boolean.ifTrue(block: () -> Unit) = apply { if (this) block() }

/** Invokes the block if this is false and returns this value. */
inline fun Boolean.ifFalse(block: () -> Unit) = apply { if (!this) block() }

/** @return lazy sequence yielded iteratively starting with this as first element until null element is reached */
fun <T: Any> T.traverse(next: (T) -> T?) = generateSequence(this, next)

/** @return lazy sequence yielded recursively in depth-first order starting with this as first element */
fun <T> T.recurse(children: (T) -> Iterable<T>): Sequence<T> = sequence {
    yield(this@recurse)
    children(this@recurse).forEach { it.recurse(children).forEach { yield(it) } }
}

/** @return an array containing all elements */
inline fun <reified E> Sequence<E>.asArray() = toList().toTypedArray()

/** @return an array containing all elements */
inline fun <reified E> Stream<E>.asArray() = toList().toTypedArray()

/** @return null-safe comparator wrapper putting nulls at the end */
fun <T> Comparator<T>.nullsLast(): Comparator<T?> = Comparator.nullsLast(this) as Comparator<T?>

/** @return null-safe comparator wrapper putting nulls at the the start */
fun <T> Comparator<T>.nullsFirst(): Comparator<T?> = Comparator.nullsFirst(this) as Comparator<T?>