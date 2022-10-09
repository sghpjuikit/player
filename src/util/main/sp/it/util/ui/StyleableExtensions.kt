package sp.it.util.ui

import javafx.css.Styleable
import javafx.scene.Node

/** Adds the specified styleclass to [Node.styleClass] of this node, if it has not yet been assigned. */
fun Styleable.styleclassAdd(styleClass: String) {
   if (styleClass !in this.styleClass)
      this.styleClass += styleClass
}

/** Adds (true) or removes (false) the specified styleclass using [Node.styleclassAdd] and [Node.styleclassRemove]. */
fun Styleable.styleclassToggle(styleClass: String, enabled: Boolean) {
   if (enabled) styleclassAdd(styleClass)
   else styleclassRemove(styleClass)
}

/** Adds or removes the specified styleclass using [Node.styleclassAdd] and [Node.styleclassRemove]. */
fun Styleable.styleclassToggle(styleClass: String) {
   if (styleClass !in this.styleClass) styleclassAdd(styleClass)
   else styleclassRemove(styleClass)
}

/** Removes all instances of the specified styleclass from [Node.styleClass] of this node. */
fun Styleable.styleclassRemove(styleClass: String) {
   this.styleClass.removeIf { it==styleClass }
}