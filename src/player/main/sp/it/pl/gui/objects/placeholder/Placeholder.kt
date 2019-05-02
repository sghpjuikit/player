package sp.it.pl.gui.objects.placeholder

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import sp.it.pl.core.CoreMouse
import sp.it.pl.gui.objects.icon.Icon
import sp.it.util.functional.traverse
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.Util.layHeaderBottom
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.size

/**
 * Placeholder pane. Can invoke action and display its icon and name.
 *
 * Useful mostly instead of empty content like "click to add items" or to signal interactive ui element.
 */
open class Placeholder(actionIcon: GlyphIcons, actionName: String, action: () -> Unit): StackPane() {

    @JvmField val icon = Icon()
    @JvmField val desc = Label()
    private var s: Subscription? = null

    init {
        styleClass += STYLECLASS
        desc.text = actionName

        icon.styleclass(STYLECLASS_ICON)
        icon.icon(actionIcon)
        icon.onClick(action)
        icon.isFocusTraversable = true

        onEventDown(MOUSE_CLICKED, PRIMARY) { action() }
        onEventDown(KEY_PRESSED, ENTER) { action() }
        parentProperty() sync {
            if (it!=null && it.scene?.window?.isShowing==true && it.contains(it.screenToLocal(CoreMouse.mousePosition)))
                pseudoClassChanged("hover", true)
        }

        lay += layHeaderBottom(8.0, Pos.CENTER, icon, desc)
        isVisible = false
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
        private const val STYLECLASS = "placeholder-pane"
        private const val STYLECLASS_ICON = "placeholder-pane-icon"
    }

}