package sp.it.pl.ui.itemnode.textfield

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.input.Clipboard
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.SHORTCUT
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import kotlin.properties.Delegates.observable
import sp.it.pl.core.CoreMenus
import sp.it.pl.main.APP
import sp.it.pl.main.Key
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.textfield.DecoratedTextField
import sp.it.util.collections.setToOne
import sp.it.util.reactive.Handler1
import sp.it.util.reactive.onEventDown
import sp.it.util.text.keys
import sp.it.util.text.resolved
import sp.it.util.type.estimateRuntimeType
import sp.it.util.ui.copySelectedOrAll
import sp.it.util.ui.cutSelectedOrAll
import sp.it.util.ui.dsl
import sp.it.util.ui.show

/**
 * Customized [TextField] that displays a nullable object value. Normally a non-editable text
 * field that brings up a popup picker for its item type. Useful as an editor with value selection feature.
 *
 * In addition there is a dialog button calling implementation dependant item
 * chooser expected in form of a pop-up.
 *
 * @param <T> type of the value
 */
abstract class ValueTextField<T>(textValueConverter: (T?) -> String = APP.converter.ui::toS): DecoratedTextField() {

   /** Behavior executing when value changes */
   val onValueChange = Handler1<T?>()
   /** Value to string converter */
   val textValueConverter = textValueConverter
   /** Value */
   var value by observable<T?>(null) { _, ov, nv ->
      if (ov!=nv) {
         text = textValueConverter(nv)
         promptText = text
         onValueChange(nv)
      }
   }

   init {
      styleClass += STYLECLASS
      isEditable = false
      text = textValueConverter(null)
      promptText = text

      onEventDown(ContextMenuEvent.ANY) { it.consume() }
      onEventDown(MOUSE_CLICKED, SECONDARY) {
         fun MenuItem.disIf(condition: Boolean) = apply { isDisable = condition }
         val tf = this@ValueTextField
         ContextMenu().dsl {
            item("Undo (${keys(SHORTCUT, Key.Z)})") { tf.undo() }.disIf(!tf.isUndoable)
            item("Redo (${keys(SHORTCUT, SHIFT, Key.Z)})") { tf.redo() }.disIf(!tf.isRedoable)
            item("Cut (${keys(SHORTCUT, Key.X)})") { tf.cutSelectedOrAll() }
            item("Copy (${keys(SHORTCUT, Key.C)})") { tf.copySelectedOrAll() }
            item("Paste (${keys(SHORTCUT, Key.V)})") { tf.paste() }.disIf(!Clipboard.getSystemClipboard().hasString())
            item("Delete (${keys(DELETE)})") { tf.deleteText(tf.selection) }.disIf(tf.selectedText.isNullOrEmpty())
            separator()
            item("Select All (${keys("${SHORTCUT.resolved} + C")})") { tf.selectAll() }
            separator()
            menu("Text") {
               items {
                  CoreMenus.menuItemBuilders[tf.text]
               }
            }
            menu(tf.value?.estimateRuntimeType()?.toUi() ?: "Value") {
               items {
                  CoreMenus.menuItemBuilders[tf.value]
               }
            }
         }.show(this, it)
      }

      right setToOne ArrowDialogButton().apply {
         onEventDown(MOUSE_CLICKED, PRIMARY) { onDialogAction() }
      }
   }

   /** Behavior to be executed on dialog button click. Normally, on success it sets [value]. */
   protected abstract fun onDialogAction()

   companion object {
      const val STYLECLASS = "value-text-field"
   }

}