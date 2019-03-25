package sp.it.pl.gui.objects.contextmenu

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseEvent
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import sp.it.pl.util.collections.setTo
import sp.it.pl.util.reactive.onEventDown

/** Context menu displaying items for a value. These are generated using [contextMenuGenerator]. */
open class ValueContextMenu<E: Any?>(clearItemsOnHidden: Boolean = true): ContextMenu() {

    init {
        consumeAutoHidingEvents = false
        if (clearItemsOnHidden) onEventDown(WINDOW_HIDDEN) { items.clear() }
    }

    /** Sets items to those provided by [contextMenuGenerator] for the specified value. */
    open fun setValueAndItems(value: E) {
        items setTo contextMenuGenerator[this, value]
    }

    override fun show(n: Node, screenX: Double, screenY: Double) = show(n.scene.window, screenX, screenY)

    /**
     * Shows the context menu for node at proper coordinates of the event.
     *
     * Prefer this method to show context menu (especially in MouseClick handler), because when showing ContextMenu,
     * there is a difference between show(Window,x,y) and (Node,x,y). The former will not hide the menu when next click
     * happens within the node itself! This method avoids that.
     */
    fun show(n: Node, e: MouseEvent) = show(n.scene.window, e.screenX, e.screenY)

    fun show(n: Node, e: ContextMenuEvent) = show(n.scene.window, e.screenX, e.screenY)

}