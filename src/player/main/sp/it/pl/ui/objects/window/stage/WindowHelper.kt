package sp.it.pl.ui.objects.window.stage

import javafx.stage.Window as WindowFX
import java.awt.Rectangle
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.geometry.Pos.CENTER
import javafx.geometry.Rectangle2D
import javafx.scene.Node
import javafx.stage.Stage
import javafx.stage.StageStyle.TRANSPARENT
import javafx.stage.StageStyle.UNDECORATED
import sp.it.pl.layout.Layout
import sp.it.pl.main.APP
import sp.it.pl.main.formEditorsUiToggleIcon
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.ui.objects.window.ShowArea.SCREEN_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.onIsShowing1
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.popWindow
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.NONE
import sp.it.pl.ui.pane.ConfigPane.Companion.compareByDeclaration
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.OrV.OrValue.Initial.Override
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.EditMode
import sp.it.util.conf.between
import sp.it.util.conf.butOverridden
import sp.it.util.conf.cOr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.defInherit
import sp.it.util.conf.readOnlyIf
import sp.it.util.conf.readOnlyUnless
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.toUnit
import sp.it.util.math.P
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.asDisposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.on
import sp.it.util.reactive.syncBiFrom
import sp.it.util.system.Os
import sp.it.util.ui.uiDelegate

/** Determines whether global window stage style setting is overridden for this window */
var Window.stageStyleOverride: Boolean
   get() = properties["stageStyleOverride"].asIf<Boolean>() ?: false
   set(value) = properties.put("stageStyleOverride", value).toUnit()

/** @return [Stage] associated with this javafx window or null if not [Stage]-based */
fun WindowFX.asStage() = asIf<Stage>()

/** @return application [Window] associated with this javafx window */
fun WindowFX.asAppWindow(): Window? = uiDelegate.asIf()

/** @return whether this [Window] */
fun WindowFX.isAppWindow() = asAppWindow()!=null

/** @return whether this [isAppWindow] and [Window.isMain] */
fun WindowFX.isMainWindow() = asAppWindow()?.isMain?.value==true

/** @return whether this [isAppWindow] and [Window.isMain] */
fun Window.isMainWindow() = this.isMain.value

/** @return application layout associated with this javafx window */
fun WindowFX.asLayout() = null
   ?: asAppWindow()?.layout
   ?: properties[Window.keyWindowLayout] as? Layout
   ?: scene?.root?.properties?.get(Window.keyWindowLayout) as? Layout

var WindowFX.popWindowOwner: WindowFX?
   get() = properties["popWindowOwner"].asIf()
   set(value) = properties.put("popWindowOwner", value).toUnit()

/** Currently shown window for [openWindowSettings] */
var Window.settingsWindow: PopWindow?
  get() = properties["settingsWindow"].asIs()
  private set(value) = properties.put("settingsWindow", value).toUnit()

