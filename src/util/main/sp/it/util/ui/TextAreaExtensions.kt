package sp.it.util.ui

import javafx.scene.control.IndexRange
import javafx.scene.control.ScrollBar
import javafx.scene.control.TextArea
import javafx.scene.input.MouseEvent
import sp.it.util.dev.printIt
import sp.it.util.dev.printStacktrace
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown

/** [TextArea.appendText] that 1 preserves scroll if user is not at the bottom or selection not empty; 2 preserves selection */
fun TextArea.appendTextSmart(t: String) {
   if (t.isEmpty()) return

   // install monitoring lazily
   val skKey = "isScrolledToBottom-skin-observer"
   val scKey = "isScrolledToBottom-scrollbar"
   if (scKey !in properties) properties[scKey] = skin?.node?.lookup(".scroll-bar:vertical")?.asIs<ScrollBar>()
   if (skKey !in properties) properties[skKey] = skinProperty() attach { properties[scKey] = null }

   // preserve selection
   val s = selection

   val sb = properties[scKey]?.asIs<ScrollBar>()
   var isBottom = sb?.net { it.value == it.max } ?: true
   if (isBottom) {
      appendText(t)
   } else {
      val sbValue = sb?.value
      setText(text.orEmpty() + t)
      sb?.value = sbValue!!
   }

   // restore selection
   if (s.length>0) selectRange(s.start, s.end)
}