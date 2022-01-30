package sp.it.pl.ui.objects.picker

import java.time.LocalDateTime
import java.time.format.TextStyle.FULL
import java.util.Locale
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.layout.VBox
import sp.it.pl.main.emScaled
import sp.it.util.access.v
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.sync
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.text

/** Digital clock for [LocalDateTime] in Windows 8 Start Screen style. */
class DateTimeClockWin(locale: Locale = Locale.getDefault()): VBox() {
   /** Locale */
   val locale = locale
   /** Time value */
   val value = v<LocalDateTime>(LocalDateTime.now())
   /** Updates graphics to display [value] */
   val update = Handler1<LocalDateTime>()

   init {
      styleClass += "date-time-clock-win"

      lay += hBox(15.emScaled, BOTTOM_RIGHT) {
         lay += text {
            styleClass += "date-time-clock-win-text"
            styleClass += "date-time-clock-win-text-daypart"
            update += { text = if (value.value.hour<12) "AM" else "PM" }
         }
         lay += text {
            styleClass += "date-time-clock-win-text"
            styleClass += "date-time-clock-win-text-hourminute"
            update += { text = "%d:%02d".format(value.value.hour%12, value.value.minute) }
         }
      }
      lay += text {
         styleClass += "date-time-clock-win-text"
         styleClass += "date-time-clock-win-text-date"
         update += { text = "%s, %s %d".format(value.value.dayOfWeek.getDisplayName(FULL, locale), value.value.month.getDisplayName(FULL, locale), value.value.dayOfMonth) }
      }

      value sync { update(value.value) }
   }
}