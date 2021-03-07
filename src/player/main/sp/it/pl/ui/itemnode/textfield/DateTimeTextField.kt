package sp.it.pl.ui.itemnode.textfield

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import sp.it.pl.main.AppTexts
import sp.it.util.functional.net
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.suppressed

/** [ValueTextField] for [LocalDateTime]. */
class DateTimeTextField(formatter: DateTimeFormatter): ValueTextField<LocalDateTime>({
   it?.net(formatter::format) ?: AppTexts.textNoVal
}) {
   private var formatter = formatter
   private var valueChanging = Suppressor()

   init {
      styleClass += "datetime-text-field"
      isEditable = true
      textProperty() attach {
         valueChanging.suppressed {
            try {
               value = LocalDateTime.parse(it, formatter)
            } catch (e: DateTimeParseException) {
            }
         }
      }
   }

   override fun onDialogAction() {
      // TODO
   }

}