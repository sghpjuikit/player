@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package sp.it.util.functional

import sp.it.util.dev.fail
import java.util.function.Consumer
import kotlin.UnsafeVariance as UV

/**
 * Try monad for functional error handling.
 *
 * @param <R> success return value
 * @param <E> error return value
 */
sealed class Try<out R, out E> {

   /** @return true iff this is [Ok] */
   abstract val isOk: Boolean

   /** @return true iff this is [Error] */
   abstract val isError: Boolean

   /** @return the value if ok or throw an exception if error */
   val orThrow: R
      get() = when (this) {
         is Ok<R> -> value
         is Error<E> -> fail { "Can not get result of an Error Try" }
      }

   /** Invoke the specified action if success */
   inline fun ifOk(action: (R) -> Unit) = apply { if (this is Ok<R>) action(value) }

   /** Invoke the specified action if error */
   inline fun ifError(action: (E) -> Unit) = apply { if (this is Error<E>) action(value) }

   /** Invoke the specified action if success or the other specified action if error */
   inline fun ifAny(actionOk: (R) -> Unit, actionError: (E) -> Unit) = apply {
      when {
         this is Ok<R> -> actionOk(value)
         this is Error<E> -> actionError(value)
      }
   }

   /** Legacy version of [ifOk] for Java taking a [Consumer]. */
   fun ifOkUse(action: Consumer<in R>) = ifOk(action.kt)

   /** Legacy version of [ifError] for Java taking a [Consumer]. */
   fun ifErrorUse(action: Consumer<in E>) = ifError(action.kt)

   fun <S> map(mapper: (R) -> S): Try<S, E> = when (this) {
      is Ok<R> -> ok(mapper(value))
      is Error<E> -> this
   }

   fun <F> mapError(mapper: (E) -> F): Try<R, F> = when (this) {
      is Ok<R> -> this
      is Error<E> -> error(mapper(value))
   }

   fun <S, F> map(mapperOk: (R) -> S, mapperError: (E) -> F): Try<S, F> = when (this) {
      is Ok<R> -> ok(mapperOk(value))
      is Error<E> -> error(mapperError(value))
   }

   class Ok<R>(val value: R): Try<R, Nothing>() {
      override val isOk = true
      override val isError = false
   }

   class Error<E>(val value: E): Try<Nothing, E>() {
      override val isOk = false
      override val isError = true
   }

   companion object {

      fun ok() = Ok(null)

      fun <R> ok(value: R) = Ok(value)

      fun error() = Error(null)

      fun <E> error(value: E) = Error(value)

   }

   object Java {

      /** Legacy version of [Try.ok] for Java to avoid cast. */
      @JvmStatic fun <R, E> ok() = Ok(null) as Try<R?, E>

      /** Legacy version of [Try.ok] for Java to avoid cast. */
      @JvmStatic fun <R, E> ok(value: R) = Ok(value) as Try<R, E>

      /** Legacy version of [Try.error] for Java to avoid cast. */
      @JvmStatic fun <R, E> error() = Error(null) as Try<R, E?>

      /** Legacy version of [Try.error] for Java to avoid cast. */
      @JvmStatic fun <R, E> error(value: E) = Error(value) as Try<R, E>

   }
}

/**
 * Similar to [kotlin.runCatching]: runs the specified block, catches all non fatal exceptions and wraps the result.
 *
 * Fatal exceptions are rethrown. They are: [VirtualMachineError], [ThreadDeath], [InterruptedException], [LinkageError].
 *
 * @return the specified block's return value or any caught exception.
 */
inline fun <R> runTry(block: () -> R): Try<R, Throwable> = try {
   Try.ok(block())
} catch (e: Throwable) {
   if (e is VirtualMachineError || e is ThreadDeath || e is InterruptedException || e is LinkageError) throw e
   Try.error(e)
}

/** @return the success value if success or the error value if error */
fun <T, R: T, E: T> Try<R, E>.getAny(): T = when (this) {
   is Try.Ok<R> -> value
   is Try.Error<E> -> value
}

/** @return the value if ok or the specified value if error */
fun <R, E, R1: R, R2: R> Try<R1, E>.getOr(or: @UV R2): R = when (this) {
   is Try.Ok<R1> -> value
   is Try.Error<E> -> or
}

/** @return the value if ok or the value computed with specified supplier if error */
inline fun <R, E, R1: R, R2: R> Try<R1, E>.getOrSupply(or: (E) -> @UV R2): R = when (this) {
   is Try.Ok<R1> -> value
   is Try.Error<E> -> or(value)
}

