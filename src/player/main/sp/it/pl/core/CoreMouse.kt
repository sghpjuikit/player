package sp.it.pl.core

import javafx.geometry.Point2D
import javafx.scene.robot.Robot
import javafx.stage.Screen
import kotlin.math.PI
import kotlin.math.atan2
import sp.it.util.async.executor.FxTimer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.readOnly
import sp.it.util.reactive.Subscription
import sp.it.util.units.div
import sp.it.util.units.seconds

/** Provides access to mouse position and mouse speed. By default, lazy, i.e., consumes resources only if observed. */
object CoreMouse: Core {
   private const val pulseFrequency = 10 // Hz
   private var pulse: FxTimer? = null
   private var lastPos: Point2D? = null
   private var observeSpeed = false
   private val positionSubscribers = HashSet<(Point2D) -> Unit>()
   private val velocitySubscribers = HashSet<(MouseData) -> Unit>()
   private val robot = Robot()

   /** @return mouse position in screen coordinates */
   val mousePosition: Point2D get() = robot.mousePosition

   /** Observe mouse position in screen coordinates. */
   fun observeMousePosition(action: (Point2D) -> Unit): Subscription {
      positionSubscribers += action
      pulseUpdate()
      return Subscription { unsubscribe(action) }
   }

   /** Observe mouse position speed in px/second. */
   fun observeMouseVelocity(action: (MouseData) -> Unit): Subscription {
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
            val pn = mousePosition
            val po = lastPos
            positionSubscribers.forEach { it(pn) }
            if (observeSpeed && po!=null) {
               val speed = pn.distance(po)*pulseFrequency
               val dir = if (speed==0.0) null else ((PI + atan2(pn.x-po.x, pn.y-po.y))*180/PI + 90.0).mod(360.0)
               velocitySubscribers.forEach { it(MouseData(speed, dir)) }
            }
            lastPos = pn
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

   data class MouseData(val speed: Double, val dir: Double?)
}