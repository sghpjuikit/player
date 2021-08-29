package sp.it.pl.ui

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.SHORTCUT
import javafx.scene.input.MouseEvent
import sp.it.pl.core.CoreMenus
import sp.it.pl.main.Key
import sp.it.pl.main.toUi
import sp.it.util.text.keys
import sp.it.util.text.resolved
import sp.it.util.type.estimateRuntimeType
import sp.it.util.ui.copySelectedOrAll
import sp.it.util.ui.cutSelectedOrAll
import sp.it.util.ui.dsl
import sp.it.util.ui.show

fun showContextMenu(tf: TextInputControl, event: MouseEvent, textGetter: (() -> String?)?, valueGetter: (() -> Any?)?) {
   fun MenuItem.disIf(condition: Boolean) = apply { isDisable = condition }

   ContextMenu().apply {
      dsl {
         item("Undo (${keys(SHORTCUT, Key.Z)})") { tf.undo() }.disIf(!tf.isUndoable || !tf.isEditable)
         item("Redo (${keys(SHORTCUT, SHIFT, Key.Z)})") { tf.redo() }.disIf(!tf.isRedoable || !tf.isEditable)
         item("Cut (${keys(SHORTCUT, Key.X)})") { tf.cutSelectedOrAll() }.disIf(!tf.isEditable)
         item("Copy (${keys(SHORTCUT, Key.C)})") { tf.copySelectedOrAll() }
         item("Paste (${keys(SHORTCUT, Key.V)})") { tf.paste() }.disIf(!tf.isEditable || !Clipboard.getSystemClipboard().hasString())
         item("Delete (${keys(DELETE)})") { tf.deleteText(tf.selection) }.disIf(!tf.isEditable || tf.selectedText.isNullOrEmpty())
         separator()
         item("Select All (${keys("${SHORTCUT.resolved} + C")})") { tf.selectAll() }

         if (textGetter!=null || valueGetter!=null)
            separator()

         if (textGetter!=null) {
            val t = textGetter()
            menu("Text") {
               items {
                  CoreMenus.menuItemBuilders[t]
               }
            }.disIf(t==null)
         }

         if (valueGetter!=null) {
            val v = valueGetter()
            menu(v?.estimateRuntimeType()?.toUi() ?: "Value") {
               items {
                  CoreMenus.menuItemBuilders[v]
               }
            }.disIf(v==null)
         }
      }
      show(tf, event)
   }
}