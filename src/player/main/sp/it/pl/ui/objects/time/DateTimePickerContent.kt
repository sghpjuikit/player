package sp.it.pl.ui.objects.time

import java.time.LocalDateTime
import java.util.Locale
import javafx.scene.layout.HBox
import sp.it.util.access.v
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassToggle

/** Content for picking [LocalDateTime] */
class DateTimePickerContent(locale: Locale = Locale.getDefault()): HBox() {
   val locale = locale
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Time value */
   val value = v(LocalDateTime.now())
   /** Digital clock */
   val dateContent = DatePickerContent(locale)
   /** Analog clock */
   val timeContent = TimePickerContent()
   /** Suppresses value changes */
   private val valueChangingDigital = Suppressor()
   /** Suppresses value changes */
   private val valueChangingAnalog = Suppressor()

   init {
      styleClass += "datetime-picker-content"
      editable sync { pseudoClassToggle("readonly", !editable.value) }
      editable syncTo dateContent.editable
      editable syncTo timeContent.editable

      dateContent.value attach { valueChangingDigital.suppressing { value.setValueOf { v -> v.withYear(it.year).withMonth(it.month.value).withDayOfMonth(it.dayOfMonth) } } }
      timeContent.value attach { valueChangingAnalog.suppressing { value.setValueOf { v -> v.withHour(it.hour).withMinute(it.minute).withSecond(it.second).withNano(it.nano) } } }
      value sync {
         valueChangingAnalog.suppressed { dateContent.value.value = it.toLocalDate() }
         valueChangingDigital.suppressed { timeContent.value.value = it.toLocalTime() }
      }

      lay += dateContent
      lay += timeContent
   }
}