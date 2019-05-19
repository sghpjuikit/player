@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package sp.it.util.functional

import sp.it.util.dev.fail
import java.util.function.Consumer
import java.util.function.Supplier
import kotlin.UnsafeVariance as UV

/**
 * Try monad for functional error handling.
 *
 * @param <R> success return value
 * @param <E> error return value
 */
sealed class Try<out R,out E> {

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

        /** Legacy version of [runTry] for Java, catching only specified exceptions types. */
        @JvmStatic fun tryR(f: Runnable, ecs: Iterable<Class<*>>): Try<Nothing?, Throwable> {
            try {
                f.run()
                return Try.ok()
            } catch (e: Exception) {
                for (ec in ecs)
                    if (ec.isInstance(e))
                        return error(e)
                throw RuntimeException("Unhandled exception thrown in Try operation", e)
            }
        }

        /** Legacy version of [runTry] for Java, catching only specified exceptions types. */
        @JvmStatic fun tryR(f: Runnable, vararg ecs: Class<*>): Try<Nothing?, Throwable> {
            try {
                f.run()
                return Try.ok()
            } catch (e: Throwable) {
                for (ec in ecs)
                    if (ec.isInstance(e))
                        return Try.error(e)
                throw RuntimeException("Unhandled exception thrown in Try operation", e)
            }
        }

        /** Legacy version of [runTry] for Java, catching only specified exceptions types. */
        @JvmStatic fun <O> tryS(f: Supplier<out O>, ecs: Iterable<Class<*>>): Try<O, Throwable> {
            try {
                return Try.ok(f.get())
            } catch (e: Exception) {
                for (ec in ecs)
                    if (ec.isInstance(e))
                        return Try.error(e)
                throw RuntimeException("Unhandled exception thrown in Try operation", e)
            }
        }

        /** Legacy version of [runTry] for Java, catching only specified exceptions types. */
        @JvmStatic fun <O> tryS(f: Supplier<out O>, vararg ecs: Class<*>): Try<O, Throwable> {
            try {
                return Try.ok(f.get())
            } catch (e: Throwable) {
                for (ec in ecs)
                    if (ec.isInstance(e))
                        return Try.error(e)
                throw RuntimeException("Unhandled exception thrown in Try operation", e)
            }
        }

    }
}

/**
 * Same as [kotlin.runCatching], runs the specified block, catches all exceptions and wraps the result.
 *
 * @return the specified block's return value or any caught exception.
 */
inline fun <R> runTry(block: () -> R): Try<R, Throwable> = try {
    Try.ok(block())
} catch (e: Throwable) {
    Try.error(e)
}

/** @return the success value if success or the error value if error */
fun <T, R: T, E: T> Try<R,E>.getAny(): T = when(this) {
    is Try.Ok<R> -> value
    is Try.Error<E> -> value
}
/** @return the value if ok or the specified value if error */
fun <R, E, R1: R, R2: R> Try<R1,E>.getOr(or: @UV R2): R = when (this) {
    is Try.Ok<R1> -> value
    is Try.Error<E> -> or
}

/** @return the value if ok or the value computed with specified supplier if error */
inline fun <R, E, R1: R, R2: R> Try<R1,E>.getOrSupply(or: (E) -> @UV R2): R = when (this) {
    is Try.Ok<R1> -> value
    is Try.Error<E> -> or(value)
}

/**
 * Applies short-circuit boolean && operation.
 * Returns Ok if both Try are Ok, otherwise first Error, in order: this, the specified Try.
 * Hence, the specified Try is only considered if this is Ok. Note how its OK parameter is never used.
 *
 * @return this if error or if both ok otherwise the specified Try (which will be known to be Error at that point)
 */
infix fun <R,E, E1: E, E2: E> Try<R,E1>.and(and: Try<*, @UV E2>): Try<R, E> = when (this) {
    is Try.Ok<R> -> when (and) {
        is Try.Ok<*> -> this
        is Try.Error<E2> -> and
    }
    is Try.Error<E1> -> this
}

/**
 * Applies short-circuit boolean && operation.
 * Returns Ok if both this and the predicate result are Ok/true, otherwise first Error, in order: this, the specified supplier.
 * Hence, the specified predicate is only invoked if this is Ok and the error supplier only if the test returns false.
 *
 * @return this if error or if both this and the predicate are ok/true otherwise the supplied error
 */
inline fun <R,E, E1: E, E2: E> Try<R,E1>.and(and: (R) -> Boolean, errorSupplier: (R) -> @UV E2): Try<R, E> = when (this) {
    is Try.Ok<R> -> if (and(value)) this else Try.error(errorSupplier(value))
    is Try.Error<E1> -> this
}

/** Lazy [Try.and]. */
inline fun <R,E, E1: E, E2: E> Try<R,E1>.and(and: (R) -> Try<*, @UV E2>): Try<R, E> = when (this) {
    is Try.Ok<R> -> when(val c = and(value)) {
        is Try.Ok<*> -> this
        is Try.Error<E2> -> c
    }
    is Try.Error<E1> -> this
}

/**
 * Applies short-circuit boolean || operation.
 * Returns Error if both Try are Error, otherwise first Ok, in order: this, the specified Try.
 * Hence, the specified Try is only considered if this is Error. Note how its ERROR parameter is never used.
 *
 * @return this if ok or if both ok otherwise the specified Try (which will be known to be Ok at that point)
 */
infix fun <R, E, R1: R, R2: R> Try<R1,E>.or(or: Try<@UV R2, *>): Try<R, E> = when (this) {
    is Try.Ok<R1> -> this
    is Try.Error<E> -> when(or) {
        is Try.Ok<R2> -> or
        else -> this
    }
}

/** Lazy [Try.or]. */
inline fun <R, E, R1: R, R2: R> Try<R1,E>.or(or: (E) -> Try<@UV R2, *>): Try<R, E> = when (this) {
    is Try.Ok<R1> -> this
    is Try.Error<E> -> when(val c = or(value)) {
        is Try.Ok<R2> -> c
        else -> this
    }
}