/**
 * Applies short-circuit boolean && operation.
 * Returns Ok if both Try are Ok, otherwise Error.
 * Hence, the specified Try is only considered if this is Ok. Note how its OK parameter is never used.
 * This operation is not commutative, these: a [Try.and] b, b [Try.and] a are not same, however these are: a [Try.and] b, b [Try.andAlso] a.
 *
 * @return this if error or if both ok otherwise the specified Try (which will be known to be Error at that point)
 */
infix fun <R, E, E1: E, E2: E> Try<R, E1>.and(and: Try<*, @UV E2>): Try<R, E> = when (this) {
   is Try.Ok<R> -> when (and) {
      is Try.Ok<*> -> this
      is Try.Error<E2> -> and
   }
   is Try.Error<E1> -> this
}

/** Lazy [Try.and]. */
inline fun <R, E, E1: E, E2: E> Try<R, E1>.and(and: (R) -> Try<*, @UV E2>): Try<R, E> = when (this) {
   is Try.Ok<R> -> when (val c = and(value)) {
      is Try.Ok<*> -> this
      is Try.Error<E2> -> c
   }
   is Try.Error<E1> -> this
}

/**
 * Applies short-circuit boolean && operation.
 * Returns Ok if both Try are Ok, otherwise Error.
 * Hence, the specified predicate is only invoked if this is Ok. Note how this' OK parameter is never used.
 * This operation is not commutative, these: a [Try.andAlso] b, b [Try.andAlso] a are not same, however these are: a [Try.andAlso] b, b [Try.and] a.
 *
 * @return this if error otherwise the specified Try
 */
fun <E, R1, R2, E1: E, E2: E> Try<R1, E1>.andAlso(and: Try<R2, @UV E2>): Try<R2, E> = when (this) {
   is Try.Ok<R1> -> and
   is Try.Error<E1> -> this
}

/** Lazy [Try.andAlso]. */
inline fun <E, R1, R2, E1: E, E2: E> Try<R1, E1>.andAlso(and: (R1) -> Try<R2, @UV E2>): Try<R2, E> = when (this) {
   is Try.Ok<R1> -> and(value)
   is Try.Error<E1> -> this
}

/**
 * Applies short-circuit boolean || operation.
 * Returns Error if both Try are Error, otherwise Ok.
 * Hence, the specified Try is only considered if this is Error. Note how its ERROR parameter is never used.
 * This operation is not commutative, these: a [Try.or] b, b [Try.or] a are not same, however these are: a [Try.or] b, b [Try.orAlso] a.
 *
 * @return this if ok or if both ok otherwise the specified Try (which will be known to be Ok at that point)
 */
infix fun <R, E, R1: R, R2: R> Try<R1, E>.or(or: Try<@UV R2, *>): Try<R, E> = when (this) {
   is Try.Ok<R1> -> this
   is Try.Error<E> -> when (or) {
      is Try.Ok<R2> -> or
      else -> this
   }
}

/** Lazy [Try.or]. */
inline fun <R, E, R1: R, R2: R> Try<R1, E>.or(or: (E) -> Try<@UV R2, *>): Try<R, E> = when (this) {
   is Try.Ok<R1> -> this
   is Try.Error<E> -> when (val c = or(value)) {
      is Try.Ok<R2> -> c
      else -> this
   }
}

/**
 * Applies short-circuit boolean || operation.
 * Returns Error if both Try are Error, otherwise Ok.
 * The specified Try is only considered if this is Error. Note how this' ERROR parameter is never used.
 * This operation is not commutative, these: a [Try.orAlso] b, b [Try.orAlso] a are not same, however these are: a [Try.orAlso] b, b [Try.or] a.
 *
 * @return this if ok otherwise the specified Try
 */
fun <R, E, R1: R, R2: R, E1: E, E2: E> Try<R1, E1>.orAlso(or: Try<R2, E2>): Try<R, E2> = when (this) {
   is Try.Ok<R1> -> this
   is Try.Error<E1> -> or
}

/** Lazy [Try.orAlso]. */
inline fun <R, E, R1: R, R2: R, E1: E, E2: E> Try<R1, E1>.orAlso(or: (E) -> Try<R2, E2>): Try<R, E2> = when (this) {
   is Try.Ok<R1> -> this
   is Try.Error<E1> -> or(value)
}
