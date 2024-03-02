package sp.it.util.ui

import javafx.scene.Node
import javafx.stage.Window
import sp.it.util.functional.toUnit

/**
 * Marks non-ui object that owns ui object and may define additional observable properties for it.
 * Use for ui classes that do not not use inheritance but composition.
 * Set on ui object to make the non-ui object discoverable through scenegraph, see [Node.uiDelegate] and [Window.uiDelegate]
 * ideally in non-ui object's constructor.
 */
object UiDelegate {
   fun of(o: Any): Any? = when (o) {
      is Node -> o.uiDelegate
      is Window -> o.uiDelegate
      else -> null
   }
}

/** [UiDelegate] for this node */
var Node.uiDelegate: Any?
   get() = properties[UiDelegate]
   set(value) = properties.put(UiDelegate, value).toUnit()

/** [UiDelegate] for this window */
var Window.uiDelegate: Any?
   get() = properties[UiDelegate]
   set(value) = properties.put(UiDelegate, value).toUnit()