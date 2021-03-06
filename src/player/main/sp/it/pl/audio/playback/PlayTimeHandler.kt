package sp.it.pl.audio.playback

import javafx.util.Duration
import sp.it.util.async.executor.FxTimer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer

/** Handler for handling events at specific point of song playback. */
class PlayTimeHandler {

   private val whenCalculator: (Duration) -> Duration
   private val timer: FxTimer

   private constructor(whenCalculator: (Duration) -> Duration, action: () -> Unit) {
      this.whenCalculator = whenCalculator
      this.timer = fxTimer(Duration.INDEFINITE, 1, action)
   }

   fun pause() = timer.pause()

   fun unpause() = timer.unpause()

   fun stop() = timer.stop()

   fun restart(totalTime: Duration) = timer.start(whenCalculator(totalTime))

   companion object {

      /**
       * @param whenCalculator calculates when the event should fire. The function receives total song length and returns time at which the action executes
       * @param action action to execute
       */
      @JvmStatic
      fun at(whenCalculator: (Duration) -> Duration, action: () -> Unit) = PlayTimeHandler(whenCalculator, action)

   }

}