package sp.it.pl.core

import java.util.Objects
import javafx.geometry.Point2D
import javafx.scene.input.MouseButton
import javafx.scene.robot.Robot
import javafx.stage.Screen
import javafx.util.Duration
import kotlin.math.PI
import kotlin.math.atan2
import kotlinx.coroutines.delay
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.launch
import sp.it.util.async.executor.FxTimer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.materialize
import sp.it.util.collections.readOnly
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.on
import sp.it.util.units.div
import sp.it.util.units.seconds

/** API to access mouse. Use only on `JavaFX Application Thread` unless specified otherwise. */
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

   /** [Robot.mouseMove] */
   fun mouseMove(location: Point2D) {
      robot.mouseMove(location)
   }

   /** [Robot.mousePress] */
   fun mousePress(vararg buttons: MouseButton) {
      robot.mousePress(*buttons)
   }

   /** [Robot.mouseRelease] */
   fun mouseRelease(vararg buttons: MouseButton) {
      robot.mouseRelease(*buttons)
   }

   /** [Robot.mouseClick] */
   fun mouseClick(vararg buttons: MouseButton) {
      robot.mouseClick(*buttons)
   }

   /** [Robot.mouseWheel] */
   fun mouseWheel(wheelAmt: Int) {
      robot.mouseWheel(wheelAmt)
   }

   /** Observe mouse position in screen coordinates. */
   fun observeMousePosition(action: (Point2D) -> Unit): Subscription {
      failIfNotFxThread()
      positionSubscribers += action
      pulseUpdate()
      return Subscription { unsubscribe(action) }
   }

   /** Observe mouse position speed in px/second. */
   fun observeMouseVelocity(action: (MouseData) -> Unit): Subscription {
      failIfNotFxThread()
      velocitySubscribers += action
      pulseUpdate()
      return Subscription { unsubscribe(action) }
   }
   
   val screens = Screen.getScreens().readOnly()
   val primaryScreen: Screen
      get() = Screen.getPrimary()

   private fun unsubscribe(s: Any) {
      failIfNotFxThread()
      positionSubscribers.remove(s)
      velocitySubscribers.remove(s)
      pulseUpdate()
   }

   private fun pulseStart() {
      if (pulse==null)
         pulse = fxTimer(1.seconds/pulseFrequency, -1) {
            val pn = mousePosition
            val po = lastPos
            val ps = positionSubscribers.materialize()
            val vs = velocitySubscribers.materialize()
            ps.forEach { it(pn) }
            if (observeSpeed && po!=null) {
               val speed = pn.distance(po)*pulseFrequency
               val dir = if (speed==0.0) null else ((PI + atan2(pn.x-po.x, pn.y-po.y))*180/PI + 90.0).mod(360.0)
               vs.forEach { it(MouseData(speed, dir)) }
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

   /** Mouse movement vector */
   data class MouseData(val speed: Double, val dir: Double?)

   /**
    * Invokes [block] if during next [delayStop] mouse did not move.
    * During initial [delayStart] the mouse movement is ignored, this gives user time to stabilize mouse position.
    * The behavior is delayed not shorter than [delayStart].
    */
   @ThreadSafe
   fun onNextMouseMoveStop(delayStart: Duration, delayStop: Duration, block: (Boolean) -> Unit) {
      launch(FX) {
         delay(delayStart.toMillis().toLong())
         val p = mousePosition
         var c = false
         var d = Disposer()
         observeMousePosition { c = p!=mousePosition; d() } on d
         try {
            delay((delayStop.toMillis().toLong() - delayStart.toMillis().toLong()) max 0)
            block(c)
         } finally {
            d()
         }
      }
   }
}