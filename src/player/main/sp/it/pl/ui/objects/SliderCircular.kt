package sp.it.pl.ui.objects

import javafx.scene.CacheHint
import javafx.scene.Group
import javafx.scene.control.Slider
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.Arc
import javafx.scene.shape.ArcType
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sin
import sp.it.pl.main.APP
import sp.it.pl.main.F
import sp.it.util.access.v
import sp.it.util.animation.Anim
import sp.it.util.collections.observableList
import sp.it.util.math.clip
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachChanges
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.x
import sp.it.util.ui.xy
import sp.it.util.units.millis

/** Circular [Slider] with value always normalized to 0..1. */
class SliderCircular(val W: Double): StackPane() {

   /** Value, 0..1. Default 0. */
   val value = v(0.0)

   /** Represents [value] but changes only when [isValueChanging] is false. Default 0. */
   val valueSoft = Handler1<Double>()

   /** Value shown to user, 0..1. Mey differ from [value] due to [isAnimated] and [isValueChanging]. Default 0. */
   val valueShown = v(value.value)

   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)

   /** Whether user is changing the value. */
   val isValueChanging = v(false)

   /** Whether animation between value changes is enabled. Default true. */
   val isAnimated = v(true)

   /** Value animation interpolator, see [isAnimated]. */
   val valueAnimationInterpolator: F<Double, Double> = { sin(PI/2*it) }

   /** Angle in degrees where the value starts. Default 0.0. */
   val valueStartAngle = v(0.0)

   /** Whether the value grows in both directions, ranging from 0-180 degrees. Default true. */
   val valueSymmetrical = v(false)

   /** Same as [Slider.blockIncrement] */
   val blockIncrement = v(0.1)

   /** Similar to as [Slider.showTickMarks] and [Slider.snapToTicks]. Default empty. */
   val snaps = observableList<Double>()

   private val anim = Anim.anim(200.millis) { valueShown.value = valueAnimFrom + it*(valueAnimTo - valueAnimFrom) }.intpl { valueAnimationInterpolator(it) }
   private val animSuppressor = Suppressor()
   private var valueAnimFrom = value.value
   private var valueAnimTo = value.value

   init {
      styleClass += "slider-circular"
      editable sync { pseudoClassToggle("readonly", !editable.value) }
      prefSize = W/2.0 x W/2.0
      minSize = W/2.0 x W/2.0
      maxSize = W/2.0 x W/2.0
      isFocusTraversable = true
      isPickOnBounds = false
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.HOME) { decrementToMin() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.DOWN) { decrement() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.KP_DOWN) { decrement() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.LEFT) { decrement() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.KP_LEFT) { decrement() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.RIGHT) { increment() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.KP_RIGHT) { increment() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.UP) { increment() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.KP_UP) { increment() }
      onEventDown(KeyEvent.KEY_PRESSED, KeyCode.END) { incrementToMax() }

      value attach { if (!isValueChanging.value) valueSoft(it) }
      value attachChanges { _, n ->
         if (animSuppressor.isSuppressed) anim.stop()
         animSuppressor.suppressed {
            if (!isValueChanging.value) {
               valueAnimFrom = valueShown.value
               valueAnimTo = n
               if (isAnimated.value) anim.playFromStart() else valueShown.value = valueAnimTo
            }
         }
      }

      lay += Group().apply {
         children += Rectangle().apply {
            fill = null
            width = W
            height = W
            isCache = true
            isCacheShape = true
            cacheHint = CacheHint.ROTATE
         }
         children += Circle(W/4.0).apply {
            styleClass += "slider-circular-bgr"
            centerX = W/2.0
            centerY = W/2.0
            isCache = true
            isCacheShape = true
            cacheHint = CacheHint.ROTATE
            valueShown zip valueSymmetrical sync { (v, vs) -> rotate = if (vs) 180.0*v else 0.0 }
         }
         children += Circle(W/4.0).apply {
            styleClass += "slider-circular-frg"
            centerX = W/2.0
            centerY = W/2.0
            clip = Group().apply {
               scaleX = 2.0
               scaleY = 2.0
               children += Rectangle().apply {
                  fill = Color.TRANSPARENT
                  width = W
                  height = W
                  isCache = true
                  isCacheShape = true
                  cacheHint = CacheHint.ROTATE
               }
               children += Arc().apply {
                  this.type = ArcType.ROUND
                  this.centerX = W/2.0
                  this.centerY = W/2.0
                  this.radiusX = W/4.0
                  this.radiusY = W/4.0

                  isCache = true
                  isCacheShape = true
                  cacheHint = CacheHint.ROTATE

                  val updater = { _: Any? ->
                     length = 360.0*valueShown.value
                     startAngle = valueStartAngle.value - if (valueSymmetrical.value) 180.0*valueShown.value else 0.0
                  }
                  valueSymmetrical attach updater
                  valueStartAngle attach updater
                  valueShown attach updater
                  updater(Unit)
               }
            }

            fun updateFromMouse(e: MouseEvent, anim: Boolean = true) {
               val polarPos = (e.xy - (centerX x centerY))
               val angleRad = atan2(polarPos.x, polarPos.y) + PI + valueStartAngle.value*PI/180.0
               val vNorm = (angleRad/2.0/PI + 0.25).rem(1.0)
               val vRaw = when {
                  valueSymmetrical.value -> if (vNorm<=0.5) vNorm*2.0 else (1.0 - vNorm)*2.0
                  else -> vNorm
               }
               val v = vRaw.clip().snap(e)
               if (editable.value) {
                  if (isValueChanging.value && !anim) valueToAnimFalse(v)
                  else valueToAnimTrue(v)
               }
            }
            onEventDown(ScrollEvent.SCROLL) { e ->
               if (e.deltaY.sign>0) increment() else decrement()
               e.consume()
            }
            onEventDown(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY) {
               this@SliderCircular.requestFocus()
               if (editable.value) {
                  updateFromMouse(it, true)
                  isValueChanging.value = true
               }
            }
            onEventDown(MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY) {
               if (isValueChanging.value) {
                  isValueChanging.value = false
                  updateFromMouse(it, false)
               }
            }
            onEventDown(MouseEvent.MOUSE_DRAGGED, MouseButton.PRIMARY) {
               if (isValueChanging.value)
                  updateFromMouse(it, false)
            }
            onEventDown(MouseEvent.MOUSE_RELEASED, MouseButton.SECONDARY) {
               if (isValueChanging.value) {
                  isValueChanging.value = false
                  valueShownToActualAnimTrue()
               }
            }
            onEventDown(MouseEvent.MOUSE_CLICKED, MouseButton.SECONDARY, false) {
               if (it.isPrimaryButtonDown)
                  it.consume()
            }
            onEventDown(MouseEvent.MOUSE_PRESSED, MouseButton.SECONDARY) {
               if (isValueChanging.value) {
                  isValueChanging.value = false
                  valueShownToActualAnimTrue()
               }
            }
         }
      }
   }

   fun decrementToMin() {
      if (editable.value) value.setValueOf { 0.0 }
   }

   fun decrement() {
      if (editable.value) value.setValueOf { (it - blockIncrement.value).clip() }
   }

   fun increment() {
      if (editable.value) value.setValueOf { (it + blockIncrement.value).clip() }
   }

   fun incrementToMax() {
      if (editable.value) value.setValueOf { 1.0 }
   }

   private fun valueShownToActualAnimTrue() {
      valueAnimFrom = valueShown.value
      valueAnimTo = value.value
      if (isAnimated.value) anim.playFromStart() else valueShown.value = valueAnimTo
   }

   private fun valueShownToActualAnimFalse() {
      valueShown.value = value.value
   }

   private fun valueToAnimTrue(v: Double) {
      value.value = v
      if (!isValueChanging.value && value.value==v) valueSoft(v)
   }

   private fun valueToAnimFalse(v: Double) {
      animSuppressor.suppressing {
         valueShown.value = v
         value.value = v
         if (!isValueChanging.value && value.value==v) valueSoft(v)
      }
   }

   private fun Double.clip(): Double = clip(0.0, 1.0)

   private fun Double.snap(e: MouseEvent? = null): Double {
      val snap = e!=null && !e.isShiftDown && !e.isShortcutDown
      val snapBy = APP.ui.snapDistance.value/(2*PI*W/4.0)
      val snaps = if (snap) snaps else setOf()
      return snaps.minByOrNull { (it - this).absoluteValue }?.takeIf { (it - this).absoluteValue<=snapBy } ?: this
   }

}