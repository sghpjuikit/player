package sp.it.pl.ui.objects.textfield

import java.time.format.DateTimeFormatter as Formatter
import java.time.LocalTime
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.ui.objects.picker.TimePickerContent
import sp.it.pl.ui.objects.window.NodeShow
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane

/** [ValueTextField] for [LocalTime] using [TimePickerContent]. */
class TimeTextField(initialValue: LocalTime? = null, formatter: Formatter): ValueTextField<LocalTime>(initialValue, { it?.net(formatter::format) ?: textNoVal }) {
   private var formatter = formatter
   private var popup: PopWindow? = null
   private var popupContent: TimePickerContent? = null
   private var valueChanging = Suppressor()

   init {
      styleClass += "time-text-field"
      isEditable = true
      textProperty() attach {
         valueChanging.suppressed {
            runTry {
               value = LocalTime.parse(it, formatter)
            }
         }
      }
   }

   override fun onDialogAction() {
      val d = Disposer()
      val pc = popupContent ?: TimePickerContent().apply {
         popupContent = this
         value.value = this@TimeTextField.value ?: LocalTime.now()
         value attach { valueChanging.suppressing { this@TimeTextField.value = it } } on d
         editable syncFrom this@TimeTextField.editableProperty() on d
      }

      val p = popup ?: PopWindow().apply {
         styleClass += "time-text-field-popup"
         popup = this
         userMovable.value = false
         userResizable.value = false
         headerVisible.value = false
         isAutohide.value = false
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