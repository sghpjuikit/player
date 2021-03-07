package sp.it.pl.ui.itemnode.textfield

import java.time.LocalTime
import sp.it.pl.ui.objects.time.TimePickerContent
import sp.it.pl.ui.objects.window.NodeShow
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane

/** [ValueTextField] for [LocalTime] using [TimePickerContent]. */
class TimeTextField: ValueTextField<LocalTime>() {
   private var popup: PopWindow? = null
   private var popupContent: TimePickerContent? = null

   init {
      styleClass += "time-text-field"
      isEditable = true
   }

   override fun onDialogAction() {
      val d = Disposer()
      val pc = popupContent ?: TimePickerContent().apply {
         popupContent = this
         value.value = this@TimeTextField.value ?: LocalTime.now()
         value attach { this@TimeTextField.value = it } on d
         editable syncFrom this@TimeTextField.editableProperty() on d
      }

      val p = popup ?: PopWindow().apply {
         styleClass += "time-text-field-popup"
         popup = this
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