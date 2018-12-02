package sp.it.pl.util.async.future

import javafx.scene.control.ProgressIndicator
import javafx.util.Duration
import mu.KLogging
import sp.it.pl.main.APP
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.NEW
import sp.it.pl.util.async.sleep
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.functional.kt
import sp.it.pl.util.functional.toUnit
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Future monad implementation.
 *
 * Represents time sensitive value, enriched with the way it is computed. Lazy.
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
     * Sets progress to 0 on [FX], then waits for computation of this future and then
     * sets progress to 1, again on [FX].
     *
     * ```
     * fut()
     *   .then { computeX() }
     *   .then { computeY() }
     *   .showProgress()
     *   .then { computeZ() }
     * ```
     *
     * When chaining futures (like in the example above), the placement of this method determines
     * after which computation the progress reaches 1.
     *
     * The progress is 0 in any computation preceding this method and 1 in any following it. So to set the progress to
     * 1 at the end of computation, this method must be the last call of the chain.
     */
    fun showProgress(p: Optional<ProgressIndicator>): Fut<T> = p.map { showProgress(it) }.orElse(this)

    fun showProgress(p: ProgressIndicator?) = p?.let {
        Fut<T>(
                CompletableFuture
                        .runAsync({ p.progress = -1.0 }, FX.kt)
                        .thenComposeAsync { f }
                        .thenApplyAsync({ p.progress = 1.0; it }, FX.kt)
        )
    } ?: this

    /** Invokes [showProgress] using new progress indicator in the currently active window iff a window is active. */
    fun showProgressOnActiveWindow() = showProgress(APP.windowManager.active.map { it.taskAdd() })

    fun onOk(executor: Executor = defaultExecutor, block: (T) -> Unit) = onDone(executor) { it.ifOk(block) }

    fun onError(executor: Executor = defaultExecutor, block: (Throwable) -> Unit) = onDone(executor) { it.ifError(block) }

    fun onDone(executor: Executor = defaultExecutor, block: (Try<T,Throwable>) -> Unit): Unit = run {
        f.handleAsync(
                { t, e ->  block(if (e==null) Try.ok(t) else Try.error(e)) },
                executor.kt
        )
    }

    fun isDone(): Boolean = f.isDone

    // TODO improve using sealed class
    fun getDone(): Try<T,Exception> = try {
            Try.ok(f.get())
        } catch (e: InterruptedException) {
            logger.error(e) { "Asynchronous computation was interrupted" }
            Try.error(e)
        } catch (e: ExecutionException) {
            logger.error(e) { "Asynchronous computation encountered a problem" }
            Try.error(e)
        }

    // TODO improve using sealed class
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