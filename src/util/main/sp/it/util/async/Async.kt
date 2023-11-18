package sp.it.util.async

import java.awt.EventQueue
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javafx.animation.Animation
import javafx.application.Platform
import javafx.util.Duration
import javafx.util.Duration.ZERO
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import mu.KotlinLogging
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.future.Futs
import sp.it.util.collections.materialize
import sp.it.util.dev.Experimental
import sp.it.util.dev.fail
import sp.it.util.functional.asTryList
import sp.it.util.functional.invoke
import sp.it.util.functional.kt
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.math.max
import sp.it.util.reactive.Subscription
import sp.it.util.units.millis
import sp.it.util.units.minutes

private val logger = KotlinLogging.logger { }

operator fun Executor.invoke(block: Runnable) = execute(block)
operator fun Executor.invoke(block: () -> Unit) = execute(block)

@JvmField val AWT = AwtExecutor
@JvmField val FX = FxExecutor
@JvmField val FX_LATER = FxLaterExecutor
@JvmField val NEW = NewThreadExecutor()
@JvmField val IO_LATER = IOLaterExecutor()
@JvmField val IO = IOExecutor(IO_LATER)
@JvmField val CURR = Executor { it() }
@JvmField val VT = VTExecutor()

/**
 * Executes the specified block immediately on a new daemon thread.
 * Equivalent to:
 * ```
 * var thread = new Thread(action);
 * thread.setDaemon(true);
 * thread.start();
 * ```
 */
class NewThreadExecutor: Executor {

   override fun execute(command: Runnable) = thread(start = true, isDaemon = true, block = command.kt).toUnit()

   /**
    * Executes the specified block immediately on a new daemon thread.
    * Equivalent to:
    * ```
    * var thread = new Thread(action);
    * thread.setDaemon(true);
    * thread.setName(threadName);
    * thread.start();
    * ```
    */
   operator fun invoke(threadName: String, block: Runnable) = invoke(threadName).execute(block)

   operator fun invoke(threadName: String, block: () -> Unit) = invoke(threadName).execute(block)

   operator fun invoke(threadName: String) = Executor { thread(start = true, isDaemon = true, name = threadName, block = it.kt) }

}

/** Executes the specified block on awt thread, immediately if called on awt thread, or using [EventQueue.invokeLater] otherwise. */
object AwtExecutor: Executor {
   override fun execute(command: Runnable) =
      if (EventQueue.isDispatchThread()) command() else EventQueue.invokeLater(command)
}

/** Executes the specified block on fx thread, immediately if called on fx thread, or using [Platform.runLater] otherwise. */
object FxExecutor: Executor, CoroutineContext by Dispatchers.JavaFx  {
   override fun execute(command: Runnable) =
      if (Platform.isFxApplicationThread()) command() else Platform.runLater(command)

   /**
    * Executes the specified block on fx thread after specified delay from now.
    *
    * If delay is
    * * zero, block is invoked on [FX]
    * * less than zero exception is thrown.
    * * more than zero, blocked is invoked after the delay
    */
   fun delayed(delay: Duration): Executor = when {
      delay<ZERO -> fail()
      delay>ZERO -> Executor {
         val time = System.currentTimeMillis().toDouble()
         runFX {
            val diff = System.currentTimeMillis() - time
            val duration = 0.0 max delay.toMillis() - diff
            fxTimer(duration.millis, 1, it.kt).start()
         }
      }
      else -> this
   }
}

/** Executes the specified block on fx thread using [Platform.runLater]. */
object FxLaterExecutor: Executor {
   override fun execute(command: Runnable) = Platform.runLater(command)
}

/** Executes the specified block on thread in an IO thread pool or immediately if called on such thread. */
class IOExecutor(private val e: IOLaterExecutor): Executor {
   override fun execute(it: Runnable) = if (Thread.currentThread().name.startsWith("io-")) it() else e(it)
}

/** Executes the specified block on thread in an IO thread pool. */
class IOLaterExecutor(private val e: Executor = burstTPExecutor(32, 1.minutes, threadFactory("io", true))): Executor {
   override fun execute(it: Runnable) = e(it)
}

/** Executes the specified block on thread in an IO thread pool. */
class VTExecutor: Executor by Executors.newVirtualThreadPerTaskExecutor()!! {
   fun named(name: String): Executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name(name).factory())
   operator fun invoke(name: String): Executor = Executor { Thread.ofVirtual().name(name).start(it) }
}

/** Sleeps currently executing thread for specified duration. When interrupted, returns.  */
fun sleep(duration: Duration) = sleep(duration.toMillis().toLong())

/** Sleeps currently executing thread for duration specified in milliseconds. When interrupted, returns.  */
fun sleep(durationMillis: Long) {
   try {
      Thread.sleep(durationMillis)
   } catch (e: InterruptedException) {
      logger.trace { "Thread=${Thread.currentThread().name} interrupted while sleeping" }
   }
}

