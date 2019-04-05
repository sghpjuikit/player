@file:Suppress("EXTENSION_SHADOWED_BY_MEMBER")

package sp.it.pl.util.functional

import sp.it.pl.util.dev.fail
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

    /** @return the value if ok or the specified value if error */
    fun getOr(or: @UV R): R = when (this) {
        is Ok<R> -> value
        is Error<E> -> or
    }

    /** @return the value if ok or the value computed with specified supplier if error */
    inline fun getOrSupply(or: () -> @UV R): R = when (this) {
        is Ok<R> -> value
        is Error<E> -> or()
    }

    /** @return the value if ok or the value computed with specified supplier if error */
    inline fun getOrSupply(or: (E) -> @UV R): R = when (this) {
        is Ok<R> -> value
        is Error<E> -> or(value)
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
    fun ifOk(action: Consumer<in R>) = ifOk(action.kt)

    /** Legacy version of [ifOk] for Java taking a [Consumer]. */
    fun ifOkUse(action: Consumer<in R>) = ifOk(action.kt)

    /** Legacy version of [ifError] for Java taking a [Consumer]. */
    fun ifError(action: Consumer<in E>) = ifError(action.kt)

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

    fun and(and: Try<@UV R, @UV E>): Try<R, E> = when (this) {
        is Ok<R> -> when (and) {
            is Ok<R> -> this
            is Error<E> -> and
        }
        is Error<E> -> this
    }

    fun and(and: (R) -> Boolean, errorSupplier: (R) -> @UV E): Try<R, E> = when (this) {
        is Ok<R> -> if (and(value)) this else error(errorSupplier(value))
        is Error<E> -> this
    }

    fun and(and: (R) -> Try<*, @UV E>): Try<R, E> = when (this) {
        is Ok<R> -> when(val c = and(value)) {
            is Ok<*> -> this
            is Error<E> -> c
        }
        is Error<E> -> this
    }

    fun or(or: Try<@UV R, @UV E>): Try<R, E> = when (this) {
        is Ok<R> -> this
        is Error<E> -> when(or) {
            is Ok<R> -> or
            else -> this
        }
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
fun <R> runTry(block: () -> R): Try<R, Throwable> = try {
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
fun <R, E, R1: R, R2: R> Try<R,E>.getOr(or: @UV R2): R = when (this) {
    is Try.Ok<R> -> value
    is Try.Error<E> -> or
}
/** @return the value if ok or the value computed with specified supplier if error */
fun <R, E, R1: R, R2: R> Try<R,E>.getOrSupply(or: () -> @UV R2): R = when (this) {
    is Try.Ok<R> -> value
    is Try.Error<E> -> or()
}

/** @return the value if ok or the value computed with specified supplier if error */
fun <R, E, R1: R, R2: R> Try<R,E>.getOrSupply(or: (E) -> @UV R2): R = when (this) {
    is Try.Ok<R> -> value
    is Try.Error<E> -> or(value)
}