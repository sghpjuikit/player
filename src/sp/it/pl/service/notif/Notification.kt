package sp.it.pl.service.notif

import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.layout.StackPane
import javafx.util.Duration
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.pl.util.async.executor.FxTimer
import sp.it.pl.util.functional.invoke

/** Notification popover. */
class Notification: PopOver<Node>() {
    private val closer = FxTimer(5000.0, 1) { hide() }
    private val root = StackPane()

    /** Executes on left mouse click. Default does nothing. */
    var lClickAction = Runnable {}

    /** Executes on right mouse click. Default does nothing. */
    var rClickAction = Runnable {}

    /** Time this notification will remain visible. Default 5 seconds. */
    var duration: Duration
        get() = closer.period
        set(duration) { closer.period = duration }

    init {
        detached.value = false
        detachable.value = false
        userResizable.value = false
        isHideOnEscape = false
        arrowSize = 0.0
        arrowIndent = 0.0
        cornerRadius = 0.0
        isAutoFix = false
        isAutoHide = false
        skinn.setTitleAsOnlyHeaderContent(true)
        styleClass += "notification"

        contentNode = root
        root.setOnMouseClicked {
            if (it.button==PRIMARY) lClickAction()
            if (it.button==SECONDARY) rClickAction()
        }
        root.setOnMouseEntered { closer.pause() }
        root.setOnMouseExited { closer.unpause() }
    }

    override fun show(pos: ScreenPos) {
        super.show(pos)
        closer.start()
    }

    fun setContent(content: Node, titleText: String) {
        headerVisible.value = !titleText.isEmpty()
        title.value = titleText
        root.children.setAll(content)
        content.isMouseTransparent = true
    }

}