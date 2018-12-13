package sp.it.pl.gui.itemnode.textfield

import javafx.scene.layout.StackPane
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.main.IconFA

/** Button for calling dialogs, from within [javafx.scene.control.TextField]. */
class ArrowDialogButton: StackPane() {
    init {
        // Non-icon pure css implementation, that looks exactly like other javaFx dialog
        // children +=  Region().apply {
        //     styleClass += "dialog-button"
        //     setMinSize(0.0, 0.0)
        //     setPrefSize(7.0, 6.0)
        //     setMaxSize(7.0, 6.0)
        // }
        // setPrefSize(22.0, 22.0)

        children += Icon(IconFA.CARET_DOWN, 7.0).scale(2.0).tooltip("Open dialog")
    }
}