package sp.it.util.ui

import javafx.event.ActionEvent
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCombination
import javafx.stage.PopupWindow
import javafx.stage.Window
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.traverse
import sp.it.util.reactive.sync1IfNonNull
import sp.it.util.text.nullIfBlank

/** @return [ActionEvent.source] cast to [MenuItem] */
fun ActionEvent.sourceMenuItem(): MenuItem? = source.asIf<MenuItem>()

/** @return [PopupWindow.ownerNode] of the root [ContextMenu] of this menu item */
fun MenuItem.traverseToPopupOwnerNode(): Node? = parentPopup?.traverse { it.ownerWindow?.asIf<ContextMenu>() }?.lastOrNull()?.ownerNode

/** @return [PopupWindow.ownerWindow] of the root [ContextMenu] of this menu item */
fun MenuItem.traverseToPopupOwnerWindow(): Window? = parentPopup?.traverse<Window> { if (it is PopupWindow) it.ownerWindow else null }?.firstOrNull { it !is PopupWindow }

/** [MenuItem.accelerator] text for cases without accelerator but text is desired. Getter always returns null. Sets accelerator to [KeyCombination.NO_MATCH] */
var MenuItem.acceleratorText: String?
   @Deprecated("set-only")
   get() = null
   set(keys) {
      keys.nullIfBlank().ifNotNull { k ->
         accelerator = KeyCombination.NO_MATCH // required so accelerator-text is visible
         parentPopupProperty().flatMap { it.skinProperty() }.sync1IfNonNull {
            it.node.lookupAll(".accelerator-text").forEach { if (it.parent?.lookupChildAs<Label>()?.text==text) it.asIs<Label>().text = k }
         }
      }
   }