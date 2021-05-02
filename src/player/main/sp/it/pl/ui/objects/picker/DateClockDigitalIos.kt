package sp.it.pl.ui.objects.picker

import java.time.LocalDate
import javafx.geometry.Orientation.VERTICAL
import javafx.scene.control.Separator
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.HBox
import kotlin.math.sign
import sp.it.pl.ui.objects.picker.TimeClockPrecision.DAY
import sp.it.pl.ui.objects.picker.TimeClockPrecision.MONTH
import sp.it.pl.ui.objects.picker.TimeClockPrecision.YEAR
import sp.it.util.access.StyleableCompanion
import sp.it.util.access.enumConverter
import sp.it.util.access.sv
import sp.it.util.access.svMetaData
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

/** Editable digital clock for [LocalDate]. */
class DateClockDigitalIos: HBox() {
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Time value */
   val value = v(LocalDate.now())
   /** Updates graphics to display [value] */
   val update = Handler1<LocalDate>()
   /** The smallest displayed unit */
   val precisionMin by sv(PRECISION_MIN)
   /** The largest displayed unit */
   val precisionMax by sv(PRECISION_MAX)

   init {
      styleClass += "date-clock-digital-ios"
      editable sync { pseudoClassToggle("readonly", !editable.value) }

      val nodeYear = vBox {
         (-1..1).forEach { by ->
            lay += text("") {
               styleClass += "date-clock-digital-ios-text"
               styleClass += "date-clock-digital-ios-text-year"
               pseudoClassChanged("secondary", by!=0)
               update += { text = "%2d".format(it.year + by).padStart(4, ' ') }
            }
         }
         onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusYears(-e.deltaY.sign.toLong()) } }
      }
      val nodeMonth = vBox {
         (-1..1).forEach { by ->
            lay += text("") {
               styleClass += "date-clock-digital-ios-text"
               styleClass += "date-clock-digital-ios-text-month"
               pseudoClassChanged("secondary", by!=0)
               update += { text = "%02d".format((it.monthValue + by + 12)%12) }
            }
         }
         onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusMonths(-e.deltaY.sign.toLong()) } }
      }
      val nodeDay = vBox {
         (-1..1).forEach { by ->
            lay += text("") {
               styleClass += "date-clock-digital-ios-text"
               styleClass += "date-clock-digital-ios-text-day"
               pseudoClassChanged("secondary", by!=0)
               update += { text = "%02d".format((it.dayOfMonth + by + it.lengthOfMonth())%it.lengthOfMonth()) }
            }
         }
         onEventDown(SCROLL) { e -> if (editable.value) value.setValueOf { it.plusDays(-e.deltaY.sign.toLong()) } }
      }

      fun sep() = Separator(VERTICAL).apply {
         styleClass += "date-clock-digital-ios-separator"
      }

      precisionMin zip precisionMax sync { (min, max) ->
         children setTo listOf(YEAR to nodeYear, MONTH to nodeMonth, DAY to nodeDay)
            .filter { it.first.size in min.size..max.size }
            .flatMapIndexed { i, it -> listOfNotNull(if (i==0) null else sep(), it.second) }
      }

      value sync { update(value.value) }
   }

   override fun getCssMetaData() = classCssMetaData

   companion object: StyleableCompanion() {
      val PRECISION_MIN by svMetaData<DateClockDigitalIos, TimeClockPrecision>("-fx-precision-min", enumConverter(), MONTH, DateClockDigitalIos::precisionMin)
      val PRECISION_MAX by svMetaData<DateClockDigitalIos, TimeClockPrecision>("-fx-precision-max", enumConverter(), DAY, DateClockDigitalIos::precisionMax)
   }
}