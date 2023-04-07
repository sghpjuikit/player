package sp.it.util.ui

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.stage.PopupWindow
import javafx.stage.Window
import sp.it.util.functional.asIf
import sp.it.util.functional.traverse

fun ActionEvent.sourceMenuItem(): MenuItem? = source.asIf<MenuItem>()

fun MenuItem.traverseToPopupOwnerNode(): Node? = parentPopup?.traverse { it.ownerWindow?.asIf<ContextMenu>() }?.lastOrNull()?.ownerNode

fun MenuItem.traverseToPopupOwnerWindow(): Window? = parentPopup?.traverse<Window> { if (it is PopupWindow) it.ownerWindow else null }?.firstOrNull { it !is PopupWindow }