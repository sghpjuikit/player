package sp.it.pl.layout.widget

import javafx.scene.Node
import sp.it.pl.main.APP
import sp.it.util.functional.ifNotNull
import sp.it.util.ui.isAnyParentOf

object Widgets {
   val focusChangedHandler: (Node?) -> Unit = { n ->
      val window = n?.scene?.window
      if (n!=null && window!=null) {
         val widgets = APP.widgetManager.widgets.findAll(WidgetSource.OPEN).filter { it.window===window }.toList()
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