package sp.it.util.ui

import javafx.scene.control.TextField

/** [TextField.copy] selected text or all text if no selection. */
fun TextField.copySelectedOrAll() {
   if (selectedText.isEmpty()) {
      selectAll()
      copy()
      deselect()
   } else {
      copy()
   }
}

/** [TextField.cut] selected text or all text if no selection. */
fun TextField.cutSelectedOrAll() {
   if (selectedText.isEmpty()) {
      selectAll()
      cut()
      deselect()
   } else {
      cut()
   }
}