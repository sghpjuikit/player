package sp.it.pl.ui.objects.picker

import java.time.LocalTime
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Separator
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.HBox
import kotlin.math.sign
import sp.it.pl.ui.objects.picker.TimeClockPrecision.HOUR
import sp.it.pl.ui.objects.picker.TimeClockPrecision.MINUTE
import sp.it.pl.ui.objects.picker.TimeClockPrecision.SECOND
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.enumConverter
import sp.it.util.access.svMetaData
import sp.it.util.access.sv
import sp.it.util.access.v
import sp.it.util.collections.setTo
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.text
import sp.it.util.ui.vBox

/** Editable digital clock for [LocalTime]. */
class TimeClockDigitalIos: HBox() {
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Time value */
   val value = v<LocalTime>(LocalTime.now())
   /** Updates graphics to display [value] */
   val update = Handler1<LocalTime>()
   /** The smallest displayed unit */
   val precisionMin by sv(PRECISION_MIN)
   /** The largest displayed unit */
   val precisionMax by sv(PRECISION_MAX)

   init {
      styleClass += "time-clock-digital-ios"
      editable sync { pseudoClassToggle("readonly", !editable.value) }

      val nodeHour = vBox {
         (-1..1).forEach { by ->
            lay += text("") {
               styleClass += "time-clock-digital-ios-text"
               styleClass += "time-clock-digital-ios-text-hour"
               pseudoClassChanged("secondary", by!=0)
               update += { text = "%2d".format((it.hour + by + 24)%24).padStart(2, ' ') }
            }
         }
         onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusHours(-e.deltaY.sign.toLong()) } }
      }
      val nodeMin = vBox {
         (-1..1).forEach { by ->
            lay += text("") {
               styleClass += "time-clock-digital-ios-text"
               styleClass += "time-clock-digital-ios-text-minute"
               pseudoClassChanged("secondary", by!=0)
               update += { text = "%02d".format((it.minute + by + 60)%60) }
            }
         }
         onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusMinutes(-e.deltaY.sign.toLong()) } }
      }
      val nodeSec = vBox {
         (-1..1).forEach { by ->
            lay += text("") {
               styleClass += "time-clock-digital-ios-text"
               styleClass += "time-clock-digital-ios-text-second"
               pseudoClassChanged("secondary", by!=0)
               update += { text = "%02d".format((it.second + by + 60)%60) }
            }
         }
         onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusSeconds(-e.deltaY.sign.toLong()) } }
      }

      fun sep() = Separator(VERTICAL).apply {
         styleClass += "time-clock-digital-ios-separator"
      }

      precisionMin zip precisionMax sync { (min, max) ->
         children setTo listOf(HOUR to nodeHour, MINUTE to nodeMin, SECOND to nodeSec)
            .filter { it.first.size in min.size..max.size }
            .flatMapIndexed { i, it -> listOfNotNull(if (i==0) null else sep(), it.second) }
      }

      value sync { update(value.value) }
   }

   override fun getCssMetaData() = classCssMetaData

   companion object: StyleableCompanion() {
      val PRECISION_MIN by svMetaData<TimeClockDigitalIos, TimeClockPrecision>("-fx-precision-min", enumConverter(), SECOND, TimeClockDigitalIos::precisionMin)
      val PRECISION_MAX by svMetaData<TimeClockDigitalIos, TimeClockPrecision>("-fx-precision-max", enumConverter(), HOUR, TimeClockDigitalIos::precisionMax)
   }
}