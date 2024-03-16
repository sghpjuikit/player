package sp.it.pl.ui

import javafx.beans.value.WritableValue
import javafx.scene.control.ToggleButton
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.HBox
import sp.it.pl.main.toUi
import sp.it.util.access.V
import sp.it.util.access.v
import sp.it.util.access.vx
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventUp
import sp.it.util.ui.lay

@Suppress("UNCHECKED_CAST")
class ValueToggleButtonGroup<T>(val value: V<T>, val initialValue: T = value.value, val values: List<T>, val customizer: ToggleButton.(T) -> Unit = {}): HBox() {

   val isEditable = v(true)

   init {
      styleClass += "toggle-button-group"
      val group = ToggleGroup()
      lay += values.mapIndexed { i, v ->
         ToggleButton().apply {
            text = v.toUi()
            userData = v
            customizer(v)
            toggleGroup = group
            isSelected = v==initialValue
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

   companion object {

      fun <T> ofValue(initialValue: T, values: List<T>, customizer: ToggleButton.(T) -> Unit = {}): ValueToggleButtonGroup<T> =
         ValueToggleButtonGroup(vx(initialValue), initialValue, values, customizer)

      fun <T> ofObservableValue(value: V<T>, values: List<T>, customizer: ToggleButton.(T) -> Unit = {}): ValueToggleButtonGroup<T> =
         ValueToggleButtonGroup(value, value.value, values, customizer)

   }
}