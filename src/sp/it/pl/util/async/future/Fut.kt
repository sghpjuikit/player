package sp.it.pl.util.async.future

import javafx.scene.control.ProgressIndicator
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.NEW
import sp.it.pl.util.functional.kt
import sp.it.pl.util.async.sleep
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.functional.toUnit
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Future monad implementation.
 *
 * Oriented for practicality * ease of use, not specification (monadic laws) or robustness (API completeness).
 */
class Fut<T>(private var f: CompletableFuture<T>) {

    fun cancel(): Unit = f.cancel(true).toUnit()

    @JvmOverloads
    fun <R> then(executor: Executor = defaultExecutor, block: (T) -> R) = Fut<R>(f.thenApplyAsync(block.logging(), executor.kt))

    @JvmOverloads
    fun <R> then(executor: Executor = defaultExecutor, block: Fut<R>): Fut<R> = Fut(f.thenComposeAsync( { block.f }, executor.kt))

    /** [then] which sleeps the specified duration on [NEW]. */
    fun <R> thenWait(time: Duration) = then(NEW) { sleep(time) }

    /** [then] with [FX] executor. Intended for simple and declarative use of asynchronous computation from ui. */
    infix fun <R> ui(block: (T) -> R) = then(FX, block)

    /** [then] which returns the original value. Intended for side-effects. */
    fun use(executor: Executor = defaultExecutor, block: (T) -> Unit) = then(executor) { block(it); it }

    /** Legacy version of [use] for Java taking a [Consumer]. */
    @JvmOverloads
    fun useBy(executor: Executor = defaultExecutor, block: Consumer<in T>) = then(executor) { block(it); it }

    /**
     * Returns new future, which sets progress to 0 on fx thread, then executes
     * this future and then sets progress to 1, again on fx thread.
     *
     * Note that when chaining futures, the position within chain decides when
     * does the progress reach 1. It will not be at the end of the chain, but
     * at the position of this method in it. The progress is set to 0 always at
     * the beginning of the computation, i.e. the chain length or position of
     * this method within it does not have effect.
     *
     * To set the progress to 1 at the end of computation, this method must be
     * the last element of the chain.
     * To set the progress to 0 somewhere during the computation, a future for
     * the progress computation must created, this method called on it and
     * passed as Runnable into another future which executes it as
     * part of its computation. This will cause only that computation to be bound to
     * the progress.
     */
    fun showProgress(p: Optional<ProgressIndicator>): Fut<T> = p.map { showProgress(it) }.orElse(this)

    /** Invokes [showProgress] using new progress indicator in the currently active window iff a window is active. */
    fun showProgressOnActiveWindow() = showProgress(APP.windowManager.active.map { it.taskAdd() })

    fun showProgress(p: ProgressIndicator) = Fut<T>(
            CompletableFuture
                .runAsync({ p.progress = -1.0 }, FX.kt)
                .thenComposeAsync { f }
                .thenApplyAsync({ p.progress = 1.0; it }, FX.kt)
        )

    /**
     * @param condition test that if false, progress will not be displayed
     * @param p function that supplies nonnull progress indicator
     * @return this (fluent style)
     */
    fun showProgress(condition: Boolean, p: () -> ProgressIndicator) = if (condition) showProgress(p()) else this

    fun onOk(executor: Executor = defaultExecutor, block: (T) -> Unit) = onDone(executor) { it.ifOk(block) }

    fun onError(executor: Executor = defaultExecutor, block: (Throwable) -> Unit) = onDone(executor) { it.ifError(block) }

    fun onDone(executor: Executor = defaultExecutor, block: (Try<T,Throwable>) -> Unit): Unit = run {
        f.handleAsync(
                { t, e ->  block(if (e==null) Try.ok(t) else Try.error(e)) },
                executor.kt
        )
    }

    fun isDone(): Boolean = f.isDone

    fun getDone(): Try<T,Exception> = try {
            Try.ok(f.get())
        } catch (e: InterruptedException) {
            logger.error(e) { "Asynchronous computation was interrupted" }
            Try.error(e)
        } catch (e: ExecutionException) {
            logger.error(e) { "Asynchronous computation encountered a problem" }
            Try.error(e)
        }

    fun getDoneOrNull(): T? = if (f.isDone) {
            try {
                f.get()
            } catch (e: InterruptedException) {
                logger.error(e) { "Asynchronous computation was interrupted" }
                null
            } catch (e: ExecutionException) {
                logger.error(e) { "Asynchronous computation encountered a problem" }
                null
            }
        } else {
            null
        }

    companion object: KLogging() {

        /** @return future completed with [Unit]] */
        @JvmStatic
        fun fut() = fut(Unit)

        /** @return future completed with the specified value */
        @JvmStatic
        fun <T> fut(value: T) = Fut(CompletableFuture.completedFuture(value))

        /** @return future with promised value completed on the specified executor with the specified block */
        @JvmStatic
        @JvmOverloads
        fun <T> runFut(executor: Executor = defaultExecutor, block: () -> T) = fut(null).then(executor) { block() }

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

}