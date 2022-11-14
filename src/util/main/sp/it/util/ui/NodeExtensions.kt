package sp.it.util.ui

import javafx.beans.value.ObservableValue
import javafx.geometry.Point2D
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Control
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Pane
import javafx.scene.robot.Robot
import javafx.stage.Window
import mu.KotlinLogging
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.functional.traverse
import sp.it.util.reactive.Disposer

private val robot by lazy { Robot() }

/** @return property that is true iff this node is attached to a scene in a window that is [Window.displayed] */
val Node.displayed: ObservableValue<Boolean> get() = sceneProperty().flatMap { it.windowProperty() }.flatMap { it.showingProperty() }.orElse(false)

/** @return path of this node to scene root */
val Node.scenePath: String get() = traverseParents().map { "${it.id.orEmpty()}:${it::class.simpleName}" }.toList().asReversed().joinToString(" ")

/** @return sequence of this and all parents in bottom to top order */
fun Node.traverseParents(): Sequence<Node> = traverse { it.parent }

/** @return true iff this or any of [Parent.unmodifiableChildren] [Node.isFocused] */
fun Node.hasFocus(): Boolean = scene?.focusOwner?.traverseParents().orEmpty().any { it==this }

/** @return true iff this is direct parent of the specified node */
fun Node.isParentOf(child: Node) = child.parent===this

/** @return true iff this is direct or indirect parent of the specified node */
fun Node.isAnyParentOf(child: Node) = generateSequence(child.parent) { it.parent }.any { it===this }

/** @return true iff this is direct child of the specified node */
fun Node.isChildOf(parent: Node) = parent.isParentOf(this)

/** @return true iff this is direct or indirect child of the specified node */
fun Node.isAnyChildOf(parent: Node) = parent.isAnyParentOf(this)

/** @return this or direct or indirect parent of this that passes specified filter or null if no element passes */
fun Node.findParent(filter: (Node) -> Boolean) = traverseParents().find(filter)

/** Removes this from its parent's children if possible (parent is [Pane] or [Group]). Any child's focus is moved to parent. */
fun Node.removeFromParent() {
   val p = parent
   val hasFocusedChild = scene?.focusOwner?.isAnyChildOf(this) ?: false

   when (p) {
      is Group -> p.children -= this
      is Pane -> p.children -= this
   }

   // Fixes possible usage of this node after removal from parent, because scene retains reference to focusOwner and
   // removes it when focus changes. Focus listener would invoke when this node is no longer part of scene graph.
   if (hasFocusedChild)
      runTry { p?.requestFocus() }
         .ifError { KotlinLogging.logger {}.error(it) { "Failed to refocus content after removal from scene graph" } }
}

inline fun <reified T: Node> Node.lookupId(id: String): T = lookup("#$id").let {
   when (it) {
      null -> fail { "No match for id=$id" }
      is T -> it
      else -> fail { "No match for id=$id, ${it::class} is not ${T::class}" }
   }
}

@Suppress("UNCHECKED_CAST")
fun <T: Node> Node.lookupChildAt(at: Int): T = when (this) {
   is Parent -> childrenUnmodifiable.getOrNull(at) as T
   else -> fail { "${this::class} can not have children" }
}

inline fun <reified T: Node> Node.lookupChildAs(): T = when (this) {
   is Parent -> childrenUnmodifiable.asSequence().filterIsInstance<T>().first()
   else -> fail { "${this::class} can not have children" }
}

inline fun <reified T: Node> Node.lookupChildAsOrNull(): T? = when (this) {
   is Parent -> childrenUnmodifiable.asSequence().filterIsInstance<T>().firstOrNull()
   else -> fail { "${this::class} can not have children" }
}

@Suppress("UNCHECKED_CAST")
fun <T: Node> Node.lookupSiblingUp(by: Int = 1): T = parent!!.asIs<Parent>().childrenUnmodifiable.net { it.getOrNull(it.indexOf(this) - by)!! } as T

@Suppress("UNCHECKED_CAST")
fun <T: Node> Node.lookupSiblingDown(by: Int = 1): T = parent!!.asIs<Parent>().childrenUnmodifiable.net { it.getOrNull(it.indexOf(this) + by)!! } as T

/** @return whether this node shape contains the scene coordinates represented by the specified point */
fun Node.containsScene(scenePoint: Point2D): Boolean = contains(sceneToLocal(scenePoint)!!)

/** @return whether this node shape contains the scene coordinates */
fun Node.containsScene(sceneX: Double, sceneY: Double) = containsScene(Point2D(sceneX, sceneY))

