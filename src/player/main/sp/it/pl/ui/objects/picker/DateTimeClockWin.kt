package sp.it.pl.ui.objects.picker

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle.FULL
import java.util.Locale
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.scene.layout.VBox
import sp.it.pl.main.APP
import sp.it.pl.main.emScaled
import sp.it.util.access.v
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.map
import sp.it.util.reactive.sync
import sp.it.util.reactive.zip
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.text

/** Digital clock for [ZonedDateTime] in Windows 8 Start Screen style. */
class DateTimeClockWin(locale: Locale = Locale.getDefault(), zoneId: ZoneId = APP.timeZone.value): VBox() {
   /** Locale */
   val locale = locale
   /** Time zone */
   val zoneId = v<ZoneId>(zoneId)
   /** Updates graphics to display [value] */
   val update = Handler1<ZonedDateTime>()
   /** Time value */
   val value = v<ZonedDateTime>(ZonedDateTime.now())
   /** Time value */
   private val _value = value zip this.zoneId map { (v, z) -> v.withZoneSameInstant(z) }

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
            update += { text = "%d:%02d".format(_value.value.hour%12, _value.value.minute) }
         }
      }
      lay += text {
         styleClass += "date-time-clock-win-text"
         styleClass += "date-time-clock-win-text-date"
         update += { text = "%s, %s %d".format(_value.value.dayOfWeek.getDisplayName(FULL, locale), _value.value.month.getDisplayName(FULL, locale), _value.value.dayOfMonth) }
      }

      _value sync { update(_value.value) }
   }
}