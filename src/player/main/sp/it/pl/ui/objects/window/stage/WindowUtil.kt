@file:Suppress("UNCHECKED_CAST")

package sp.it.pl.ui.objects.window.stage

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser.GWL_STYLE
import com.sun.jna.platform.win32.WinUser.SMTO_NORMAL
import javafx.event.Event
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Cursor
import javafx.scene.Cursor.E_RESIZE
import javafx.scene.Cursor.NE_RESIZE
import javafx.scene.Cursor.NW_RESIZE
import javafx.scene.Cursor.N_RESIZE
import javafx.scene.Cursor.SE_RESIZE
import javafx.scene.Cursor.SW_RESIZE
import javafx.scene.Cursor.S_RESIZE
import javafx.scene.Cursor.W_RESIZE
import javafx.scene.Node
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Region
import javafx.scene.robot.Robot
import javafx.stage.Stage
import sp.it.pl.ui.objects.picker.ContainerPicker
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.pl.layout.widget.initialTemplateFactory
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconFA
import sp.it.util.async.runFX
import sp.it.util.dev.fail
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1If
import sp.it.util.system.Os
import sp.it.util.ui.anchorPane
import sp.it.util.ui.centre
import sp.it.util.ui.hBox
import sp.it.util.ui.initClip
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.stackPane
import sp.it.util.ui.toPoint2D
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.seconds
import java.util.UUID

fun Window.installStartLayoutPlaceholder() {

   fun showStartLayoutPlaceholder() {
      var action = {}
      val p = Placeholder(IconFA.FOLDER, "Start with a simple click\n\nIf you are 1st timer, choose ${ContainerPicker.choiceForTemplate} > ${initialTemplateFactory.name}") { action() }
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
 * Sets window to be non-interactive and always at bottom (opposite of always on top).
 * Windows only, no-op on other platforms.
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

/**
 * Sets window to be non-interactive and always at the bottom, behind the icons.
 * Windows only, no-op on other platforms.
 *
 * Closing this window will make its last graphics to remain visible until wallpaper is repainted.
 *
 * @apiNote adjusts native window style. Based on: https://www.codeproject.com/articles/856020/draw-behind-desktop-icons-in-windows-plus
 */
@Suppress("LocalVariableName", "SpellCheckingInspection", "UNUSED_ANONYMOUS_PARAMETER")
fun Stage.setNonInteractingProgmanOnBottom() {
   if (!Os.WINDOWS.isCurrent) return

   showingProperty().sync1If({ it }) {
      val user32 = User32.INSTANCE

      val titleOriginal = title
      val titleUnique = UUID.randomUUID().toString()
      title = titleUnique
      val hwnd = user32.FindWindow(null, titleUnique)   // find native window by title
      title = titleOriginal

      // Fetch the Progman window
      val progman = user32.FindWindow("Progman", null)

      // Send 0x052C to Progman
      // This message directs Progman to spawn a WorkerW behind the desktop icons. If it is already there, nothing happens.
      val result: WinDef.DWORDByReference? = null
      user32.SendMessageTimeout(progman, 0x052C, null, WinDef.LPARAM(), SMTO_NORMAL, 1000, result);

      // Get WorkerW window
      var workerW: WinDef.HWND? = null
      user32.EnumWindows(
         { tophandle, topparamhandle ->
            val p = user32.FindWindowEx(tophandle, null, "SHELLDLL_DefView", null)
            if (p!=null) {
               workerW = user32.FindWindowEx(null, tophandle, "WorkerW", null)
            }
            true
         },
         null
      )

      // Set WorkerW as parent
      if (hwnd!=null && workerW!=null)
         user32.SetParent(hwnd, workerW)
   }
}

@Suppress("UNUSED_PARAMETER")
@SuppressWarnings
fun <T: Node> Node.lookupId(id: String, type: Class<T>): T = lookup("#$id") as T? ?: fail { "No match for id=$id" }

fun buildWindowLayout(onDragStart: (MouseEvent) -> Unit, onDragged: (MouseEvent) -> Unit, onDragEnd: (MouseEvent) -> Unit) = stackPane {
   id = "root"
   styleClass += "window"
   prefSize = 400 x 600

   lay += stackPane {
      id = "back"
      isMouseTransparent = true

      lay += stackPane {
         id = "backImage"
         styleClass += "bgr-image"
      }
   }

   lay += anchorPane {
      id = "front"

      layFullArea += vBox {
         id = "frontContent"

         lay += stackPane {
            id = "headerContainer"

            lay += stackPane {
               id = "header"
               styleClass += "header"
               padding = Insets(8.0, 5.0, 0.0, 5.0)

               lay(CENTER_LEFT) += hBox(4.0, CENTER_LEFT) {
                  id = "leftHeaderBox"
                  isFillHeight = false
                  isPickOnBounds = false

                  lay += label() {
                     padding = Insets(0.0, 0.0, 0.0, 5.0)
                     id = "titleL"
                  }
               }

               lay(CENTER_RIGHT) += hBox(4.0, CENTER_RIGHT) {
                  id = "rightHeaderBox"
                  isFillHeight = false
                  isPickOnBounds = false

                  onEventDown(MouseEvent.MOUSE_DRAGGED, Event::consume)
               }
            }
         }
         lay(ALWAYS) += anchorPane {
            id = "content"
            styleClass += "content"
            minSize = 0 x 0
            initClip()
         }
      }
      lay(0, 0, null, 0) += stackPane {
         id = "headerActivator"
         prefHeight = 5.0
      }

      fun borderRegion(w: Number, h: Number, mc: Cursor) = Region().apply {
         onEventDown(MouseEvent.MOUSE_DRAGGED, onDragged)
         onEventDown(MouseEvent.MOUSE_PRESSED, onDragStart)
         onEventDown(MouseEvent.MOUSE_RELEASED, onDragEnd)
         cursor = mc
         prefSize = w x h
      }

      lay(null, 25, 0, 25) += borderRegion(-1, 4, S_RESIZE)
      lay(0, 25, null, 25) += borderRegion(-1, 4, N_RESIZE)
      lay(25, 0, 25, null) += borderRegion(4, -1, E_RESIZE)
      lay(25, null, 25, 0) += borderRegion(4, -1, W_RESIZE)
      lay(null, 0, 0, null) += borderRegion(4, 25, SE_RESIZE)
      lay(null, 0, 0, null) += borderRegion(25, 4, SE_RESIZE)
      lay(0, null, null, 0) += borderRegion(4, 25, NW_RESIZE)
      lay(0, null, null, 0) += borderRegion(25, 4, NW_RESIZE)
      lay(null, null, 0, 0) += borderRegion(25, 4, SW_RESIZE)
      lay(null, null, 0, 0) += borderRegion(4, 25, SW_RESIZE)
      lay(0, 0, null, null) += borderRegion(4, 25, NE_RESIZE)
      lay(0, 0, null, null) += borderRegion(25, 4, NE_RESIZE)
   }
}