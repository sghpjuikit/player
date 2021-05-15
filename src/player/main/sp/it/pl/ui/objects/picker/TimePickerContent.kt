package sp.it.pl.ui.objects.picker

import java.time.LocalTime
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import sp.it.util.access.v
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassToggle

/** Content for picking [LocalTime] */
class TimePickerContent: VBox() {
   /** Whether user can change [value] through ui. Only if true. Default true. */
   val editable = v(true)
   /** Time value */
   val value = v<LocalTime>(LocalTime.now())
   /** Digital clock */
   val clockDigital = TimeClockDigital()
   /** Analog clock */
   val clockAnalog = TimeClockAnalog()
   /** Suppresses value changes */
   private val valueChangingDigital = Suppressor()
   /** Suppresses value changes */
   private val valueChangingAnalog = Suppressor()

   init {
      styleClass += "time-picker-content"
      editable sync { pseudoClassToggle("readonly", !editable.value) }
      editable syncTo clockDigital.editable
      editable syncTo clockAnalog.editable

      clockDigital.value attach { valueChangingDigital.suppressing { value.value = it } }
      clockAnalog.value attach { valueChangingAnalog.suppressing { value.value = it } }
      value sync {
         valueChangingAnalog.suppressed { clockAnalog.value.value = it }
         valueChangingDigital.suppressed { clockDigital.value.value = it }
      }

      lay += clockDigital
      lay(ALWAYS) += clockAnalog
   }
}