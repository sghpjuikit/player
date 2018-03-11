package sp.it.pl.util.async

import javafx.animation.Animation.INDEFINITE
import javafx.application.Platform
import javafx.util.Duration
import mu.KotlinLogging
import sp.it.pl.util.async.executor.FxTimer
import sp.it.pl.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.pl.util.dev.throwIf
import sp.it.pl.util.functional.invoke
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

// TODO: clean up entire file

private val logger = KotlinLogging.logger { }

@JvmField val FX = Consumer<Runnable> { runFX(it) }
@JvmField val FX_LATER = Consumer<Runnable> { runLater(it) }
@JvmField val NEW = Consumer<Runnable> { runNew(it) }
@JvmField val CURR = Consumer<Runnable> { it() }

fun FX_AFTER(delay: Double): Consumer<Runnable> = Consumer { runFX(delay, it) }
fun FX_AFTER(delay: Duration): Consumer<Runnable> = Consumer { runFX(delay, it) }

@JvmField val eFX = Executor { FX(it) }
@JvmField val eFX_LATER = Executor { FX_LATER(it) }
@JvmField val eBGR = Executor { NEW(it) }
@JvmField val eCURR = Executor { CURR(it) }

/* --------------------- RUNNABLE ----------------------------------------------------------------------------------- */

/** Sleeps currently executing thread for specified duration. When interrupted, returns.  */
fun sleep(d: Duration) = sleep(d.toMillis().toLong())

/** Sleeps currently executing thread for duration specified in milliseconds. When interrupted, returns.  */
fun sleep(millis: Long) {
    try {
        Thread.sleep(millis)
    } catch (e: InterruptedException) {
        logger.error { "Thread interrupted while sleeping" }
    }
}

/** Runnable that invokes [.sleep].  */
fun sleeping(d: Duration): Runnable = Runnable { sleep(d) }

/* --------------------- EXECUTORS ---------------------------------------------------------------------------------- */

/**
 * Executes the action on current thread after specified delay from now.
 * Equivalent to `new FxTimer(delay, action, 1).restart();`.
 *
 * @param delay delay
 */
fun runAfter(delay: Duration, action: Runnable) {
    FxTimer(delay, 1, action).start()
}

fun runAfter(delay: Duration, action: () -> Unit) {
    runAfter(delay, Runnable { action() })
}

/**
 * Executes the action on current thread after specified delay from now.
 * Equivalent to `new FxTimer(delay, 1, action).restart();`.
 *
 * @param delay delay in milliseconds
 */
fun run(delay: Double, action: Runnable) {
    FxTimer(delay, 1, action).start()
}

fun run(delay: Double, action: () -> Unit) {
    run(delay, Runnable { action() })
}

/**
 * Executes the action on current thread repeatedly with given time period.
 * Equivalent to `new FxTimer(delay, action, INDEFINITE).restart();`.
 *
 * @param period delay
 * @param action action. Takes the timer as a parameter. Use it to stop the periodic execution. Otherwise it will
 * never stop !
 */
fun runPeriodic(period: Duration, action: Runnable): FxTimer {
    val t = FxTimer(period, INDEFINITE, action)
    t.start()
    return t
}

/**
 * Executes the runnable immediately on a new daemon thread.
 * Equivalent to
 * <pre>`Thread thread = new Thread(action);
 * thread.setDaemon(true);
 * thread.start();
`* </pre>
 */
fun runNew(r: Runnable) {
    Thread(r).apply {
        isDaemon = true
        start()
    }
}

fun runNew(r: () -> Unit) = runNew(Runnable { r() })

fun runNew(threadName: String, r: Runnable) {
    Thread(r).apply {
        isDaemon = true
        name = threadName
        start()
    }
}

fun runNew(threadName: String, r: () -> Unit) = runNew(threadName, Runnable { r() })

fun runAfter(delay: Duration, executor: Consumer<Runnable>, r: Runnable) {
    if (delay.lessThanOrEqualTo(Duration.ZERO)) {
        executor(r)
    } else {
        executor(Runnable {
            if (Platform.isFxApplicationThread()) {
                FxTimer(delay, 1, r).start()
            } else {
                try {
                    Thread.sleep(delay.toMillis().toLong())
                    r()
                } catch (e: InterruptedException) {
                    logger.error(e) { "Thread interrupted while sleeping" }
                }

            }
        })
    }
}

fun runNewAfter(delay: Duration, r: Runnable) {
    val thread = Thread {
        try {
            Thread.sleep(delay.toMillis().toLong())
            r()
        } catch (e: InterruptedException) {
            logger.error(e) { "Thread interrupted while sleeping" }
        }
    }
    thread.isDaemon = true
    thread.start()
}

/** Executes runnable on awt thread, immediately if called on fx thread, or using [EventQueue.invokeLater] otherwise. */
fun runAwt(block: () -> Unit) = if (EventQueue.isDispatchThread()) block() else EventQueue.invokeLater(block)

/** Executes runnable on fx thread, immediately if called on fx thread, or using [Platform.runLater] otherwise. */
fun runFX(r: Runnable): Unit = if (Platform.isFxApplicationThread()) r() else Platform.runLater(r)

fun runFX(r: () -> Unit) = runFX(Runnable { r() })

fun runNotFX(r: Runnable): Unit = if (Platform.isFxApplicationThread()) runNew(r) else r()

fun runNotFX(r: () -> Unit) = runNotFX(Runnable { r() })

/**
 * Executes the action on fx thread after specified delay from now.
 *
 * @param delay delay in milliseconds
 */
fun runFX(delay: Double, r: Runnable) {
    throwIf(delay<0)
    if (delay==0.0)
        runFX(r)
    else
        fxTimer(delay, 1) { runFX(r) }.start()
}

fun runFX(delay1: Double, r1: Runnable, delay2: Double, r2: Runnable) {
    throwIf(delay1<0)
    runFX(millis(delay1), Runnable {
        r1()
        runFX(delay2, r2)
    })
}

/**
 * Executes the action on fx thread after specified delay from now.
 *
 * @param delay delay
 */
fun runFX(delay: Duration, r: Runnable) {
    fxTimer(delay, 1) { runFX(r) }.start()
}

/**
 * Executes the runnable on fx thread at unspecified time in the future.
 *
 * Use to execute the action on fx thread, but not immediately. In practice
 * the delay is very small.
 *
 * Equivalent to: `Platform.runLater(r);
 */
fun runLater(r: Runnable) = Platform.runLater(r)

fun runLater(r: () -> Unit) = runLater(Runnable { r() })

fun onlyIfMatches(r: Runnable, counter: AtomicLong): Runnable {
    val c = counter.get()
    return Runnable {
        if (c==counter.get())
            r()
    }
}

fun newSingleDaemonThreadExecutor() = Executors.newSingleThreadExecutor(threadFactory(true))!!

/**
 * Resolves:<br></br>
 * https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing/19528305#19528305
 */
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