/** @return whether this node shape contains the screen coordinates represented by the specified point */
fun Node.containsScreen(screenPoint: Point2D): Boolean = contains(screenToLocal(screenPoint)!!)

/** @return whether this node shape contains the screen coordinates */
fun Node.containsScreen(screenX: Double, screenY: Double) = containsScene(Point2D(screenX, screenY))

/** @return whether this node shape contains the coordinates of the specified mouse event */
fun Node.containsMouse(event: MouseEvent) = containsScene(Point2D(event.sceneX, event.sceneY))

/** @return whether this node shape contains the coordinates of the mouse */
fun Node.containsMouse() = containsScreen(robot.mousePosition)

/** @return topmost child node containing the specified scene coordinates optionally testing against the specified test or null if no match */
fun Node.pickTopMostAt(sceneX: Double, sceneY: Double, test: (Node) -> Boolean = { true }): Node? {
   // Groups need to be handled specially - they need to be transparent
   fun Node.isIn(sceneX: Double, sceneY: Double, test: (Node) -> Boolean) =
      if (parent is Group) test(this) && localToScene(layoutBounds).contains(sceneX, sceneY)
      else test(this) && sceneToLocal(sceneX, sceneY, true)?.let { it in this } ?: false

   return if (isIn(sceneX, sceneY, test)) {
      if (this is Parent) {
         for (i in childrenUnmodifiable.indices.reversed()) {
            val child = childrenUnmodifiable[i]

            // traverse into the group as if its children were this node's children
            if (child is Group) {
               for (j in child.childrenUnmodifiable.indices.reversed()) {
                  val ch = child.childrenUnmodifiable[j]
                  if (ch.isIn(sceneX, sceneY, test)) {
                     return ch.pickTopMostAt(sceneX, sceneY, test)
                  }
               }
            }

            if (child.isIn(sceneX, sceneY, test)) {
               return child.pickTopMostAt(sceneX, sceneY, test)
            }
         }
         return this
      } else {
         this
      }
   } else {
      null
   }
}

/** Convenience for [Node.pseudoClassStateChanged]. */
fun Node.pseudoClassChanged(pseudoClass: String, active: Boolean) = pseudoClassStateChanged(pseudoclass(pseudoClass), active)

/** Equivalent to [Node.pseudoClassChanged]. */
fun Node.pseudoClassToggle(pseudoClass: String, enabled: Boolean) = pseudoClassChanged(pseudoClass, enabled)

/** Adds or removes the specified pseudoclass using [Node.pseudoClassChanged]. */
fun Node.pseudoClassToggle(pseudoClass: String) {
   if (pseudoClassStates.none { it.pseudoClassName == pseudoClass }) pseudoClassToggle(pseudoClass, true)
   else pseudoClassToggle(pseudoClass, false)
}

/**
 * Disposer called in [Node.onNodeDispose].
 * 1st invocation of this property initializes the disposer and stores it in [Node.properties] with a unique key.
 */
val Node.onNodeDispose: Disposer
   get() = properties.getOrPut("onDispose_7bccf7a3-bcae-42ca-a91d-9f95217b942c") { Disposer() } as Disposer

/**
 * Disposer called in [Node.onNodeDispose] or null if it hasn't been initialized yet.
 */
val Node.onNodeDisposeOrNull: Disposer?
   get() = properties["onDispose_7bccf7a3-bcae-42ca-a91d-9f95217b942c"].asIf()

/**
 * Disposes of this node, with the intention of it and all it's children to never again be used in the scene graph.
 *
 * If a disposer is initialized, it will be invoked. Use [Node.onNodeDispose] property to access, initialize and add
 * subscriptions to the underlying disposer.
 *
 * If this is [Control], [javafx.scene.control.Skin.dispose] will be invoked on its skin.
 *
 * If this is [Parent], this function will be invoked on each item in [Parent.getChildrenUnmodifiable]. Disposal
 * is recursive, in a top-down depth-first traversal.
 *
 * This is an optional dispose API and as such is not used nor called automatically by the JavaFx. It is up to the
 * developer to call it appropriately, but one may wish to call it immediately after a call to [Node.removeFromParent].
 */
fun Node.onNodeDispose() {
   onNodeDisposeOrNull?.invoke()
   if (this is SplitPane) {
      items.forEach { it.onNodeDispose() }
   }
   if (this is ScrollPane) {
      content?.onNodeDispose()
      content = null // avoids `skin = null` calling `requestLayout()` and throwing exception
   }
   if (this is Control) skin?.dispose()
   if (this is Parent) childrenUnmodifiable.forEach { it.onNodeDispose() }
}