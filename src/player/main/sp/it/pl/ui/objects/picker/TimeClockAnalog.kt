package sp.it.pl.ui.objects.picker

import java.time.LocalTime
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.StackPane
import javafx.scene.transform.Rotate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import sp.it.util.access.toggleNext
import sp.it.util.access.togglePrevious
import sp.it.util.access.v
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.centre
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.ui.xy

/** Editable analog clock for [LocalTime]. */
class TimeClockAnalog: StackPane() {
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Time value */
   val value = v(LocalTime.now())
   /** Unit the arrow displays and user sets by dragging the arrow. Default [Unit.HOUR] */
   val unit = v(Unit.HOUR)
   /** Updates graphics to display [value] */
   val update = Handler1<LocalTime>()

   /** Circular background */
   val circle = stackPane {
      styleClass += "time-clock-analog-circle"

      val updateFromMouse = { e: MouseEvent ->
         val u = unit.value
         val polarPos = (e.xy - size/2.0)*(1.0 x -1.0)
         val angleRad = atan2(polarPos.x, polarPos.y) + PI
         val centerDist = polarPos distance (0 x 0)
         if (editable.value && centerDist<=width/2.0)
            value.setValueOf { u.setter(it, ((angleRad/2.0/PI*u.base.toDouble()).roundToInt() + u.base/2)%u.base) }
      }
      onEventDown(MOUSE_DRAGGED, PRIMARY) { updateFromMouse(it) }
      onEventDown(MOUSE_CLICKED, PRIMARY) { updateFromMouse(it) }
      onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { unit.value.adder(it, -e.deltaY.sign.toLong()) } }
   }

   /** The twelve hour labels */
   private val hourLabels = (1..12).map {
      label("$it") {
         styleClass += "time-clock-analog-hour-label"
         isMouseTransparent = true
      }
   }

   /** Arrow. Shared for all [unit] values. */
   private val arrow = stackPane {
      styleClass += "time-clock-analog-arrow"
      isMouseTransparent = true

      update += {
         val u = unit.value
         val angleDeg = 360/u.base*u.getter(it)
         transforms setToOne Rotate(angleDeg.toDouble() - 90, 0.0, height/2.0)
      }
   }

   /** Middle dot */
   private val middleDot = stackPane {
      styleClass += "time-clock-analog-middle-dot"
   }

   /** Displays and toggles [unit] */
   private val unitLabel = label {
      styleClass += "time-clock-analog-unit-label"
      unit sync { text = it.nameUi }
      onEventDown(MOUSE_CLICKED, PRIMARY) { unit.toggleNext() }
      onEventDown(MOUSE_CLICKED, SECONDARY) { unit.togglePrevious() }
   }

   /** Displays and toggles [Mode] */
   private val modeLabel = label {
      styleClass += "time-clock-analog-mode-label"
      update += { text = if (it.hour<12) "AM" else "PM" }
      onEventDown(MOUSE_CLICKED, PRIMARY) { if (editable.value) value.setValueOf { it.withHour((it.hour + 12)%24) } }
   }

   init {
      styleClass += "time-clock-analog"
      editable sync { pseudoClassToggle("readonly", !editable.value) }
      unit attach { update(value.value) }
      unit sync {
         Unit.values().forEach { u -> arrow.pseudoClassChanged(u.nameCss, u==it) }
      }

      lay += circle
      lay += hourLabels
      lay += arrow
      lay += middleDot
      lay += unitLabel
      lay += modeLabel

      value sync { update(value.value) }
   }

   override fun layoutChildren() {
      super.layoutChildren()

      val radius = circle.width/2.0
      val posDist = radius*0.8
      hourLabels.forEachIndexed { i, l ->
         val a = 2.0*PI/12.0*(i.toDouble() + 1 - 3)
         l.layoutX = circle.boundsInParent.centerX + posDist*cos(a) - l.width/2.0
         l.layoutY = circle.boundsInParent.centerY + posDist*sin(a) - l.height/2.0
      }

      val centre = circle.boundsInParent.centre
      middleDot.relocate(centre.x - middleDot.width/2.0, centre.y - middleDot.height/2.0)
      arrow.relocate(centre.x, centre.y - arrow.height/2.0)
      unitLabel.relocate(centre.x + radius/2.5*cos(PI/4.0) - unitLabel.width/2.0, centre.y - radius/2.5*sin(PI/4.0) - unitLabel.height/2.0)
      modeLabel.relocate(centre.x + radius/2.5*cos(-PI/4.0) - unitLabel.width/2.0, centre.y - radius/2.5*sin(-PI/4.0) - unitLabel.height/2.0)
   }

   enum class Unit(val nameUi: String, val nameCss: String, val base: Int, val getter: (LocalTime) -> Int, val setter: (LocalTime, Int) -> LocalTime, val adder: (LocalTime, Long) -> LocalTime) {
      HOUR("H", "hour", 12, LocalTime::getHour, LocalTime::withHour, LocalTime::plusHours),
      MINUTE("M", "minute", 60, LocalTime::getMinute, LocalTime::withMinute, LocalTime::plusMinutes),
      SECOND("S", "second", 60, LocalTime::getSecond, LocalTime::withSecond, LocalTime::plusSeconds)
   }

   enum class Mode { AM, PM }
}