package sp.it.pl.ui.objects

import javafx.geometry.Side
import javafx.scene.input.Clipboard
import javafx.scene.layout.Priority
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.VBox
import sp.it.pl.main.IconMD
import sp.it.pl.main.listBox
import sp.it.pl.main.listBoxRow
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.collections.setTo
import sp.it.util.collections.toStringPretty
import sp.it.util.reactive.on
import sp.it.util.reactive.syncTrue
import sp.it.util.ui.displayed
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onNodeDispose
import sp.it.util.ui.textArea

/** Displays content of system clipboard */
class ClipboardViewer: VBox() {
   private val groups = listBox { }
   private val content = textArea { isEditable = false }

   init {
      lay += groups
      lay += label()
      lay += Icon(IconMD.RELOAD).onClickDo { reload() }.withText(Side.RIGHT, "Reload")
      lay += label()
      lay(ALWAYS) += content

      displayed syncTrue { reload() } on onNodeDispose
   }

   override fun requestFocus() {
      (groups.children.firstOrNull() ?: children.firstOrNull())?.requestFocus()
   }

   private fun reload() {
      val expandedOld = groups.userData
      val c = Clipboard.getSystemClipboard()
      groups.children setTo c.contentTypes
         .sortedBy { it.toString() }
         .map { f ->
            if (f===expandedOld)
               content.text = c.getContent(f).convertToUi()

            listBoxRow(IconMD.MAGNIFY, f.toString()) {
               icon.onClickDo {
                  content.text = c.getContent(f).convertToUi()
                  groups.userData = f
               }
               this.select(f===expandedOld)
            }
         }
   }

   fun Any?.convertToUi() = when (this) {
      is Collection<*> -> toStringPretty()
      is Map<*, *> -> toStringPretty()
      else -> toUi()
   }

}