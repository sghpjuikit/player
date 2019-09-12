package sp.it.pl.gui.objects.window.stage

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser.GWL_STYLE
import javafx.scene.input.MouseButton
import javafx.scene.robot.Robot
import javafx.stage.Stage
import sp.it.pl.gui.objects.picker.ContainerPicker
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.layout.widget.initialTemplateFactory
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconFA
import sp.it.util.async.runFX
import sp.it.util.reactive.sync1If
import sp.it.util.system.Os
import sp.it.util.ui.centre
import sp.it.util.ui.toPoint2D
import sp.it.util.units.millis
import sp.it.util.units.seconds
import java.util.UUID

fun Window.installStartLayoutPlaceholder() {

   fun showStartLayoutPlaceholder() {
      var action = {}
      val p = Placeholder(IconFA.FOLDER, "Start with a simple click\n\nIf you are 1st timer, choose ${ContainerPicker.choiceForTemplate} > ${initialTemplateFactory.name()}") { action() }
      action = {
         runFX(300.millis) {
            AppAnimator.closeAndDo(p) {
               runFX(500.millis) {
                  p.hide()
                  Robot().apply {
                     mouseMove(root.localToScreen(root.layoutBounds).centre.toPoint2D())
                     mouseClick(MouseButton.PRIMARY)
                  }
               }
            }
         }
      }
      AppAnimator.applyAt(p, 0.0)
      p.showFor(content)
      AppAnimator.openAndDo(p) {}
   }

   s.showingProperty().sync1If({ it }) {
      runFX(1.seconds) {
         if (topContainer?.children?.isEmpty()==true) {
            showStartLayoutPlaceholder()
         }
      }
   }

}

/**
 * Sets window always at bottom (opposite of always on top).<br></br>
 * Windows only.
 *
 * @apiNote adjusts native window style. Based on: http://stackoverflow.com/questions/26972683/javafx-minimizing-undecorated-stage
 */
@Suppress("LocalVariableName", "SpellCheckingInspection")
fun Stage.setNonInteractingOnBottom() {
   if (!Os.WINDOWS.isCurrent) return

   showingProperty().sync1If({ it }) {
      val user32 = User32.INSTANCE
      val titleOriginal = title
      val titleUnique = UUID.randomUUID().toString()
      title = titleUnique
      val hwnd = user32.FindWindow(null, titleUnique)   // find native window by title
      title = titleOriginal

      // Prevent window from popping up
      val WS_EX_NOACTIVATE = 0x08000000  // https://msdn.microsoft.com/en-us/library/ff700543(v=vs.85).aspx
      val oldStyle = user32.GetWindowLong(hwnd, GWL_STYLE)
      val newStyle = oldStyle or WS_EX_NOACTIVATE
      user32.SetWindowLong(hwnd, GWL_STYLE, newStyle)

      // Put the window on bottom
      // http://stackoverflow.com/questions/527950/how-to-make-always-on-bottom-window
      val SWP_NOSIZE = 0x0001
      val SWP_NOMOVE = 0x0002
      val SWP_NOACTIVATE = 0x0010
      val HWND_BOTTOM = 1
      user32.SetWindowPos(hwnd, WinDef.HWND(Pointer(HWND_BOTTOM.toLong())), 0, 0, 0, 0, SWP_NOSIZE or SWP_NOMOVE or SWP_NOACTIVATE)
   }
}
