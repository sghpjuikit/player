package sp.it.pl.ui

import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.control.TextArea
import javafx.scene.control.TextInputControl
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SHIFT
import javafx.scene.input.KeyCode.SHORTCUT
import javafx.scene.input.MouseEvent
import javafx.scene.text.TextAlignment
import sp.it.pl.core.CoreMenus
import sp.it.pl.main.Key
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.contextmenu.MenuItemBoolean
import sp.it.util.reactive.attach
import sp.it.util.text.keys
import sp.it.util.type.estimateRuntimeType
import sp.it.util.ui.copySelectedOrAll
import sp.it.util.ui.cutSelectedOrAll
import sp.it.util.ui.dsl
import sp.it.util.ui.insertNewline
import sp.it.util.ui.isNewlineOnShiftEnter
import sp.it.util.ui.show
import sp.it.util.ui.textAlignment

fun showContextMenu(tf: TextInputControl, event: MouseEvent, textGetter: (() -> String?)?, valueGetter: (() -> Any?)?) {
   fun MenuItem.disIf(condition: Boolean) = apply { isDisable = condition }

   ContextMenu().apply {
      dsl {
         item("Undo", keys = keys(SHORTCUT, Key.Z)) { tf.undo() }.disIf(!tf.isUndoable || !tf.isEditable)
         item("Redo", keys = keys(SHORTCUT, SHIFT, Key.Z)) { tf.redo() }.disIf(!tf.isRedoable || !tf.isEditable)
         item("Cut", keys = keys(SHORTCUT, Key.X)) { tf.cutSelectedOrAll() }.disIf(!tf.isEditable)
         item("Copy", keys = keys(SHORTCUT, Key.C)) { tf.copySelectedOrAll() }
         item("Paste", keys = keys(SHORTCUT, Key.V)) { tf.paste() }.disIf(!tf.isEditable || !Clipboard.getSystemClipboard().hasString())

         if (tf is TextArea && tf.isEditable)
            item("Paste newline", keys = if (tf.isNewlineOnShiftEnter) keys(SHIFT, ENTER) else keys(ENTER)) { tf.insertNewline() }

         item("Delete", keys = keys(DELETE)) { tf.deleteText(tf.selection) }.disIf(!tf.isEditable || tf.selectedText.isNullOrEmpty())

         separator()

         item("Select All", keys = keys(SHORTCUT, Key.A)) { tf.selectAll() }

         if (tf is TextArea) {
            separator()
            item {
               MenuItemBoolean("Wrap text", tf.isWrapText).apply { selected.attach { tf.isWrapText = it } }
            }
            menu("Align text") {
               items {
                  MenuItemBoolean.buildSingleSelectionMenu(TextAlignment.entries, tf.textAlignment, { it.toUi() }) { tf.textAlignment = it }.asSequence()
               }
            }
            menu("Scroll") {
               item("Top", keys = keys(SHORTCUT, Key.HOME)) { tf.scrollTop = Double.MIN_VALUE }
               item("Bottom", keys = keys(SHORTCUT, Key.END)) { tf.scrollTop = Double.MAX_VALUE }
               item("Left", keys = keys(Key.HOME)) { tf.scrollLeft = Double.MIN_VALUE }
               item("Right", keys = keys(Key.END)) { tf.scrollLeft = Double.MIN_VALUE }
            }
         }

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