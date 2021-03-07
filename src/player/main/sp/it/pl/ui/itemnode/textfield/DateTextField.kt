package sp.it.pl.ui.itemnode.textfield

import java.time.format.DateTimeFormatter as Formatter
import java.time.LocalDate
import java.time.format.DateTimeParseException
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.util.functional.net
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.suppressed

/** [ValueTextField] for [LocalDate]. */
class DateTextField(formatter: Formatter): ValueTextField<LocalDate>({ it?.net(formatter::format) ?: textNoVal }) {
   private var formatter = formatter
   private var valueChanging = Suppressor()

   init {
      styleClass += "date-text-field"
      isEditable = true
      textProperty() attach {
         valueChanging.suppressed {
            try {
               value = LocalDate.parse(it, formatter)
            } catch (e: DateTimeParseException) {
            }
         }
      }
   }

   override fun onDialogAction() {
      // TODO
   }

}