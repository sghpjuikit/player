package sp.it.util.animation

import javafx.animation.AnimationTimer
import sp.it.util.functional.invoke
import java.util.function.LongConsumer

/** Timer, that executes behavior in each frame while it is running. */
open class Loop {
    private var active: Boolean = false
    private val action: LongConsumer
    private val timer = object: AnimationTimer() {
        override fun handle(l: Long) {
            doLoop(l)
        }
    }

    /**
     * Creates a new loop with the specified action.
     *
     * @param action behavior to execute, that takes parameter - the timestamp of the current frame given in nanoseconds
     */
    constructor(action: LongConsumer) {
        this.action = action
    }

    /** Creates a new loop with the specified action. */
    constructor(action: Runnable) {
        this.action = LongConsumer { action() }
    }

    /** Starts this loop if it is not running. Once started, the action will be called in every frame. */
    fun start() {
        if (!active) {
            active = true
            timer.start()
        }
    }

    /** Stops this loop if it is running. */
    fun stop() {
        if (active) {
            timer.stop()
            active = false
        }
    }

    /**
     * @param now The timestamp of the current frame given in nanoseconds
     */
    protected open fun doLoop(now: Long) = action(now)

}