@file:Suppress("UNCHECKED_CAST")

package sp.it.pl.ui.objects.window.stage

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser.GWL_STYLE
import com.sun.jna.platform.win32.WinUser.SMTO_NORMAL
import java.util.UUID
import javafx.beans.value.ObservableValue
import javafx.event.Event
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.geometry.Rectangle2D
import javafx.geometry.Side
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
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ListView
import javafx.scene.control.ScrollPane
import javafx.scene.control.TableView
import javafx.scene.control.TreeTableView
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode.F1
import javafx.scene.input.KeyCode.F2
import javafx.scene.input.KeyCode.F3
import javafx.scene.input.KeyCode.F4
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_DRAGGED
import javafx.scene.input.MouseEvent.MOUSE_PRESSED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Region
import javafx.scene.robot.Robot
import javafx.stage.Stage
import javafx.stage.WindowEvent
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import mu.KotlinLogging
import sp.it.pl.layout.WidgetIoManager
import sp.it.pl.layout.initialTemplateFactory
import sp.it.pl.layout.widgetFocused
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.contextMenuFor
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.picker.ContainerPicker
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.pl.ui.objects.window.Resize
import sp.it.pl.ui.objects.window.Resize.ALL
import sp.it.pl.ui.objects.window.Resize.E
import sp.it.pl.ui.objects.window.Resize.N
import sp.it.pl.ui.objects.window.Resize.NE
import sp.it.pl.ui.objects.window.Resize.NONE
import sp.it.pl.ui.objects.window.Resize.NW
import sp.it.pl.ui.objects.window.Resize.S
import sp.it.pl.ui.objects.window.Resize.SE
import sp.it.pl.ui.objects.window.Resize.SW
import sp.it.pl.ui.objects.window.Resize.W
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.isOpenChild
import sp.it.util.access.focused
import sp.it.util.access.showing
import sp.it.util.action.ActionManager.keyActionsComponent
import sp.it.util.action.ActionManager.keyManageLayout
import sp.it.util.action.ActionManager.keyShortcuts
import sp.it.util.action.ActionManager.keyShortcutsComponent
import sp.it.util.async.flowTimer
import sp.it.util.async.runFX
import sp.it.util.dev.fail
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.localDateTimeFromMillis
import sp.it.util.math.P
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventDown1
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1If
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.reactive.syncTrue
import sp.it.util.system.Os
import sp.it.util.ui.anchorPane
import sp.it.util.ui.centre
import sp.it.util.ui.findParent
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

private val logger = KotlinLogging.logger { }

fun Window.leftHeaderMenuIcon() = Icon(IconFA.CARET_DOWN, -1.0, "App Menu").styleclass("header-icon").onClickDo {
   contextMenuFor(APP).show(it, Side.BOTTOM, 0.0, 0.0)
}
fun Window.rightHeaderMenuIcon() = Icon(IconFA.CARET_DOWN, -1.0, "Window Menu").styleclass("header-icon").onClickDo {
   contextMenuFor(this@rightHeaderMenuIcon).show(it, Side.BOTTOM, 0.0, 0.0)
}

