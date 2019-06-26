package sp.it.util.async

import javafx.animation.Animation
import javafx.application.Platform
import javafx.util.Duration
import javafx.util.Duration.ZERO
import mu.KotlinLogging
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.dev.fail
import sp.it.util.functional.invoke
import sp.it.util.functional.kt
import sp.it.util.math.max
import sp.it.util.reactive.Subscription
import sp.it.util.units.millis
import sp.it.util.units.minutes
import java.awt.EventQueue
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }

operator fun Executor.invoke(block: Runnable) = execute(block)
operator fun Executor.invoke(block: () -> Unit) = execute(block)

@JvmField val AWT = AwtExecutor()
@JvmField val FX = FxExecutor()
@JvmField val FX_LATER = FxLaterExecutor()
@JvmField val NEW = NewThreadExecutor()
@JvmField val CURR = Executor { it() }
@JvmField val IO: Executor = burstTPExecutor(64, 1.minutes, threadFactory("io", true))

/**
 * Executes the specified block immediately on a new daemon thread.
 * Equivalent to:
 * ```
 * Thread thread = new Thread(action);
 * thread.setDaemon(true);
 * thread.start();
 * ```
 */
class NewThreadExecutor: Executor {

    override fun execute(command: Runnable) {
        thread(start = true, isDaemon = true, block = command.kt)
    }

    /**
     * Executes the specified block immediately on a new daemon thread.
     * Equivalent to:
     * ```
     * Thread thread = new Thread(action);
     * thread.setDaemon(true);
     * thread.setName(threadName);
     * thread.start();
     * ```
     */
    operator fun invoke(threadName: String) = Executor {
        thread(start = true, isDaemon = true, name = threadName, block = it.kt)
    }

}

/** Executes the specified block on awt thread, immediately if called on awt thread, or using [EventQueue.invokeLater] otherwise. */
class AwtExecutor: Executor {
    override fun execute(command: Runnable) = if (EventQueue.isDispatchThread()) command() else EventQueue.invokeLater(command)
}

/** Executes the specified block on fx thread, immediately if called on fx thread, or using [Platform.runLater] otherwise. */
class FxExecutor: Executor {
    override fun execute(command: Runnable) = if (Platform.isFxApplicationThread()) command() else Platform.runLater(command)

    /**
     * Executes the specified block on fx thread after specified delay from now.
     *
     * If delay is
     * * zero, block is invoked on [FX]
     * * less than zero exception is thrown.
     * * more than zero, blocked is invoked after the delay
     */
    operator fun invoke(delay: Duration) = when {
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
class FxLaterExecutor: Executor {
    override fun execute(command: Runnable) = Platform.runLater(command)
}

/** Sleeps currently executing thread for specified duration. When interrupted, returns.  */
fun sleep(duration: Duration) = sleep(duration.toMillis().toLong())

/** Sleeps currently executing thread for duration specified in milliseconds. When interrupted, returns.  */
fun sleep(durationMillis: Long) {
    try {
        Thread.sleep(durationMillis)
    } catch (e: InterruptedException) {
        logger.error { "Thread interrupted while sleeping" }
    }
}

/** Executes the specified block using the specified executor and return the result as [sp.it.util.async.future.Fut]. */
fun <T> runOn(executor: Executor, block: () -> T) = fut(null).then(executor) { block() }

/** Calls [runOn] using [NEW] and the specified block. */
fun <T> runNew(block: () -> T) = runOn(NEW, block)

/** Legacy version of [runNew] for Java taking a [Runnable]. */
fun runNew(block: Runnable) = runNew(block.kt)

/** Calls [runOn] using [NEW] with the specified thread name and the specified block. */
fun <T> runNew(threadName: String, block: () -> T) = runOn(NEW(threadName), block)

/** Legacy version of [runNew] for Java taking a [Runnable]. */
fun runNew(threadName: String, block: Runnable) = runNew(threadName, block.kt)

/** Calls [runOn] using [AWT] and the specified block. */
fun <T> runAwt(block: () -> T) = runOn(AWT, block)

/** Legacy version of [runAwt] for Java taking a [Runnable]. */
fun runAwt(block: Runnable) = runAwt(block.kt)

/** Calls [runOn] using [FX] and the specified block. */
fun <T> runFX(block: () -> T) = runOn(FX, block)

/** Legacy version of [runFX] for Java taking a [Runnable]. */
fun runFX(block: Runnable) = runFX(block.kt)

/** Calls [runOn] using [FX] with the specified delay and the specified block. */
fun <T> runFX(delay: Duration, block: () -> T) = runOn(FX(delay), block)

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

/** @return single thread executor using specified thread factory */
fun oneTPExecutor() = Executors.newSingleThreadExecutor(threadFactory(true))!!

/** @return single thread executor keeping the thread alive for specified time and using specified thread factory */
fun oneCachedTPExecutor(keepAliveTime: Duration, threadFactory: ThreadFactory) =
    ThreadPoolExecutor(0, 1, keepAliveTime.toMillis().toLong(), TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), threadFactory)

/**
 * Resolves: https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing/19528305#19528305
 * Due to the nature of the fix, core pool size is 0.
 *
 * @return single thread executor keeping the thread alive for specified time and using specified thread factory
 */
fun burstTPExecutor(maxPoolSize: Int, keepAliveTime: Duration, threadFactory: ThreadFactory): ExecutorService {
    return ThreadPoolExecutor(maxPoolSize, maxPoolSize, keepAliveTime.toMillis().toLong(), TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), threadFactory).apply {
        allowCoreThreadTimeOut(true)
    }
}

fun threadFactory(daemon: Boolean): ThreadFactory {
    return ThreadFactory { r ->
        Thread(r).apply {
            isDaemon = daemon
            setUncaughtExceptionHandler { _, e -> logger.error(e) { "Uncaught exception" } }
        }
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