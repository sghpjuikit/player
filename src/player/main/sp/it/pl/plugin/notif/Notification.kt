package sp.it.pl.plugin.notif

import javafx.scene.Node
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.util.Duration
import sp.it.pl.gui.objects.window.popup.PopWindow
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.setToOne
import sp.it.util.functional.invoke
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.ui.stackPane
import sp.it.util.units.seconds

/** Notification popover. */
class Notification: PopWindow() {
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
      userResizable.value = false
      userMovable.value = false
      isEscapeHide.value = false
      isAutohide.value = false
      headerIconsVisible.value = false
      focusOnShow.value = false
      styleClass += "notification"
      onShown += { closer.start() }
      content.value = root.apply {
         setMinSize(150.0, 70.0)
         onEventDown(MOUSE_CLICKED, PRIMARY) { lClickAction() }
         onEventDown(MOUSE_CLICKED, SECONDARY) { rClickAction() }
         onEventUp(MOUSE_ENTERED) { closer.pause() }
         onEventUp(MOUSE_EXITED) { closer.unpause() }
      }
   }

   fun setContent(content: Node, titleText: String) {
      headerVisible.value = titleText.isNotEmpty()
      title.value = titleText
      root.children setToOne content
   }

}