fun Window.installStartLayoutPlaceholder() {

   fun showStartLayoutPlaceholder() {
      var action = {}
      val p = Placeholder(IconUN(0x1f4c1), "Start with a simple click\n\nIf you are 1st timer, choose ${ContainerPicker.choiceForTemplate} > ${initialTemplateFactory.name}") { action() }
      action = {
         runFX(300.millis) {
            AppAnimator.closeAndDo(p) {
               runFX(500.millis) {
                  p.hide()
                  Robot().apply {
                     mouseMove(root.localToScreen(root.layoutBounds).centre.toPoint2D())
                     mouseClick(PRIMARY)
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

fun Stage.installWindowInteraction() = sceneProperty().syncNonNullWhile { it.installWindowInteraction() }

fun Parent.installWindowInteraction() = sceneProperty().syncNonNullWhile { it.installWindowInteraction() }

fun Scene.installWindowInteraction() = Subscription(
   // change volume on scroll
   onEventDown(SCROLL) {
      fun isScrollableContent(it: Node) = it is ScrollPane || it is ListView<*> || it is TableView<*> || it is TreeView<*> || it is TreeTableView<*>
      val isInScrollable = it.target.asIf<Node>()?.findParent(::isScrollableContent)!=null
      if (!isInScrollable) {
         if (it.isShortcutDown) {
            if (it.deltaY>0) APP.ui.incFontSize()
            else if (it.deltaY<0) APP.ui.decFontSize()
         } else {
            if (it.deltaY>0) APP.audio.volumeInc()
            else if (it.deltaY<0) APP.audio.volumeDec()
         }
      }
   },
   // show help hotkeys
   onEventDown(KEY_PRESSED) {
      if (it.isAltDown && it.code==F4) {
         window.ifNotNull { w ->
            w.hide()
            it.consume()
         }
      }
      if (!it.isAltDown && !it.isControlDown && !it.isShortcutDown && !it.isMetaDown) {
         if (it.code==F1 || it.code==keyShortcuts) {
            APP.actions.showShortcuts()
            it.consume()
         }
         if (it.code==F2 || it.code==keyShortcutsComponent) {
            root?.widgetFocused().ifNotNull(APP.actions::showShortcutsFor)
            it.consume()
         }
         if (it.code==F3 || it.code==keyActionsComponent) {
            root?.widgetFocused().ifNotNull { APP.ui.actionPane.orBuild.show(it) }
            it.consume()
         }
      }
   },
   // layout mode hotkeys
   onEventUp(KEY_PRESSED) {
      if (!it.isControlDown && !it.isShortcutDown && !it.isMetaDown && it.code==keyManageLayout) {
         APP.ui.isLayoutMode = !APP.ui.isLayoutMode
         it.consume()
      }
   }
)

fun Stage.installHideOnFocusLost(isAutohide: ObservableValue<Boolean>, hider: () -> Unit): Subscription {
   return focused attachFalse {
      if (isAutohide.value) {
         runFX(50.millis) {
            if (!isFocused && isShowing && !APP.ui.layoutMode.value && !isOpenChild())
               hider()
         }
      }
   }
}

fun Stage.resizeTypeForCoordinates(at: P): Resize {
   val widths = listOf(0.0 - 100.0, width/3.0, 2*width/3.0, width + 100.0).windowed(2, 1, false).map { it[0] to it[1] }
   val heights = listOf(0.0 - 100.0, height/3.0, 2*height/3.0, height + 100.0).windowed(2, 1, false).map { it[0] to it[1] }
   val areas = mutableListOf<Rectangle2D>()
   widths.forEach { (w1, w2) ->
      heights.forEach { (h1, h2) ->
         areas += Rectangle2D(x + w1, y + h1, w2 - w1, h2 - h1)
      }
   }
   val resizes = listOf(NW, W, SW, N, ALL, S, NE, E, SE)
   return areas.find { at.toPoint2D() in it }?.let { resizes[areas.indexOf(it)] } ?: NONE
}

fun Window.clone() {
   val w = toDb().copy(main = false).toDomain()
   w.s.onEventDown1(WindowEvent.WINDOW_SHOWING) { w.update() }
   w.show()
   w.setXYToCenter()
   WidgetIoManager.requestWidgetIOUpdate()
}

/** @return iff [Window.setNonInteractingOnBottom] is in effect */
val Window.isNonInteractingOnBottom: Boolean
   get() = "interaction" in properties

/** @return iff [Stage.setNonInteractingOnBottom] is in effect */
val Stage.isNonInteractingOnBottom: Boolean
   get() = "interaction" in properties

/** See [Stage.setNonInteractingOnBottom] */
fun Window.setNonInteractingOnBottom() = stage.setNonInteractingOnBottom()

/**
 * Sets window to be non-interactive and always at bottom (opposite of always on top).
 * Windows only, no-op on other platforms.
 *
 * @apiNote adjusts native window style. Based on: http://stackoverflow.com/questions/26972683/javafx-minimizing-undecorated-stage
 */
@Suppress("LocalVariableName", "SpellCheckingInspection")
fun Stage.setNonInteractingOnBottom() {
   if (!Os.WINDOWS.isCurrent) return

   properties["interaction"] = "none"
   isAlwaysOnTop = false
   showing syncTrue {
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
@OptIn(DelicateCoroutinesApi::class)
@Suppress("SpellCheckingInspection", "UNUSED_ANONYMOUS_PARAMETER")
fun Stage.setNonInteractingProgmanOnBottom() {
   if (!Os.WINDOWS.isCurrent) return

   showing.sync1If({ it }) {
      val logName = "Window $this"
      val user32 = User32.INSTANCE

      val titleOriginal = title
      val titleUnique = UUID.randomUUID().toString()
      title = titleUnique
      val hwnd = user32.FindWindow(null, titleUnique)   // find native window by title
      title = titleOriginal
      if (hwnd==null) logger.warn { "$logName getting hwnd failed with code=${Kernel32.INSTANCE.GetLastError()}" }
      if (hwnd==null) return@sync1If

      // Fetch the Progman window
      val progman = user32.FindWindow("Progman", null)

      // Send 0x052C to Progman
      // This message directs Progman to spawn a WorkerW behind the desktop icons. If it is already there, nothing happens.
      val r = user32.SendMessageTimeout(progman, 0x052C, null, WinDef.LPARAM(), SMTO_NORMAL, 1000, WinDef.DWORDByReference(WinDef.DWORD(0L)))
      if (r.equals(0)) logger.warn { "$logName Progman failed with code=${Kernel32.INSTANCE.GetLastError()}" }

      GlobalScope.launch(Dispatchers.JavaFx) {
         flowTimer(0, 150).cancellable().take(4).mapNotNull {
            logger.debug { "$logName getting workerW at ${System.currentTimeMillis().localDateTimeFromMillis()}" }
            // Get WorkerW window
            var workerW: WinDef.HWND? = null
            user32.EnumWindows(
               { tophandle, topparamhandle ->
                  user32.FindWindowEx(tophandle, null, "SHELLDLL_DefView", null).ifNotNull {
                     user32.FindWindowEx(null, tophandle, "WorkerW", null).ifNotNull {
                        workerW = it
                     }
                  }
                  true
               },
               null
            )
            if (workerW==null && it>1) logger.warn { "$logName getting workerW failed" }
            workerW
         }.firstOrNull().ifNotNull { workerW ->
            logger.debug { "$logName set workerW parent at ${System.currentTimeMillis().localDateTimeFromMillis()}" }
            // Set WorkerW as parent
            if (isShowing) {
               user32.SetParent(hwnd, workerW)
               // showingProperty().sync1If({!it}) { user32.CloseWindow(workerW  ) } // this breaks workerW search until system restart!
            }
         }
      }

   }
}

@Suppress("UNUSED_PARAMETER")
@SuppressWarnings
fun <T: Node> Node.lookupId(id: String, type: Class<T>): T = lookup("#$id") as T? ?: fail { "No match for id=$id" }

fun buildWindowLayout(onDragStart: (MouseEvent) -> Unit, onDragged: (MouseEvent) -> Unit, onDragEnd: (MouseEvent) -> Unit) = stackPane {
   id = "root"
   styleClass += Window.scWindow
   prefSize = 400 x 600

   lay += stackPane {
      id = "back"
      isMouseTransparent = true
      isPickOnBounds = false

      lay += stackPane {
         id = "backImage"
         styleClass += "bgr-image"
      }
   }

   lay += anchorPane {
      id = "front"

      layFullArea += vBox {
         id = "frontContent"
         initClip()

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

                  lay += label {
                     padding = Insets(0.0, 0.0, 0.0, 5.0)
                     id = "titleL"
                  }
               }

               lay(CENTER_RIGHT) += hBox(4.0, CENTER_RIGHT) {
                  id = "rightHeaderBox"
                  isFillHeight = false
                  isPickOnBounds = false

                  onEventDown(MOUSE_DRAGGED, Event::consume)
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
         onEventDown(MOUSE_DRAGGED, PRIMARY, true, onDragged)
         onEventDown(MOUSE_PRESSED, PRIMARY, true, onDragStart)
         onEventDown(MOUSE_RELEASED, PRIMARY, true, onDragEnd)
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


      layFullArea += vBox {
         id = "frontImage"
         styleClass += "fgr-image"
         isMouseTransparent = true
         isPickOnBounds = false
      }
   }
}