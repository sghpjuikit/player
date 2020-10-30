package sp.it.pl.core

import javafx.geometry.Point2D
import javafx.scene.robot.Robot
import javafx.stage.Screen
import sp.it.util.async.executor.FxTimer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.readOnly
import sp.it.util.reactive.Subscription
import sp.it.util.units.div
import sp.it.util.units.seconds
import java.util.HashSet

/** Provides access to mouse position and mouse speed. By default lazy, i.e., consumes resources only if observed. */
object CoreMouse: Core {
   private const val pulseFrequency = 10 // Hz
   private var pulse: FxTimer? = null
   private var lastPos: Point2D? = null
   private var observeSpeed = false
   private val positionSubscribers = HashSet<(Point2D) -> Unit>()
   private val velocitySubscribers = HashSet<(Double) -> Unit>()
   private val robot = Robot()

   /** @return mouse position in screen coordinates */
   val mousePosition: Point2D get() = robot.mousePosition

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

   val screens = Screen.getScreens().readOnly()
   val primaryScreen: Screen
      get() = Screen.getPrimary()

   private fun unsubscribe(s: Any) {
      positionSubscribers.remove(s)
      velocitySubscribers.remove(s)
      pulseUpdate()
   }

   private fun pulseStart() {
      if (pulse==null)
         pulse = fxTimer(1.seconds/pulseFrequency, -1) {
            val p = mousePosition
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
      val shouldObserveSpeed = velocitySubscribers.isNotEmpty()
      val shouldObservePosition = positionSubscribers.isNotEmpty() || shouldObserveSpeed
      observeSpeed = shouldObserveSpeed
      if (shouldObservePosition) pulseStart() else pulseStop()
   }

}