/** Executes the specified block using the specified executor and return the result as [sp.it.util.async.future.Fut]. */
fun <T> runOn(executor: Executor, block: () -> T) = fut(null).then(executor) { block() }

/** Calls [runOn] using [NEW] and the specified block. */
fun <T> runNew(block: () -> T) = runOn(NEW, block)

/** Legacy version of [runNew] for Java taking a [Runnable]. */
fun runNew(block: Runnable) = runNew(block.kt)

/** Calls [runOn] using [AWT] and the specified block. */
fun <T> runAwt(block: () -> T) = runOn(AWT, block)

/** Legacy version of [runAwt] for Java taking a [Runnable]. */
fun runAwt(block: Runnable) = runAwt(block.kt)

/** Calls [runOn] using [FX] and the specified block. */
fun <T> runFX(block: () -> T) = runOn(FX, block)

/** Legacy version of [runFX] for Java taking a [Runnable]. */
fun runFX(block: Runnable) = runFX(block.kt)

/** Calls [runOn] using [FX] with the specified delay and the specified block. */
fun <T> runFX(delay: Duration, block: () -> T) = runOn(FX.delayed(delay), block)

/** Legacy version of [runFX] for Java taking a [Runnable]. */
fun runFX(delay: Duration, block: Runnable) = runFX(delay, block.kt)

/** Calls [runOn] using [FX_LATER] with the specified thread name and the specified block. */
fun <T> runLater(block: () -> T) = runOn(FX_LATER, block)

/** Legacy version of [runLater] for Java taking a [Runnable]. */
fun runLater(block: Runnable) = runLater(block.kt)

/** Calls [runOn] using [IO] and the specified block. */
fun <T> runIO(block: () -> T) = runOn(IO, block)

/** Legacy version of [runIO] for Java taking a [Runnable]. */
fun runIO(block: Runnable) = runIO(block.kt)

/** Calls [runOn] using [VT] and the specified block. */
fun <T> runVT(block: () -> T) = runOn(VT, block)

/** Legacy version of [runVT] for Java taking a [Runnable]. */
fun runVT(block: Runnable) = runVT(block.kt)

/** Runs the specified block for each specified item using the max specified parallelism, blocks until finish and returns results.  */
@Experimental("does not handle errors correctly")
fun <T, R> runIoParallel(parallelism: Int = Runtime.getRuntime().availableProcessors(), items: Collection<T>, block: (T) -> R): Futs<R, Throwable> = runIO {
  val windowSize = if (items.size<parallelism) parallelism else items.size/parallelism
  val windows = items.windowed(windowSize, windowSize, true)
  val windowedTries = windows.map { runOn(IO_LATER) { it.map { runTry { block(it) } } } }.materialize()
  val tries = windowedTries.flatMap { it.getDone().toTry().orThrow }
  tries.asTryList()
}

/** Executes the specified block periodically with given time period (1st call is already delayed). */
fun runPeriodic(period: Duration, block: () -> Unit): Subscription {
   val t = fxTimer(period, Animation.INDEFINITE, block)
   t.start()
   return Subscription { t.stop() }
}

fun onlyIfMatches(counter: AtomicLong, r: () -> Unit): () -> Unit {
   val c = counter.get()
   return {
      if (c==counter.get())
         r()
   }
}

fun onlyIfMatches(counter: AtomicLong, r: Runnable): Runnable {
   val c = counter.get()
   return Runnable {
      if (c==counter.get())
         r()
   }
}

/** @return single thread executor keeping the thread alive for specified time and using specified thread factory */
fun oneCachedTPExecutor(keepAliveTime: Duration, threadFactory: ThreadFactory) =
   ThreadPoolExecutor(0, 1, keepAliveTime.toMillis().toLong(), TimeUnit.MILLISECONDS, LinkedBlockingQueue(), threadFactory)

/**
 * Resolves: https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing/19528305#19528305
 * Due to the nature of the fix, core pool size is 0.
 *
 * @return single thread executor keeping the thread alive for specified time and using specified thread factory
 */
fun burstTPExecutor(maxPoolSize: Int, keepAliveTime: Duration, threadFactory: ThreadFactory): ExecutorService {
   return ThreadPoolExecutor(maxPoolSize, maxPoolSize, keepAliveTime.toMillis().toLong(), TimeUnit.MILLISECONDS, LinkedBlockingQueue(), threadFactory).apply {
      allowCoreThreadTimeOut(true)
   }
}

fun threadFactory(nameBase: String, daemon: Boolean): ThreadFactory {
   val id = AtomicLong(0)
   return ThreadFactory { r ->
      Thread(r).apply {
         name = "$nameBase-${id.getAndIncrement()}"
         isDaemon = daemon
         setUncaughtExceptionHandler { _, e -> logger.error(e) { "Uncaught exception" } }
      }
   }
}