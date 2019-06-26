package sp.it.pl.plugin.notif

import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.util.Duration
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.setToOne
import sp.it.util.functional.invoke
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.stackPane
import sp.it.util.units.seconds

/** Notification popover. */
class Notification: PopOver<Node>() {
    private val closer = fxTimer(5.seconds, 1, ::hide)
    private val root = stackPane()

    /** Executes on left mouse click. Default does nothing. */
    var lClickAction = Runnable {}

    /** Executes on right mouse click. Default does nothing. */
    var rClickAction = Runnable {}

    /** Time this notification will remain visible. Default 5 seconds. */
    var duration: Duration
        get() = closer.period
        set(duration) {
            closer.period = duration
        }

    init {
        detached.value = false
        detachable.value = false
        userResizable.value = false
        isHideOnEscape = false
        arrowSize.value = 0.0
        arrowIndent.value = 0.0
        cornerRadius.value = 0.0
        isAutoFix = false
        isAutoHide = false
        skinn.setTitleAsOnlyHeaderContent(true)
        styleClass += "notification"

        contentNode.value = root.apply {
            onEventDown(MOUSE_CLICKED, PRIMARY) { lClickAction() }
            onEventDown(MOUSE_CLICKED, SECONDARY) { rClickAction() }
            onEventDown(MOUSE_ENTERED) { closer.pause() }
            onEventDown(MOUSE_EXITED) { closer.unpause() }
        }
    }

    override fun show(pos: ScreenPos) {
        super.show(pos)
        closer.start()
    }

    fun setContent(content: Node, titleText: String) {
        headerVisible.value = !titleText.isEmpty()
        title.value = titleText
        root.children setToOne content
        content.isMouseTransparent = true
    }

}