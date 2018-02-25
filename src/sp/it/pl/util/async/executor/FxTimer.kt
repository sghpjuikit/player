package sp.it.pl.util.async.executor

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.event.EventHandler
import javafx.util.Duration
import sp.it.pl.util.functional.invoke

/**
 * Provides factory methods for timers that are manipulated from and execute
 * their action on the JavaFX application thread.
 *
 * @author Tomas Mikula (original Java code)
 */
class FxTimer
/**
 * Creates a (stopped) timer that executes the given action specified number
 * of times with a delay period.
 *
 * @param delay Time to wait before each execution. The first execution is already delayed.
 * @param cycles denotes number of executions. Use 1 for single execution, n for n executions and
 * @param action action to execute
 * [javafx.animation.Transition.INDEFINITE] for infinite amount.
 */
(delay: Duration, cycles: Int, private val action: Runnable) {

    /** Equivalent to `new FxTimer(Duration.millis(delay), action, cycles);` */
    constructor(delayMillis: Double, cycles: Int, action: Runnable) : this(Duration.millis(delayMillis), cycles, action)

    constructor(delayMillis: Double, cycles: Int, action: () -> Unit) : this(Duration.millis(delayMillis), cycles, action as Runnable)

    constructor(delay: Duration, cycles: Int, action: () -> Unit) : this(delay, cycles, action as Runnable)

    private val timeline: Timeline
    /**
     * Sets the delay for the task. Takes effect only if set before the task
     * execution is planned. It will not affect currently running cycle. It will
     * affect every subsequent cycle. Therefore, it is pointless to run this
     * method if this timer is non-periodic.
     */
    var period: Duration
    private var seq: Long = 0

    init {
        this.period = Duration.millis(delay.toMillis())
        this.timeline = Timeline()
        timeline.cycleCount = cycles
    }

    /** Returns true if not stopped or paused.
     *
     * Setting it to true is equivalent to calling [start], setting it to false is equivalent to calling [stop]  */
    var isRunning: Boolean
        get() = timeline.currentRate != 0.0
        set(b) = if (b)
            start()
        else
            stop()

    /** Equivalent to setting [period] and then calling [start] */
    @JvmOverloads
    fun start(period: Duration = this.period) {
        stop()
        val expected = seq
        this.period = period

        if (period.toMillis() == 0.0)
            runNow()
        else {
            timeline.keyFrames.setAll(KeyFrame(period, EventHandler {
                if (seq == expected) {
                    runNow()
                }
            }))
            timeline.play()
        }
    }

    fun start(periodInMs: Double) {
        start(Duration.millis(periodInMs))
    }

    fun runNow() {
        action.invoke()
    }

    fun pause() {
        timeline.pause()
    }

    fun unpause() {
        timeline.play()
    }

    fun stop() {
        timeline.stop()
        seq++
    }

    fun setPeriod(millis: Double) {
        this.period = Duration.millis(millis)
    }

    /**
     * If timer running, executes [start], else sets the period
     *
     * Essentially same as [start], but restarts only when already running
     */
    fun setTimeoutAndRestart(timeout: Duration) {
        if (isRunning)
            this@FxTimer.start(timeout)
        else
            this@FxTimer.period = timeout
    }

    fun setTimeoutAndRestart(timeoutInMillis: Double) {
        if (isRunning)
            this@FxTimer.start(Duration.millis(timeoutInMillis))
        else
            this@FxTimer.period = Duration.millis(timeoutInMillis)
    }
}