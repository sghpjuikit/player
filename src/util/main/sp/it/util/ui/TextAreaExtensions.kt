package sp.it.util.ui

import javafx.scene.control.ScrollBar
import javafx.scene.control.TextArea
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.reactive.attach
import sp.it.util.text.concatenateWithBackspace

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
      val ot = text.orEmpty()
      val nt = text.orEmpty().concatenateWithBackspace(t)
      val l = nt.length-ot.length-t.length
      replaceText(ot.length+l, ot.length, nt.substring(ot.length+l))
      sb.ifNotNull { it.value = it.max }
   } else {
      val sbValue = sb?.value
      text = text.orEmpty().concatenateWithBackspace(t)
      sb?.value = sbValue!!
   }

   // restore selection
   if (s.length>0) selectRange(s.start, s.end)
}