package sp.it.pl.ui.itemnode.textfield

import java.time.format.DateTimeFormatter as Formatter
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Locale
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.ui.objects.picker.DatePickerContent
import sp.it.pl.ui.objects.window.NodeShow
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.functional.net
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane

/** [ValueTextField] for [LocalDate]. */
class DateTextField(locale: Locale = Locale.getDefault(), formatter: Formatter): ValueTextField<LocalDate>({ it?.net(formatter::format) ?: textNoVal }) {
   private var locale = locale
   private var formatter = formatter
   private var popup: PopWindow? = null
   private var popupContent: DatePickerContent? = null
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
      val d = Disposer()
      val pc = popupContent ?: DatePickerContent(locale).apply {
         popupContent = this
         value.value = this@DateTextField.value ?: LocalDate.now()
         value attach { valueChanging.suppressing { this@DateTextField.value = it } } on d
         editable syncFrom this@DateTextField.editableProperty() on d
      }

      val p = popup ?: PopWindow().apply {
         styleClass += "date-text-field-popup"
         popup = this
         userMovable.value = false
         userResizable.value = false
         headerVisible.value = false
         isAutohide.value = true
         isEscapeHide.value = true
         content.value = stackPane {
            lay += pc
         }
         onHiding += {
            d()
            popupContent = null
            popup = null
         }
      }
      p.show(NodeShow.DOWN_LEFT(this))
   }

}