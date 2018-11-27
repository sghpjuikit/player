package sp.it.pl.util.async

import javafx.animation.Animation.INDEFINITE
import javafx.application.Platform
import javafx.util.Duration
import javafx.util.Duration.ZERO
import mu.KotlinLogging
import sp.it.pl.util.async.executor.FxTimer
import sp.it.pl.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.pl.util.dev.fail
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.functional.kt
import sp.it.pl.util.math.millis
import java.awt.EventQueue
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

private val logger = KotlinLogging.logger { }

operator fun Executor.invoke(block: Runnable) = execute(block)
operator fun Executor.invoke(block: () -> Unit) = execute(block)

@JvmField val FX = FxExecutor()
@JvmField val FX_LATER = FxLaterExecutor()
@JvmField val NEW = NewThreadExecutor()
@JvmField val CURR = Executor { it() }

class NewThreadExecutor: Executor {
    override fun execute(command: Runnable) = runNew(command)
    operator fun invoke(name: String) = Executor { runNew(name, it) }
}

class FxExecutor: Executor {
    override fun execute(command: Runnable) = runFX(command)
    operator fun invoke(delay: Duration) = Executor { runFX(delay) { it() } }
}

class FxLaterExecutor: Executor {
    override fun execute(command: Runnable) = runLater(command)
    operator fun invoke(delay: Duration) = Executor { runFX(delay) { it() } }
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

/** Executes the specified block using the specified executor. */
fun runOn(executor: Executor, block: () -> Unit) = executor.execute(block)

/**
 * Executes the specified block on current thread repeatedly with given time period.
 * Equivalent to `new FxTimer(delay, action, INDEFINITE).restart();`.
 *
 * @param period delay
 * @param block action. Takes the timer as a parameter. Use it to stop the periodic execution. Otherwise it will
 * never stop !
 */
fun runPeriodic(period: Duration, block: () -> Unit): FxTimer {
    val t = fxTimer(period, INDEFINITE, block)
    t.start()
    return t
}

/**
 * Executes the specified block immediately on a new daemon thread.
 * Equivalent to:
 * ```
 * Thread thread = new Thread(action);
 * thread.setDaemon(true);
 * thread.start();
 * ```
 */
fun runNew(block: () -> Unit) {
    Thread(block).apply {
        isDaemon = true
        start()
    }
}

/** Legacy version of [runNew] for Java taking a [Runnable]. */
fun runNew(block: Runnable) = runNew(block.kt)

fun runNew(threadName: String, block: () -> Unit) {
    Thread(block).apply {
        isDaemon = true
        name = threadName
        start()
    }
}

/** Legacy version of [runNew] for Java taking a [Runnable]. */
fun runNew(threadName: String, block: Runnable) = runNew(threadName, block.kt)

fun runAfter(delay: Duration, executor: Consumer<Runnable>, block: Runnable) {
    if (delay.lessThanOrEqualTo(ZERO)) {
        executor(block)
    } else {
        executor(Runnable {
            if (Platform.isFxApplicationThread()) {
                fxTimer(delay, 1, block.kt).start()
            } else {
                try {
                    Thread.sleep(delay.toMillis().toLong())
                    block()
                } catch (e: InterruptedException) {
                    logger.error(e) { "Thread interrupted while sleeping" }
                }

            }
        })
    }
}

fun runNewAfter(delay: Duration, block: Runnable) = runNew("delayed-bgr-action") {
    try {
        Thread.sleep(delay.toMillis().toLong())
        block()
    } catch (e: InterruptedException) {
        logger.error(e) { "Thread interrupted while sleeping" }
    }
}

/** Executes the specified block on awt thread, immediately if called on awt thread, or using [EventQueue.invokeLater] otherwise. */
fun runAwt(block: () -> Unit) = if (EventQueue.isDispatchThread()) block() else EventQueue.invokeLater(block)

/** Legacy version of [runFX] for Java taking a [Runnable]. */
fun runFX(block: Runnable): Unit = runFX(block.kt)

/** Executes the specified block on fx thread, immediately if called on fx thread, or using [Platform.runLater] otherwise. */
fun runFX(block: () -> Unit) = if (Platform.isFxApplicationThread()) block() else Platform.runLater(block)

/** Legacy version of [runFX] for Java taking a [Runnable]. */
fun runFX(delay: Duration, block: Runnable) = runFX(delay, block.kt)

fun runNotFX(block: () -> Unit): Unit = if (Platform.isFxApplicationThread()) runNew(block) else block()

/** Legacy version of [runNotFX] for Java taking a [Runnable]. */
fun runNotFX(block: Runnable) = runNotFX(block.kt)


/**
 * Executes the specified block on fx thread after specified delay from now.
 *
 * If delay is
 * * zero, block is invoked immediately if called on fx thread, or using [Platform.runLater] otherwise
 * * less than zero exception is thrown.
 * * more than zero, blocked is invoked after the delay
 */
fun runFX(delay: Duration, block: () -> Unit) {
    when {
        delay < ZERO -> fail()
        delay > ZERO -> {
            val time = System.currentTimeMillis().toDouble()
            runFX {
                val diff = System.currentTimeMillis()-time
                val duration = (delay.toMillis()-diff).coerceAtLeast(0.0)
                fxTimer(duration.millis, 1, block).start()
            }
        }
        else -> runFX(block)
    }
}

/* Executes the specified blocks on fx thread sequentially with the specified delays. */
fun runFX(delay1: Double, block1: () -> Unit, delay2: Double, block2: () -> Unit) {
    runFX(delay1.millis) {
        block1()
        runFX(delay2.millis, block2)
    }
}

/** Legacy version of [runLater] for Java taking a [Runnable]. */
fun runLater(block: Runnable) = runLater(block.kt)

/**
 * Executes the specified block on fx thread at unspecified time in the future.
 *
 * Use to execute the action on fx thread, but not immediately (i.e. skip the current pulse).
 * In practice the delay is very small.
 *
 * Equivalent to: `Platform.runLater(r);
 */
fun runLater(block: () -> Unit) = Platform.runLater(block)

fun onlyIfMatches(r: Runnable, counter: AtomicLong): Runnable {
    val c = counter.get()
    return Runnable {
        if (c==counter.get())
            r()
    }
}

/** @return single thread executor using specified thread factory */
fun oneThreadExecutor() = Executors.newSingleThreadExecutor(threadFactory(true))!!

/** @return single thread executor keeping the thread alive for specified time and using specified thread factory */
fun oneCachedThreadExecutor(keepAliveTime: Duration, threadFactory: ThreadFactory) =
        ThreadPoolExecutor(0, 1, keepAliveTime.toMillis().toLong(), TimeUnit.MILLISECONDS, LinkedBlockingQueue<Runnable>(), threadFactory)

/** Resolves: https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing/19528305#19528305 */
fun newThreadPoolExecutor(maxPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, threadFactory: ThreadFactory): ExecutorService {
    // TODO: implement properly
    return ThreadPoolExecutor(maxPoolSize, maxPoolSize, keepAliveTime, unit, LinkedBlockingQueue<Runnable>(), threadFactory).apply {
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