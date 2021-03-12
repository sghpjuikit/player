package sp.it.pl.ui.itemnode.textfield

import javafx.scene.layout.StackPane
import sp.it.pl.ui.objects.icon.Icon

/** Button for calling dialogs, from within [javafx.scene.control.TextField]. */
class ArrowDialogButton: StackPane() {
   init {
      // Non-icon pure css implementation, that looks exactly like other javaFx dialog
      // children +=  Region().apply {
      //     styleClass += "dialog-button"
      //     styleClass += "value-text-field-arrow"
      //     setMinSize(0.0, 0.0)
      //     setPrefSize(7.0, 6.0)
      //     setMaxSize(7.0, 6.0)
      // }
      // setPrefSize(22.0, 22.0)

      children += Icon().styleclass(STYLECLASS)
   }

   companion object {
      const val STYLECLASS = "value-text-field-arrow"
   }
}