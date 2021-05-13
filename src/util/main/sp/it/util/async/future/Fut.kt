package sp.it.util.async.future

import javafx.util.Duration
import mu.KLogging
import sp.it.util.async.FX
import sp.it.util.async.IO
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.async.sleep
import sp.it.util.dev.Blocks
import sp.it.util.functional.Try
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.invoke
import sp.it.util.functional.kt
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

   /** @return future that waits for this to complete normally, invokes the specified block and returns its result */
   fun <R> then(executor: Executor = defaultExecutor, block: (T) -> R) = Fut<R>(f.thenApplyAsync(block.logging(), executor.kt))

   /** [use] which sleeps the specified duration on [IO]. */
   fun thenWait(time: Duration) = use(IO) { sleep(time) }

   /** [then] with [FX] executor. Intended for simple and declarative use of asynchronous computation from ui. */
   infix fun <R> ui(block: (T) -> R) = then(FX, block)

   /** [then] which returns the original value. Intended for (blocking) side-effects. */
   fun use(executor: Executor = defaultExecutor, block: (T) -> Unit) = then(executor) { block(it); it }

   /** Legacy version of [use] for Java taking a [Consumer]. */
   fun useBy(executor: Executor = defaultExecutor, block: Consumer<in T>) = then(executor) { block.invoke(it); it }

   /** Sets [block] to be invoked when this future finishes with success. Returns this. */
   fun onOk(executor: Executor = defaultExecutor, block: (T) -> Unit) = onDone(executor) { it.toTry().ifOk(block) }

   /** Sets [block] to be invoked when this future finishes with error. Returns this. */
   fun onError(executor: Executor = defaultExecutor, block: (Throwable) -> Unit) = onDone(executor) { it.toTry().ifError(block) }

   /** Sets [block] to be invoked when this future finishes regardless of success. Returns this. */
   fun onDone(executor: Executor = defaultExecutor, block: (Result<T>) -> Unit) = apply {
      f.handleAsync({ _, _ -> block.logging()(getDone()) }, executor.kt)
   }

   /** @return whether this future completed regardless of success */
   fun isDone(): Boolean = f.isDone

   /** Invokes the block if this future [isDone] and [getDone] is [ResultOk] */
   fun ifDoneOk(block: (T) -> Unit) {
      if (isDone()) getDone().toTry().ifOk(block)
   }

   /** Blocks current thread until [isDone] and returns the result. */
   @Blocks
   fun getDone(): Result<T> = try {
      ResultOk(f.get())
   } catch (e: InterruptedException) {
      ResultInterrupted(e)
   } catch (e: ExecutionException) {
      ResultFail(e)
   }

   /** Blocks current thread until [isDone]. Returns this. */
   @Blocks
   fun block() = apply { getDone() }

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

   sealed interface Result<out T> {

      data class ResultOk<T>(val value: T): Result<T>

      data class ResultInterrupted(val error: InterruptedException): Result<Nothing>

      data class ResultFail(val error: ExecutionException): Result<Nothing> {
         constructor(error: Throwable): this(error as? ExecutionException ?: ExecutionException("", error))
      }

      fun toTry(): Try<T, Exception> = when (this) {
         is ResultOk<T> -> Try.ok(value)
         is ResultInterrupted -> Try.error(error)
         is ResultFail -> Try.error(error)
      }

      fun or(block: (Exception) -> @UnsafeVariance T) = toTry().getOrSupply(block)

   }

}