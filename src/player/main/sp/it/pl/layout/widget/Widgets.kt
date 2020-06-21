package sp.it.pl.layout.widget

import javafx.scene.Node
import javafx.stage.Stage
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.main.APP
import sp.it.util.functional.ifNotNull
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.pickTopMostAt
import sp.it.util.ui.toP
import sp.it.util.ui.xy

object Widgets {
   val focusChangedHandler: (Node?) -> Unit = { n ->
      val window = n?.scene?.window
      if (n!=null && window!=null) {
         val widgets = APP.widgetManager.widgets.findAll(OPEN).filter { it.window===window }.toList()
         widgets.find {
            it.uiTemp?.root?.isAnyParentOf(n) ?: false
         }.ifNotNull { fw ->
            widgets.forEach { w -> if (w!==fw) w.focusedImpl.value = false }
            fw.focusedImpl.value = true
         }
      }
   }
}

var Widget.forceLoading: Boolean
   get() = "forceLoading" in properties
   set(value) {
      if (value) properties["forceLoading"] = Any()
      else properties -= "forceLoading"
   }

fun widgetFocused(): Widget? = APP.widgetManager.widgets.findAll(OPEN).find { it.focused.value }

fun Stage.widgetAtMousePos(): Widget? {
   val pos = APP.mouse.mousePosition.toP() - xy
   val node = scene?.root?.pickTopMostAt(pos.x, pos.y) { it.isVisible }
   return widgetContainingNode(node)
}

private fun widgetContainingNode(node: Node?): Widget? {
   if (node==null) return null
   val window = node.scene?.window
   return APP.widgetManager.widgets
      .findAll(OPEN).filter { it.window===window }
      .find { it.uiTemp?.root?.isAnyParentOf(node) ?: false }
}