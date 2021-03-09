package sp.it.pl.ui.objects.picker

import java.time.LocalTime
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import kotlin.math.sign
import sp.it.util.access.v
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.text
import sp.it.util.ui.textFlow

/** Editable digital clock for [LocalTime]. */
class TimeClockDigital: StackPane() {
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Time value */
   val value = v(LocalTime.now())
   /** Updates graphics to display [value] */
   val update = Handler1<LocalTime>()

   init {
      styleClass += "time-clock-digital"
      editable sync { pseudoClassToggle("readonly", !editable.value) }

      lay += textFlow {
         textAlignment = TextAlignment.CENTER

         lay += text("") {
            styleClass += "time-clock-digital-text"
            styleClass += "time-clock-digital-text-hour"
            update += { text = "%2d".format(it.hour%12).padStart(2, ' ') }
            onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusHours(-e.deltaY.sign.toLong()) } }
         }
         lay += text(":") {
            styleClass += "time-clock-digital-text"
         }
         lay += text("") {
            styleClass += "time-clock-digital-text"
            styleClass += "time-clock-digital-text-minute"
            update += { text = "%02d".format(it.minute) }
            onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusMinutes(-e.deltaY.sign.toLong()) } }
         }
         lay += text(":") {
            styleClass += "time-clock-digital-text"
         }
         lay += text("") {
            styleClass += "time-clock-digital-text"
            styleClass += "time-clock-digital-text-second"
            update += { text = "%02d".format(it.second) }
            onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusSeconds(-e.deltaY.sign.toLong()) } }
         }
         lay += text {
            styleClass += "time-clock-digital-text"
            styleClass += "time-clock-digital-text-mode"
            update += { text = if (it.hour<12) "AM" else "PM" }
            val updateFromMouse = { if (editable.value) value.setValueOf { it.withHour((it.hour + 12)%24) } }
            onEventDown(MOUSE_CLICKED, PRIMARY) { updateFromMouse() }
            onEventDown(SCROLL) { updateFromMouse() }
         }
      }

      value sync { update(value.value) }
   }
}