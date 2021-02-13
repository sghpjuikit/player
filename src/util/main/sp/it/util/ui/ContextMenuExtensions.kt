package sp.it.util.ui

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.MouseEvent

/**
 * Shows the context menu for node at screen coordinates of the specified event.
 *
 * Prefer this method to show context menu (especially in MouseClick handler), because when showing ContextMenu,
 * there is a difference between show(Window,x,y) and (Node,x,y). The former will not hide the menu when next click
 * happens within the node itself! This method avoids that.
 */
fun ContextMenu.show(n: Node, e: MouseEvent) = show(n.scene.window, e.screenX, e.screenY)

/**
 * Shows the context menu for node at screen coordinates of the specified event.
 *
 * Prefer this method to show context menu (especially in MouseClick handler), because when showing ContextMenu,
 * there is a difference between show(Window,x,y) and (Node,x,y). The former will not hide the menu when next click
 * happens within the node itself! This method avoids that.
 */
fun ContextMenu.show(n: Node, e: ContextMenuEvent) = show(n.scene.window, e.screenX, e.screenY)