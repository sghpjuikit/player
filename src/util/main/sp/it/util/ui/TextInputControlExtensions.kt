package sp.it.util.ui

import javafx.scene.control.TextInputControl

/** [TextInputControl.copy] selected text or all text if no selection. */
fun TextInputControl.copySelectedOrAll() {
   if (selectedText.isEmpty()) {
      selectAll()
      copy()
      deselect()
   } else {
      copy()
   }
}

/** [TextInputControl.cut] selected text or all text if no selection. */
fun TextInputControl.cutSelectedOrAll() {
   if (selectedText.isEmpty()) {
      selectAll()
      cut()
      deselect()
   } else {
      cut()
   }
}