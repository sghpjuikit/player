package sp.it.pl.ui

import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.HBox
import sp.it.util.access.v
import sp.it.util.access.vx
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.ui.lay

@Suppress("UNCHECKED_CAST")
class ValueToggleButtonGroup<T>(val initialValue: T, val values: List<T>, val customizer: ToggleButton.(T) -> Unit): HBox() {
   val isEditable = v(true)
   val value = vx(initialValue)

   init {
      styleClass += "toggle-group"
      val group = ToggleGroup()
      values.forEachIndexed { i, it ->
         lay += ToggleButton().apply {
            userData = it
            customizer(it)
            toggleGroup = group
            isSelected = it==initialValue
            onEventUp(MOUSE_PRESSED) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(MOUSE_RELEASED) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_PRESSED, SPACE, false) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_PRESSED, ENTER, false) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_RELEASED, SPACE, false) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_RELEASED, ENTER, false) { if (isSelected || !isEditable.value) it.consume() }
            if (values.size<=1) styleClass += when (i) { 0 -> "left-pill"; values.lastIndex -> "right-pill"; else -> "center-pill" }
         }
      }
      value.attach { t -> group.selectToggle(group.toggles.find { it.userData== t }!!) }
      group.selectedToggleProperty() attach { value.value = it.userData as T }
   }
}