package sp.it.pl.layout.area

import javafx.animation.FadeTransition
import javafx.animation.Interpolator.LINEAR
import javafx.animation.ScaleTransition
import javafx.event.EventHandler
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.layout.AnchorPane
import javafx.scene.shape.Rectangle
import sp.it.pl.gui.objects.picker.ContainerPicker
import sp.it.pl.gui.objects.picker.Picker
import sp.it.pl.gui.objects.picker.WidgetPicker
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.bicontainer.BiContainer
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.nodeAnimation
import sp.it.pl.util.animation.interpolator.CircularInterpolator
import sp.it.pl.util.animation.interpolator.EasingMode.EASE_OUT
import sp.it.pl.util.ui.drag.installDrag
import sp.it.pl.util.ui.layFullArea

/**
 * Container graphics for container null child.
 *
 * Shows user component picker to populate container with content (at this index) - it allows creation of layouts. Uses
 * two nested [Picker]s, one for [Container] and the other for widgets.
 */
class Layouter: ContainerNode {

    private val container: Container<*>
    private val index: Int

    override val root = AnchorPane()
    private var isCancelPlaying = false
    private val cp: ContainerPicker
    private val a1: FadeTransition
    private val a2: ScaleTransition
    private val clickShowHider: EventHandler<MouseEvent>
    private val exitHider: EventHandler<MouseEvent>
    var onCancel: () -> Unit = {}

    constructor(container: Container<*>, index: Int) {
        this.container = container
        this.index = index
        this.cp = ContainerPicker({ showContainer(it) }, { showWidgetArea() }).apply {
            onSelect = { AppAnimator.closeAndDo(root) { it.onSelect() } }
            onCancel = {
                isCancelPlaying = true
                if (!APP.ui.isLayoutMode)
                    hide()
            }
            consumeCancelEvent = false
            buildContent()
        }

        root.layFullArea += cp.root

        val dur = nodeAnimation(Rectangle()).cycleDuration
        a1 = FadeTransition(dur, cp.root)
        a1.interpolator = LINEAR
        a2 = ScaleTransition(dur, cp.root)
        a2.interpolator = CircularInterpolator(EASE_OUT)
        cp.root.opacity = 0.0
        cp.root.scaleX = 0.0
        cp.root.scaleY = 0.0
        installDrag(
                root, IconFA.EXCHANGE, "Switch components",
                { e -> Df.COMPONENT in e.dragboard },
                { e -> e.dragboard[Df.COMPONENT]===container },
                { e -> e.dragboard[Df.COMPONENT].swapWith(container, index) }
        )
        clickShowHider = EventHandler {
            if (it.button==PRIMARY) {
                if (cp.root.opacity==0.0 && !container.lockedUnder.value) {
                    show()
                    it.consume()
                }
            }
        }
        exitHider = EventHandler {
            if (!isCancelPlaying) {
                cp.onCancel()
                it.consume()
            }
        }

        isWeakMode = false
    }

    /**
     * In normal mode the controls are displayed on mouse click
     * In weak mode the controls are displayed on mouse hover
     */
    private var isWeakMode: Boolean
        set(value) {
            field = value
            if (root.onMouseExited==null)
                root.onMouseExited = exitHider
            if (value) {
                root.addEventHandler(MOUSE_ENTERED, clickShowHider)
                root.removeEventHandler(MOUSE_CLICKED, clickShowHider)
            } else {
                root.addEventHandler(MOUSE_CLICKED, clickShowHider)
                root.removeEventHandler(MOUSE_ENTERED, clickShowHider)
            }
        }

    override fun show() = showControls(true)

    override fun hide() = showControls(false)

    private fun showControls(value: Boolean) {
        val isWidgetSelectionActive = root.children.size!=1
        if (value && isWidgetSelectionActive) return

        a1.stop()
        a2.stop()
        if (value) {
            a1.onFinished = null
            a1.toValue = 1.0
            a2.toX = 1.0
            a2.toY = 1.0
        } else {
            val wasCancelPlaying = isCancelPlaying
            a1.onFinished = EventHandler {
                isCancelPlaying = false
                if (wasCancelPlaying) onCancel()
            }
            a1.toValue = 0.0
            a2.toX = 0.0
            a2.toY = 0.0
            isCancelPlaying = true
        }
        a1.play()
        a2.play()
    }

    private fun showContainer(c: Container<*>) {
        container.addChild(index, c)
        if (c is BiContainer) APP.actionStream("Divide layout")
    }

    private fun showWidgetArea() {
        val wp = WidgetPicker()
        wp.onSelect = { factory ->
            AppAnimator.closeAndDo(wp.root) {
                root.children -= wp.root
                root.onMouseExited = null
                container.addChild(index, factory.create())
                if (APP.ui.isLayoutMode) container.show()
                APP.actionStream("New widget")
            }
        }
        wp.onCancel = {
            AppAnimator.closeAndDo(wp.root) {
                root.children -= wp.root
                showControls(true)
            }
        }
        wp.consumeCancelEvent = true // we need right click to not close container
        wp.root.addEventHandler(MOUSE_CLICKED) { it.consume() } // also left click to not open container chooser
        wp.buildContent()

        root.layFullArea += wp.root
        AppAnimator.openAndDo(wp.root, null)
    }

}