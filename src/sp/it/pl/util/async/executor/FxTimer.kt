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
/** @see fxTimer */
(delay: Duration, cycles: Int, private val action: Runnable) {

    /** @see fxTimer */
    constructor(delayMillis: Double, cycles: Int, action: Runnable): this(Duration.millis(delayMillis), cycles, action)

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
        period = Duration.millis(delay.toMillis())
        timeline = Timeline()
        timeline.cycleCount = cycles
    }

    /** Returns true if not stopped or paused */
    val isRunning: Boolean
        get() = timeline.currentRate!=0.0

    /** @param run if true, [start] will be called, otherwise calls [stop]. */
    fun setRunning(run: Boolean) {
        if (run)
            start()
        else
            stop()
    }

    /** Equivalent to setting [period] and then calling [start]. */
    @JvmOverloads
    fun start(period: Duration = this.period) {
        stop()
        val expected = seq
        this.period = period

        if (period.toMillis()==0.0) {
            runNow()
        } else {
            timeline.keyFrames.setAll(KeyFrame(period, EventHandler {
                if (seq==expected)
                    runNow()
            }))
            timeline.play()
        }
    }

    fun runNow() = action.invoke()

    fun pause() = timeline.pause()

    fun unpause() = timeline.play()

    fun stop() {
        timeline.stop()
        seq++
    }

    /**
     * If timer running, executes [start], otherwise sets the period.
     *
     * Essentially same as [start], but restarts only when already running
     */
    fun setTimeoutAndRestart(timeout: Duration) {
        if (isRunning)
            start(timeout)
        else
            period = timeout
    }

    fun setTimeoutAndRestart(timeoutInMillis: Double) {
        if (isRunning)
            start(Duration.millis(timeoutInMillis))
        else
            period = Duration.millis(timeoutInMillis)
    }

    companion object {
        /**
         * Creates a (stopped) timer that executes the given action a specified number of times with a delay period.
         *
         * @param delay Time to wait before each execution. The first execution is already delayed.
         * @param cycles denotes number of executions, use -1 for infinite executions
         * @param action action to execute
         */
        fun fxTimer(delayMillis: Double, cycles: Int, action: () -> Unit) = FxTimer(Duration.millis(delayMillis), cycles, Runnable { action() })

        /** Equivalent to `fxTimer(Duration.millis(delay), action, cycles);`
         * @see fxTimer */
        fun fxTimer(delay: Duration, cycles: Int, action: () -> Unit) = FxTimer(delay, cycles, Runnable { action() })
    }

}