/** Open ui settings pertaining to the specified window. */
fun openWindowSettings(w: Window, eventSource: Node?) {
   if (w.settingsWindow!=null) {
      w.settingsWindow?.focus()
      return
   }

   val onClose = Disposer()
   val c = object: ConfigurableBase<Any?>() {

      val main by cv(w.isMain.value, false).readOnlyIf(w.isMain)
         .def(name = "Main", info = "Whether this window is main. Closing main window closes the application if application is not set to allow no ui). At most one window can be main. Can only be set to true (which steals main from previous main window).")
      val opacity by cOr(w.opacity, Inherit()).butOverridden { between(0.1, 1.0) }
         .defInherit(APP.windowManager::windowOpacity)
      val transparencyAllow by cOr(APP.windowManager::windowStyleAllowTransparency, if (w.stageStyleOverride) Override(w.s.style==TRANSPARENT) else Inherit(), onClose)
         .defInherit(APP.windowManager::windowStyleAllowTransparency)
      val transparency by cOr(w.transparency, Inherit())
         .defInherit(APP.windowManager::windowTransparency)
      val effect by cOr(w.effect, Inherit())
         .defInherit(APP.windowManager::windowEffect)
      val headerAllowed by cv(w.isHeaderAllowed, true)
         .def(name = "Allow header", info = "Whether header can be visible. Some windows do not support header.", editable = EditMode.APP)
      val headerVisible by cv(w.isHeaderVisible, true).readOnlyUnless(w.isHeaderAllowed)
         .def(name = "Show header", info = "Whether header is visible. Otherwise it will auto-hide.")
      val taskbarVisible by cv(w.isTaskbarVisible, true)
         .def(name = "Show taskbar", info = "Whether window is displayed in OS taskbar.")
      val onTop by cv(w.alwaysOnTop, false)
         .def(name = "On top", info = "Window will stay in foreground when other window is being interacted with.")
      val onBottom by cv(w.isNonInteractingOnBottom, false).readOnlyUnless(Os.WINDOWS.isCurrent)
         .def(name = "On bottom", info = "Window will stay in background and never receive focus.")
      val fullscreen by cv(w.fullscreen, false)
         .def(name = "Fullscreen", info = "Window will stay in foreground and span entire screen.")
      val maximized by cv(w.maximized, NONE).readOnlyIf(w.fullscreen)
         .def(name = "Maximized", info = "Whether window is maximized to specific area of the screen.")

      init {
         fun recreate() = w.also { ow ->
            val nw = w.recreateWith(if (transparencyAllow.value) TRANSPARENT else UNDECORATED, taskbarVisible.value, onBottom.value)

            ow.opacity.value = 0.0
            nw.update()
            nw.show()
            nw.initLayout(ow.detachLayout())
            if (ow.isMain.value) APP.windowManager.setAsMain(nw)
            nw.stage.onIsShowing1 { ow.settingsWindow?.hide(); ow.close(); openWindowSettings(nw, nw.root) } on onClose
         }

         w.isMain attachFalse { main.value = it } on onClose
         main attachTrue { APP.windowManager.setAsMain(w) }

         transparencyAllow.override attach { w.stageStyleOverride = it } on onClose
         onBottom attachTrue { w.setNonInteractingOnBottom() } on onClose
         onBottom attachFalse  { recreate() } on onClose
         taskbarVisible attach { recreate() } on onClose
         transparencyAllow attach { recreate() } on onClose
      }
   }

   popWindow {
      w.settingsWindow = this
      onClose += { w.settingsWindow = null }

      val form = form(c, null).apply {
         editorOrder = compareByDeclaration
         editorUi syncBiFrom APP.ui.formLayout on onHidden.asDisposer()
         onExecuteDone = { if (it.isOk && isShowing) hide() }
      }

      content.value = form
      title.value = "Window settings"
      isAutohide.value = false
      headerIcons += formEditorsUiToggleIcon(form.editorUi)
      onHiding += onClose

      show(if (eventSource==null) SCREEN_ACTIVE(CENTER) else DOWN_CENTER(eventSource))
      form.focusFirstConfigEditor()
   }
}


fun bestRec(area: Rectangle2D, at: P, newW: Collection<Rectangle>): Bounds {
   data class TupleM4(var a: Double, var b: Double, var c: Double, var d: Double)
   val b = TupleM4(area.minX, area.width, area.minY, area.height)
   for (w in newW) {
      if (w===newW) continue  // ignore self
      val wl = w.x + w.width
      if (wl<at.x && wl>b.a) b.a = wl.toDouble()
      val wr = w.x
      if (wr>at.x && wr<b.b) b.b = wr.toDouble()
      val ht = w.y + w.height
      if (ht<at.y && ht>b.c) b.c = ht.toDouble()
      val hb = w.y
      if (hb>at.y && hb<b.d) b.d = hb.toDouble()
   }
   b.a = area.minX
   b.b = area.width
   for (w in newW) {
      if (w===newW) continue  // ignore self
      val wl = w.x + w.width
      val wr = w.x
      val ht = w.y + w.height
      val hb = w.y
      val inTheWay = !(ht<at.y && ht<=b.c || hb>at.y && hb>=b.d)
      if (inTheWay) {
         if (wl<at.x && wl>b.a) b.a = wl.toDouble()
         if (wr>at.x && wr<b.b) b.b = wr.toDouble()
      }
   }
   return BoundingBox(b.a, b.c, b.b - b.a, b.d - b.c)
}