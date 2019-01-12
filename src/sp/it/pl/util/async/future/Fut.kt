package sp.it.pl.util.async.future

import javafx.util.Duration
import mu.KLogging
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.NEW
import sp.it.pl.util.async.future.Fut.Result.ResultFail
import sp.it.pl.util.async.future.Fut.Result.ResultInterrupted
import sp.it.pl.util.async.future.Fut.Result.ResultOk
import sp.it.pl.util.async.sleep
import sp.it.pl.util.dev.Blocks
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.functional.kt
import sp.it.pl.util.functional.toUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Future monad implementation.
 *
 * Represents time sensitive value, enriched with computation context. Lazy.
 *
 * Oriented for practicality and ease of use, not specification (monadic laws) or robustness (API completeness).
 */
class Fut<T>(private var f: CompletableFuture<T>) {

    // Document properly and make sure exceptions are handled properly in getDone
    fun cancel(): Unit = f.cancel(true).toUnit()

    /** @return future that waits for this to complete and then invokes the specified block and returns its result */
    @JvmOverloads
    fun <R> then(executor: Executor = defaultExecutor, block: (T) -> R) = Fut<R>(f.thenApplyAsync(block.logging(), executor.kt))

    /** @return future that waits for this to complete and then invokes the specified future and returns its result */
    @JvmOverloads
    fun <R> then(executor: Executor = defaultExecutor, block: Fut<R>): Fut<R> = Fut(f.thenComposeAsync( { block.f }, executor.kt))

    /** [use] which sleeps the specified duration on [NEW]. */
    fun thenWait(time: Duration) = use(NEW) { sleep(time) }

    /** [then] with [FX] executor. Intended for simple and declarative use of asynchronous computation from ui. */
    infix fun <R> ui(block: (T) -> R) = then(FX, block)

    /** [then] which returns the original value. Intended for (blocking) side-effects. */
    fun use(executor: Executor = defaultExecutor, block: (T) -> Unit) = then(executor) { block(it); it }

    /** Legacy version of [use] for Java taking a [Consumer]. */
    @JvmOverloads
    fun useBy(executor: Executor = defaultExecutor, block: Consumer<in T>) = then(executor) { block(it); it }

    /** Sets [block] to be invoked when this future finishes with success. Returns this. */
    fun onOk(executor: Executor = defaultExecutor, block: (T) -> Unit) = onDone(executor) { it.ifOk(block) }

    /** Sets [block] to be invoked when this future finishes with error. Returns this. */
    fun onError(executor: Executor = defaultExecutor, block: (Throwable) -> Unit) = onDone(executor) { it.ifError(block) }

    /** Sets [block] to be invoked when this future finishes regardless of success. Returns this. */
    fun onDone(executor: Executor = defaultExecutor, block: (Try<T,Throwable>) -> Unit) = apply {
        f.handleAsync(
                { t, e ->  block(if (e==null) Try.ok(t) else Try.error(e)) },
                executor.kt
        )
    }

    /** @return whether this future completed regardless of success */
    fun isDone(): Boolean = f.isDone

    /** Waits for this future to complete and return the result. */
    @Blocks
    fun getDone(): Result<T> = try {
            ResultOk(f.get())
        } catch (e: InterruptedException) {
            ResultInterrupted(e)
        } catch (e: ExecutionException) {
            ResultFail(e)
        }

    @Deprecated("for removal")
    fun getDoneOrNull(): T? = if (f.isDone) {
            getDone().let { it.asIf<ResultOk<T>>()?.value }
        } else {
            null
        }

    companion object: KLogging() {

        /** @return future completed with [Unit]] */
        @JvmStatic
        fun fut() = fut(Unit)

        /** @return future completed with the specified value */
        @JvmStatic
        fun <T> fut(value: T) = Fut<T>(CompletableFuture.completedFuture(value))

        private val defaultExecutor = CompletableFuture<Any>().defaultExecutor()!!

        private fun <T, R> ((T) -> R).logging(): ((T) -> R) = {
            try {
                this(it)
            } catch (t: Throwable) {
                logger.error(t) { "Unhandled exception" }
                throw t
            }
        }

    }

    sealed class Result<T> {
        data class ResultOk<T>(val value: T): Result<T>()
        data class ResultInterrupted<T>(val exception: InterruptedException): Result<T>()
        data class ResultFail<T>(val error: ExecutionException): Result<T>()

        fun or(block: () -> T): T = if (this is ResultOk) this.value else block.invoke()
    }

}
