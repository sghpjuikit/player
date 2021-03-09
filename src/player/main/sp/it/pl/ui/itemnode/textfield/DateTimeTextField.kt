package sp.it.pl.ui.itemnode.textfield

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import sp.it.pl.main.AppTexts
import sp.it.pl.ui.objects.picker.DateTimePickerContent
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

/** [ValueTextField] for [LocalDateTime]. */
class DateTimeTextField(locale: Locale = Locale.getDefault(), formatter: DateTimeFormatter): ValueTextField<LocalDateTime>({
   it?.net(formatter::format) ?: AppTexts.textNoVal
}) {
   private var locale = locale
   private var formatter = formatter
   private var popup: PopWindow? = null
   private var popupContent: DateTimePickerContent? = null
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
      val d = Disposer()
      val pc = popupContent ?: DateTimePickerContent(locale).apply {
         popupContent = this
         value.value = this@DateTimeTextField.value ?: LocalDateTime.now()
         value attach { valueChanging.suppressing { this@DateTimeTextField.value = it } } on d
         editable syncFrom this@DateTimeTextField.editableProperty() on d
      }

      val p = popup ?: PopWindow().apply {
         styleClass += "datetime-text-field-popup"
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