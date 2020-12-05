package sp.it.pl.layout.widget

import javafx.scene.Node
import javafx.scene.Parent
import javafx.stage.Stage
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.main.APP
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.traverse
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.pickTopMostAt
import sp.it.util.ui.toP
import sp.it.util.ui.xy

object Widgets {
   val focusChangedHandler: (Node?, Boolean) -> Unit = { n, allowTraversal ->
      val window = n?.scene?.window
      if (n!=null && window!=null) {
         val widgets = APP.widgetManager.widgets.findAll(OPEN).filter { it.window===window }.toList()
         widgets.find {
            it.uiTemp?.root?.isAnyParentOf(n) ?: false
         }.ifNotNull { fw ->
            widgets.forEach { w -> if (w!==fw) w.focusedImpl.value = false }
            fw.focusedImpl.value = true
            if (allowTraversal) fw.focusAndTraverseFromToRoot()
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

fun Widget.focusAndTraverseFromToRoot() {
   traverse<Component> { it.parent }
      .windowed(2, 1, false) { it[1].asIs<Container<*>>() to it[0] }
      .forEach { (c, w) -> c.ui?.focusTraverse(w, this) }
}

fun Parent.widgetFocused(): Widget? = APP.widgetManager.widgets.findAll(OPEN).find { it.focused.value && it.window?.scene?.root == this }

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