package util.async

import javafx.animation.Animation.INDEFINITE
import javafx.application.Platform
import javafx.util.Duration
import mu.KotlinLogging
import util.async.executor.FxTimer
import util.dev.throwIf
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
@JvmField val CURR = Consumer<Runnable> { it.run() }

fun FX_AFTER(delay: Double): Consumer<Runnable> = Consumer { runFX(delay, it) }
fun FX_AFTER(delay: Duration): Consumer<Runnable> = Consumer { runFX(delay, it) }
@JvmField val eFX = Executor { FX.accept(it) }
@JvmField val eFX_LATER = Executor { FX_LATER.accept(it) }
@JvmField val eBGR = Executor { NEW.accept(it) }
@JvmField val eCURR = Executor { CURR.accept(it) }

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

/** Executes the runnable immediately on current thread. */
fun Runnable.run() = run()

/**
 * Executes the action on current thread after specified delay from now.
 * Equivalent to `new FxTimer(delay, action, 1).restart();`.
 *
 * @param delay delay
 */
fun run(delay: Duration, action: Runnable) {
    FxTimer(delay, 1, action).start()
}

/**
 * Executes the action on current thread after specified delay from now.
 * Equivalent to `new FxTimer(delay, action, 1).restart();`.
 *
 * @param delay delay in milliseconds
 */
fun run(delay: Double, action: Runnable) {
    FxTimer(delay, 1, action).start()
}

/**
 * Executes the action on current thread repeatedly with given time period.
 * Equivalent to `new FxTimer(delay, action, INDEFINITE).restart();`.
 *
 * @param period delay
 * @param action action. Takes the timer as a parameter. Use it to stop the periodic execution. Otherwise it will
 * never stop !
 */
fun runPeriodic(period: Duration, action: Consumer<FxTimer>): FxTimer {
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
        executor.accept(r)
    } else {
        executor.accept(Runnable {
            if (Platform.isFxApplicationThread()) {
                FxTimer(delay, 1, r).start()
            } else {
                try {
                    Thread.sleep(delay.toMillis().toLong())
                    r.run()
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
            r.run()
        } catch (e: InterruptedException) {
            logger.error(e) { "Thread interrupted while sleeping" }
        }
    }
    thread.isDaemon = true
    thread.start()
}

/**
 * Executes runnable on fx thread, immediately id called on fx thread, or
 * using Platform.runLater() otherwise.
 *
 * Use to execute the action on fx as soon as possible.
 *
 * Equivalent to
 * <pre>
 * `if (Platform.isFxApplicationThread())
 * r.run();
 * else
 * Platform.runLater(r);
`* </pre>
 */
fun runFX(r: Runnable) {
    if (Platform.isFxApplicationThread())
        r.run()
    else
        Platform.runLater(r)
}

fun runFX(r: () -> Unit) = runFX(Runnable { r() })

fun runNotFX(r: Runnable) {
    if (Platform.isFxApplicationThread())
        runNew(r)
    else
        r.run()
}

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
        FxTimer(delay, 1, Runnable { runFX(r) }).start()
}

fun runFX(delay1: Double, r1: Runnable, delay2: Double, r2: Runnable) {
    throwIf(delay1<0)
    runFX(delay1, Runnable {
        r1.run()
        runFX(delay2, r2)
    })
}

/**
 * Executes the action on fx thread after specified delay from now.
 *
 * @param delay delay
 */
fun runFX(delay: Duration, r: Runnable) {
    FxTimer(delay, 1, Runnable { runFX(r) }).start()
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
            r.run()
    }
}

fun newSingleDaemonThreadExecutor() = Executors.newSingleThreadExecutor(threadFactory(true))!!

/**
 * Resolves:<br></br>
 * https://stackoverflow.com/questions/19528304/how-to-get-the-threadpoolexecutor-to-increase-threads-to-max-before-queueing/19528305#19528305
 */
fun newThreadPoolExecutor(maxPoolSize: Int, keepAliveTime: Long, unit: TimeUnit, threadFactory: ThreadFactory): ExecutorService {
    // TODO: implement properly
    return ThreadPoolExecutor(maxPoolSize, maxPoolSize, keepAliveTime, unit, LinkedBlockingQueue(), threadFactory).apply {
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
            setUncaughtExceptionHandler{ _, e -> logger.error(e) { "Uncaught exception" } }
        }
    }
}