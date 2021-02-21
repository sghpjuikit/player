/*
 * Implementation based on ReactFX by Tomas Mikula, https://github.com/TomasMikula/ReactFX
 */

package sp.it.util.async.executor

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.util.Duration
import sp.it.util.collections.setToOne
import sp.it.util.functional.invoke
import sp.it.util.units.millis

/**
 * Lightweight timer constrained to JavaFX application thread.
 * Allows running an action with delay or periodically as well as restarting or querying state.
 *
 * Not thread safe! Must only be used on FxApplication thread.
 */
class FxTimer {

   /**
    * Sets the delay for the task. Takes effect only if set before the task
    * execution is planned. It will not affect currently running cycle. It will
    * affect every subsequent cycle. Therefore, it is pointless to run this
    * method if this timer is non-periodic.
    */
   var period: Duration
   private val action: Runnable
   private val timeline: Timeline
   private var seq: Long = 0

   /** Returns true if not stopped or paused */
   val isRunning: Boolean
      get() = timeline.currentRate!=0.0

   private constructor(delay: Duration, cycles: Int, action: Runnable) {
      this.action = action
      this.period = delay
      this.timeline = Timeline()
      this.timeline.cycleCount = cycles
   }

   /** @param run if true, [start] will be called, otherwise calls [stop]. */
   fun setRunning(run: Boolean) {
      if (run) start()
      else stop()
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
         timeline.keyFrames setToOne KeyFrame(period, {
            if (seq==expected)
               runNow()
         })
         timeline.play()
      }
   }

   fun runNow() = action()

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

   fun setTimeoutAndRestart(timeoutMs: Double) {
      if (isRunning) start(timeoutMs.millis)
      else period = timeoutMs.millis
   }

   companion object {
      /**
       * Creates a (stopped) timer that executes the given action a specified number of times with a delay period.
       *
       * @param delay time to wait before each execution. The first execution is already delayed.
       * @param cycles denotes number of executions, use -1 for infinite executions
       * @param action action to execute
       */
      @JvmStatic
      fun fxTimer(delay: Duration, cycles: Int, action: () -> Unit) = FxTimer(delay, cycles, action)
   }

}