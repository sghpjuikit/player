package sp.it.pl.ui.objects.placeholder

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment
import sp.it.pl.ui.objects.Text
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.functional.asIf
import sp.it.util.functional.orNull
import sp.it.util.functional.traverse
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.Util.layHeaderBottom
import sp.it.util.ui.containsMouse
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.size
import sp.it.util.units.millis
import kotlin.math.sqrt

/**
 * Placeholder pane. Can invoke action and display its icon and multi-line information text.
 *
 * Useful mostly instead of empty content like "click to add items" or to signal interactive ui element.
 */
open class Placeholder(actionIcon: GlyphIcons, actionName: String, action: () -> Unit): StackPane() {

   @JvmField val icon = Icon()
   @JvmField val info = Text()
   private var s: Subscription? = null
   private var s2: Subscription? = null

   init {
      styleClass += STYLECLASS
      info.text = actionName
      info.textAlignment = TextAlignment.CENTER

      icon.styleclass(STYLECLASS_ICON)
      icon.icon(actionIcon)
      icon.action(action)
      icon.isMouseTransparent = true
      hoverProperty() sync { icon.select(it) }

      onEventDown(MOUSE_CLICKED, PRIMARY) { action() }
      onEventDown(KEY_PRESSED, ENTER) { action() }
      parentProperty() sync {
         if (it!=null && it.scene?.window?.isShowing==true && it.containsMouse())
            pseudoClassChanged("hover", true)
      }

      lay += layHeaderBottom(8.0, Pos.CENTER, icon, info)
      isVisible = false
   }

   /** Invoke just after [showFor] to get animation effect. [animateHide] must then be called! */
   fun animateShow(n: Node) {
      if (n.placeholderAnim==null) {
         n.placeholderAnim = anim(250.millis) { n.opacity = 1-ALMOST_TRANSPARENT*sqrt(it); opacity = sqrt(it) }.apply { playOpen() }
         s2?.unsubscribe()
         s2 = Subscription {
            n.placeholderAnim?.stop();
            n.placeholderAnim?.applyAt(0.0);
            n.placeholderAnim=null
         }
      }
   }

   /** Invoke just after [hide] if [animateShow] was invoked. */
   fun animateHide() {
      s2?.unsubscribe()
      s2 = null
   }

   /** Shows this placeholder so it is topmost child of specified node (or its first [Pane] parent), [visible] is true and [icon].[focused] is true. */
   fun showFor(n: Node) {
      val p = n.traverse { it.parent }.filterIsInstance<Pane>().firstOrNull()
      if (p!=null && this !in p.children) {

         p.lay += this
         s?.unsubscribe()
         s = n.layoutBoundsProperty().sync {
            minSize = it.size
            prefSize = it.size
            maxSize = it.size
            resizeRelocate(it.minX, it.minY, it.width, it.height)
         }
         toFront()
         isVisible = true
         requestFocus()
      }
   }

   override fun requestFocus() {
      icon.requestFocus()
   }

   /** Hides this placeholder so it is not a child of any node and [visible] is false. */
   fun hide() {
      s?.unsubscribe()
      s = null
      removeFromParent()
      isVisible = false
   }

   /** Calls [show] if specified visibility is true and [hide] otherwise. */
   fun show(n: Node, visible: Boolean) {
      if (visible) showFor(n)
      else hide()
   }

   companion object {
      private const val ALMOST_TRANSPARENT = 0.9 // not transparent, but not disrupting UX of text on top of it
      private const val STYLECLASS = "placeholder-pane"
      private const val STYLECLASS_ICON = "placeholder-pane-icon"
      private val PLACEHOLDER_ANIMATION_KEY = Any()
      private var Node.placeholderAnim: Anim?
         get() = properties[PLACEHOLDER_ANIMATION_KEY].asIf()
         set(value) {
            properties[PLACEHOLDER_ANIMATION_KEY] = value
         }
   }

}

/** Calls [Placeholder.show], preserving lazy semantics. */
fun Lazy<Placeholder>.show(n: Node, visible: Boolean) {
   if (visible) value.show(n, true)
   else orNull()?.hide()
}