package sp.it.pl.ui.objects

import javafx.scene.Cursor.HAND
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.util.reactive.onEventDown

/** Install on primary button mouse click action and `HAND` cursor to indicate label is clickable */
fun Label.installClickable(onClick: () -> Unit) {
   cursor = HAND
   onEventDown(MOUSE_CLICKED, PRIMARY, true) { onClick() }
}