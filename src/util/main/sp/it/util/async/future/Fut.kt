package sp.it.util.async.future

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.function.Consumer
import javafx.util.Duration
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import mu.KLogging
import sp.it.util.async.CURR
import sp.it.util.async.FX
import sp.it.util.async.IO
import sp.it.util.async.future.Fut.Result.ResultFail
import sp.it.util.async.future.Fut.Result.ResultInterrupted
import sp.it.util.async.future.Fut.Result.ResultOk
import sp.it.util.async.sleep
import sp.it.util.dev.Blocks
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.functional.getAny
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.invoke
import sp.it.util.functional.kt
import sp.it.util.functional.net
import sp.it.util.functional.runTry

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

   fun <R> thenFlat(executor: Executor = defaultExecutor, block: (T) -> Fut<R>) = Fut<R>(f.thenComposeAsync(block.logging().net { b -> { b(it).f } }, executor.kt))

   fun thenFlatten(executor: Executor = CURR): Fut<Any?> = thenFlat(executor) { it.asIf<Fut<Any?>>()?.thenFlatten(executor) ?: fut(it) }

   /** [use] which sleeps the specified duration on [IO]. */
   fun thenWait(time: Duration): Fut<T> = if (time.toMillis()==0.0) this else use(IO) { sleep(time) }

   /** [then] with [FX] executor. Intended for simple and declarative use of asynchronous computation from ui. */
   infix fun <R> ui(block: (T) -> R) = then(FX, block)

   /** [then] which returns the original value. Intended for (blocking) side effects. */
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
   fun isCancelled(): Boolean = f.isCancelled
   fun isOk(): Boolean = f.isDone && !f.isCompletedExceptionally
   fun isFailed(): Boolean = f.isCompletedExceptionally

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

   /** See [CompletionStage.asDeferred] */
   @Suppress("DeferredIsResult")
   fun asDeferred(): Deferred<T> = f.asDeferred()

   /** Blocks current thread until [isDone]. Returns this. */
   @Blocks
   fun block() = apply { getDone() }

   @Blocks
   fun blockAndGetOrThrow(): T = block().getDone().toTryRaw().orThrow

   companion object: KLogging() {

      /** @return future completed with [Unit]] */
      @JvmStatic
      fun fut() = fut(Unit)

      /** @return future completed successfully with the specified value */
      @JvmStatic
      fun <T> fut(value: T) = Fut<T>(CompletableFuture.completedFuture(value))

      /** @return future completed failed with the specified value */
      fun <T> futFailed(value: Throwable) = Fut<T>(CompletableFuture.failedFuture(value))

      /** @return future completed successfully or failed with the value supplied by the specified block */
      fun <T> futOfBlock(block: () -> T): Fut<T> = runTry { fut(block.logging()()) }.mapError<Fut<T>> { futFailed(it) }.getAny()

      private val defaultExecutor = CompletableFuture<Any>().defaultExecutor()!!

      private fun <R> (() -> R).logging(): () -> R = {
         try {
            this()
         } catch (t: Throwable) {
            logger.error(t) { "Unhandled exception" }
            throw t
         }
      }

      private fun <T, R> ((T) -> R).logging(): (T) -> R = {
         try {
            this(it)
         } catch (t: Throwable) {
            logger.error(t) { "Unhandled exception" }
            throw t
         }
      }

   }

   /** Output result of [Fut]. */
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

      fun toTryRaw(): Try<T, Throwable> = when (this) {
         is ResultOk<T> -> Try.ok(value)
         is ResultInterrupted -> Try.error(error)
         is ResultFail -> Try.error(error.cause ?: error)
      }

      fun or(block: (Exception) -> @UnsafeVariance T) = toTry().getOrSupply(block)

   }

}
