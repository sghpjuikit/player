package sp.it.util.async.actor

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.BlockingQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.jetbrains.annotations.Blocking
import sp.it.util.async.future.Fut
import sp.it.util.async.sleep
import sp.it.util.async.threadFactory
import sp.it.util.dev.ThreadSafe
import sp.it.util.functional.Option.Some
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.type.volatile

private val logger = KotlinLogging.logger { }

/**
 * Actor that uses virtual thread.
 * Does not prevent application shutdown.
 * Starts processing events immediatelly.
 */
class ActorVt<T>(
   private val name: String,
   private val queue: BlockingQueue<Some<T>> = LinkedBlockingQueue(),
   private val action: (T) -> Unit
) {
   private var isClosed = AtomicBoolean(true)
   private var isRunning by volatile(true)
   private val actionSafe = { t: T -> runTry { action(t) }.ifError { logger.error(it) { "$name failed to process event" } } }

   init {
      Thread.ofVirtual().name(name).start {
         while (true)
            if (queue.isEmpty() && !isRunning) break
            else actionSafe(queue.take().value)
      }
   }

   /** Send event for processing. Returns immediatelly. */
   operator fun invoke(message: T): Unit {
      if (isRunning)
         queue.put(Some(message))
   }

   /** Close actor. New events are ignored. Queued events will be processed. Returns immediatelly. */
   fun close() {
      isRunning = false
   }

   /** Close actor. New events are ignored. Queued events will be processed. Returns after all events are processed. */
   @Blocking
   @ThreadSafe
   fun closeAndWait(): Unit {
      isRunning = false
      while (queue.isNotEmpty()) sleep(1)
   }

}

/**
 * Actor that uses non daemon thread. Uses 0 threads when idle.
 * Prevents application shutdown.
 * Starts processing events immediatelly.
 */
class ActorSe<T>(
   private val name: String,
   private val action: (T) -> Unit
) {
   private val actionSafe = { t: T -> runTry { action(t) }.ifError { logger.error(it) { "Failed to process event" } } }
   private val executor = ThreadPoolExecutor(0, 1, 10, TimeUnit.SECONDS, LinkedBlockingQueue(), threadFactory(name, false))

   /** Send event for processing. Returns immediatelly. */
   operator fun invoke(message: T): Fut<Unit> =
      Fut<Unit>(
         CompletableFuture<Unit>().also { future ->
            executor.execute {
               runTry {
                  actionSafe(message)
               }.ifOk {
                  future.complete(Unit)
               }.ifError {
                  future.completeExceptionally(it)
               }
            }
         }
      )

   /** Close actor. New events are ignored. Queued events will be processed. Returns immediatelly. */
   fun close(): Unit =
      executor.shutdown()

   /** Close actor. New events are ignored. Queued events will be processed. Returns after all events are processed. */
   @Blocking
   @ThreadSafe
   fun closeAndWait(): Unit =
      executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS).toUnit()

}