package sp.it.pl.audio.playback

import javafx.util.Duration
import sp.it.pl.util.async.executor.FxTimer
import sp.it.pl.util.math.Portion

/** Handler for handling events at specific point of song playback. */
class PlayTimeHandler {

    private val whenCalculator: (Duration) -> Duration
    private val timer: FxTimer

    /**
     * @param whenCalculator calculates when the event should fire.
     * The function receives total song length and returns time at which the action executes
     *
     * @param action action to execute
     */
    private constructor(whenCalculator: (Duration) -> Duration, action: Runnable) {
        this.whenCalculator = whenCalculator
        this.timer = FxTimer(Duration.INDEFINITE, 1, action)
    }

    fun pause() = timer.pause()

    fun unpause() = timer.unpause()

    fun stop() = timer.stop()

    fun restart(totalTime: Duration) = timer.start(whenCalculator(totalTime))

    companion object {

        @JvmStatic fun at(whenCalculator: (Duration) -> Duration, action: Runnable) = PlayTimeHandler(whenCalculator, action)

        @JvmStatic fun at(at: Duration, action: Runnable) = PlayTimeHandler({ at }, action)

        @JvmStatic fun at(at: Portion, action: Runnable) = PlayTimeHandler({ total -> at*total }, action)

    }

}