package sp.it.util.ui

import javafx.scene.control.ScrollBar
import javafx.scene.control.TextArea
import sp.it.util.dev.Experimental
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.toUnit
import sp.it.util.reactive.attach
import sp.it.util.reactive.map
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.text.concatApplyBackspace
import sp.it.util.text.lengthInLines
import sp.it.util.ui.Util.computeTextWidth

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
      val nt = text.orEmpty().concatApplyBackspace(t)
      val l = nt.length-ot.length-t.length
      replaceText(ot.length+l, ot.length, nt.substring(ot.length+l))
      sb.ifNotNull { it.value = it.max }
   } else {
      val sbValue = sb?.value
      text = text.orEmpty().concatApplyBackspace(t)
      sb?.value = sbValue!!
   }

   // restore selection
   if (s.length>0) selectRange(s.start, s.end)
}

/** Inserts newline character into the text at the caret position. Clears selection. Moves caret by 1. */
fun TextArea.insertNewline() {
   if (selection.length>0) replaceText(selection, "\n")
   else insertText(caretPosition, "\n")
}

/** Hint for skin whether user adds newline with ENTER or SHIFT+ENTER. Does not add behavior itself. */
var TextArea.isNewlineOnShiftEnter: Boolean
   get() = properties["newlineOnEnter"].asIf<Boolean>() ?: false
   set(value) = properties.put("newlineOnEnter", value).toUnit()

/** @return new observable property representing whether this text area contains single unwrapped line */
@Experimental("may not work correctly")
fun TextArea.singLineProperty() = wrapTextProperty() zip textProperty() zip2 widthProperty() map { (wrap, t, width) ->
   (t ?: "").lengthInLines>1 || (wrap && computeTextWidth(font, t ?: "") > width.toDouble()-padding.width)
}