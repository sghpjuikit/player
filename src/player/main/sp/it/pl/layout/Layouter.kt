package sp.it.pl.layout

import javafx.event.EventHandler
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.StackPane
import sp.it.pl.gui.objects.picker.ContainerPicker
import sp.it.pl.gui.objects.picker.Picker
import sp.it.pl.gui.objects.picker.WidgetPicker
import sp.it.pl.layout.container.BiContainer
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.container.ComponentUi
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.Df
import sp.it.pl.main.IconFA
import sp.it.pl.main.contains
import sp.it.pl.main.get
import sp.it.pl.main.installDrag
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.lay

/**
 * Container graphics for container null child.
 *
 * Shows user component picker to populate container with content (at this index) - it allows creation of layouts. Uses
 * two nested [Picker]s, one for [Container] and the other for widgets.
 */
class Layouter: ComponentUi {

    private val container: Container<*>
    private val index: Int

    override val root = StackPane()
    var onCancel: () -> Unit = {}
    private var isCancelPlaying = false
    private var wasSelected = false
    private val cp: ContainerPicker
    private var isCpShown = false

    constructor(container: Container<*>, index: Int) {
        this.container = container
        this.index = index
        this.cp = ContainerPicker({ showContainer(it) }, { showWidgetArea(it) }).apply {
            onSelect = {
                wasSelected = true
                isCpShown = false
                AppAnimator.closeAndDo(root) { it.onSelect() }
            }
            onCancel = {
                isCancelPlaying = true
                if (!APP.ui.isLayoutMode)
                    hide()
            }
            consumeCancelEvent = false
            buildContent()
        }
        APP.widgetManager.widgets.separateWidgets attach { cp.buildContent() }   // TODO: memory leak

        root.lay += cp.root

        AppAnimator.applyAt(cp.root, 0.0)

        installDrag(
                root, IconFA.EXCHANGE, "Switch components",
                { e -> Df.COMPONENT in e.dragboard },
                { e -> e.dragboard[Df.COMPONENT]===container },
                { e -> e.dragboard[Df.COMPONENT].swapWith(container, index) }
        )

        // show cp on mouse click
        root.onEventDown(MOUSE_CLICKED, PRIMARY, false) {
            if (!container.lockedUnder.value) {
                show()
                it.consume()
            }
        }

        // hide cp on mouse exit
        cp.root.onMouseExited = EventHandler {
            if (!isCancelPlaying && !wasSelected) {
                cp.onCancel()
                it.consume()
            }
        }
    }

    override fun show() = showControls(true)

    override fun hide() = showControls(false)

    fun close(onClosed: () -> Unit = {}) = showControls(false, onClosed)

    private fun showControls(value: Boolean, onClosed: () -> Unit = {}) {
        if (isCpShown==value) return
        val isWpShown = root.children.size!=1
        if (isWpShown) return

        isCpShown = value
        if (value) {
            AppAnimator.openAndDo(cp.root, null)
        } else {
            val wasCancelPlaying = isCancelPlaying
            AppAnimator.closeAndDo(cp.root) {
                isCancelPlaying = false
                if (wasCancelPlaying && !wasSelected)
                    onCancel()
                onClosed()
            }
            isCancelPlaying = true
        }
    }

    private fun showContainer(c: Container<*>) {
        container.addChild(index, c)
        if (c is BiContainer) APP.actionStream("Divide layout")
    }

    private fun showWidgetArea(mode: WidgetPicker.Mode) {
        val wp = WidgetPicker(mode)
        wp.onSelect = { factory ->
            AppAnimator.closeAndDo(wp.root) {
                root.children -= wp.root
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
        wp.buildContent()
        wp.consumeCancelEvent = true // we need right click to not close container

        root.lay += wp.root
        AppAnimator.openAndDo(wp.root, null)
    }

}