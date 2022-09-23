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
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem
import sp.it.util.reactive.attach
import sp.it.util.text.keys
import sp.it.util.type.estimateRuntimeType
import sp.it.util.ui.copySelectedOrAll
import sp.it.util.ui.cutSelectedOrAll
import sp.it.util.ui.dsl
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
         if (tf is TextArea) menu("Paste newline") {
            item("\\r (CR)") { tf.insertText(tf.caretPosition, "\r") }
            item("\\n (LF)") { tf.insertText(tf.caretPosition, "\n") }
            item("\\r\\n(CRLF/EOF)", keys = keys(ENTER)) { tf.insertText(tf.caretPosition, "\r\n") }
         }
         item("Delete", keys = keys(DELETE)) { tf.deleteText(tf.selection) }.disIf(!tf.isEditable || tf.selectedText.isNullOrEmpty())

         separator()

         item("Select All", keys = keys(SHORTCUT, Key.C)) { tf.selectAll() }

         if (tf is TextArea) {
            separator()
            item {
               SelectionMenuItem("Wrap text", tf.isWrapText).apply { selected.attach { tf.isWrapText = it } }
            }
            menu("Align text") {
               items {
                  SelectionMenuItem.buildSingleSelectionMenu(TextAlignment.values().toList(), tf.textAlignment, { it.toUi() }) { tf.textAlignment = it }.asSequence()
               }
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