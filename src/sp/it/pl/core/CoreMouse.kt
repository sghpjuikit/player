package sp.it.pl.core

import javafx.geometry.Point2D
import org.reactfx.Subscription
import sp.it.pl.util.async.executor.FxTimer
import java.util.HashSet

/** Provides access to mouse position and mouse speed. By default lazy, i.e., consumes resources only if observed. */
object CoreMouse: Core {
    private const val pulseFrequency = 10 // Hz
    private var pulse: FxTimer? = null
    private var lastPos: Point2D? = null
    private var observeSpeed = false
    private val positionSubscribers = HashSet<(Point2D) -> Unit>()
    private val velocitySubscribers = HashSet<(Double) -> Unit>()

    /** @return mouse position in screen coordinates */
    fun getMousePosition(): Point2D = sp.it.pl.util.graphics.getMousePosition()

    /** Observe mouse position in screen coordinates. */
    fun observeMousePosition(action: (Point2D) -> Unit): Subscription {
        positionSubscribers += action
        pulseUpdate()
        return Subscription { unsubscribe(action) }
    }

    /** Observe mouse position in in px/second. */
    fun observeMouseVelocity(action: (Double) -> Unit): Subscription {
        velocitySubscribers += action
        pulseUpdate()
        return Subscription { unsubscribe(action) }
    }

    private fun unsubscribe(s: Any) {
        positionSubscribers.remove(s)
        velocitySubscribers.remove(s)
        pulseUpdate()
    }

    private fun pulseStart() {
        if (pulse==null)
            pulse = FxTimer(1000.0/pulseFrequency, -1) {
                val p = getMousePosition()
                positionSubscribers.forEach { it(p) }
                if (observeSpeed && lastPos!=null) {
                    val speed = p.distance(lastPos!!)*pulseFrequency
                    velocitySubscribers.forEach { it(speed) }
                }
                lastPos = p
            }.apply { start() }
    }

    private fun pulseStop() {
        pulse?.stop()
        pulse = null
    }

    private fun pulseUpdate() {
        val shouldObserveSpeed = !velocitySubscribers.isEmpty()
        val shouldObservePosition = !positionSubscribers.isEmpty() || shouldObserveSpeed
        observeSpeed = shouldObserveSpeed
        if (shouldObservePosition) pulseStart() else pulseStop()
    }

}