package sp.it.pl.ui

import javafx.scene.control.RadioButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.VBox
import sp.it.util.access.v
import sp.it.util.access.vx
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.ui.lay

@Suppress("UNCHECKED_CAST")
class ValueRadioButtonGroup<T>(val initialValue: T, val values: List<T>, val customizer: RadioButton.(T) -> Unit = {}) : VBox() {
   val isEditable = v(true)
   val value = vx(initialValue)

   init {
      styleClass += "toggle-radio-group"
      val group = ToggleGroup()
      lay += values.map { v ->
         RadioButton().apply {
            text = v.toString()
            userData = v
            customizer(v)
            toggleGroup = group
            isSelected = v == initialValue
            onEventUp(MOUSE_PRESSED) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(MOUSE_RELEASED) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_PRESSED, SPACE, false) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_PRESSED, ENTER, false) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_RELEASED, SPACE, false) { if (isSelected || !isEditable.value) it.consume() }
            onEventUp(KEY_RELEASED, ENTER, false) { if (isSelected || !isEditable.value) it.consume() }
         }
      }
      value.attach { t -> group.selectToggle(group.toggles.find { it.userData== t }!!) }
      group.selectedToggleProperty() attach { value.value = it.userData as T }
   }
}