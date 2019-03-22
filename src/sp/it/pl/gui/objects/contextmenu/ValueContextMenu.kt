package sp.it.pl.gui.objects.contextmenu

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseEvent
import sp.it.pl.util.access.AccessibleValue
import sp.it.pl.util.collections.setTo


/**
 * Context menu wrapping a value - usually an object set before showing, for menu items' action.
 * It can then generate the items based on the value from supported actions, using [contextMenuGenerator].
 */
open class ValueContextMenu<E: Any?>: ContextMenu(), AccessibleValue<E> {

    protected var v: E? = null

    init {
        consumeAutoHidingEvents = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(): E = v as E

    override fun setValue(value: E) {
        v = value
    }

    /** Invokes [setValue] and sets items to those provided by [contextMenuGenerator] for the value of this menu. */
    open fun setValueAndItems(value: E) {
        setValue(value)
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