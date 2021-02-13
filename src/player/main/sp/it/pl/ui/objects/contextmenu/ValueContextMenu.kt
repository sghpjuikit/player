package sp.it.pl.ui.objects.contextmenu

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import sp.it.pl.core.CoreMenus
import sp.it.util.collections.setTo
import sp.it.util.reactive.onEventDown

/** Context menu displaying items for a value. These are generated using [CoreMenus][CoreMenus.menuItemBuilders]]. */
open class ValueContextMenu<E: Any?>(clearItemsOnHidden: Boolean = true): ContextMenu() {

   init {
      consumeAutoHidingEvents = false
      if (clearItemsOnHidden) onEventDown(WINDOW_HIDDEN) { items.clear() }
   }

   /** Sets items to those provided by [CoreMenus][CoreMenus.menuItemBuilders] for the specified value. */
   open fun setItemsFor(value: E) = items setTo CoreMenus.menuItemBuilders[value]

   override fun show(n: Node, screenX: Double, screenY: Double) = show(n.scene.window, screenX, screenY)

}