package sp.it.pl.ui.objects.window.stage

import javafx.stage.Window as WindowFX
import javafx.geometry.Pos
import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.stage.StageStyle.TRANSPARENT
import javafx.stage.StageStyle.UNDECORATED
import sp.it.pl.layout.container.Layout
import sp.it.pl.main.APP
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.ui.objects.window.ShowArea
import sp.it.pl.ui.objects.window.ShowArea.SCREEN_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.onIsShowing1st
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
import sp.it.util.functional.toUnit
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.attachTo
import sp.it.util.reactive.attachTrue
import sp.it.util.reactive.on

/** Determines whether global window opacity setting is overridden for this window */
var Window.opacityOverride: Boolean
   get() = properties["opacityOverride"].asIf<Boolean>() ?: false
   set(value) = properties.put("opacityOverride", value).toUnit()

/** Determines whether global window stage style setting is overridden for this window */
var Window.stageStyleOverride: Boolean
   get() = properties["stageStyleOverride"].asIf<Boolean>() ?: false
   set(value) = properties.put("stageStyleOverride", value).toUnit()

/** @return application window associated with this javafx window */
fun WindowFX.asAppWindow() = properties[Window.keyWindowAppWindow] as? Window

/** @return application layout associated with this javafx window */
fun WindowFX.asLayout() = null
   ?: properties[Window.keyWindowLayout] as? Layout
   ?: scene?.root?.properties?.get(Window.keyWindowLayout) as? Layout

var WindowFX.popWindowOwner: WindowFX?
   get() = properties["popWindowOwner"].asIf()
   set(value) = properties.put("popWindowOwner", value).toUnit()

/** Open ui settings pertaining to the specified window. */
fun openWindowSettings(w: Window, eventSource: Node?) {
   val onClose = Disposer()
   val c = object: ConfigurableBase<Any?>() {

      val main by cv(w.isMain.value).readOnlyIf(w.isMain)
         .def(name = "Main", info = "Whether this window is main. Closing main window closes the application. At most one window can be main. Can only be set to true.")
      val opacity by cOr(APP.windowManager::windowOpacity, if (w.opacityOverride) Override(w.opacity.value) else Inherit(), onClose).butOverridden { between(0.1, 1.0) }
         .defInherit(APP.windowManager::windowOpacity)
      val transparency by cOr(APP.windowManager::windowStyleAllowTransparency, if (w.stageStyleOverride) Override(w.s.style==TRANSPARENT) else Inherit(), onClose)
         .defInherit(APP.windowManager::windowStyleAllowTransparency)
      val headerAllowed by cv(w.isHeaderAllowed)
         .def(name = "Allow header", info = "Whether header can be visible. Some windows do not support header.", editable = EditMode.APP)
      val headerVisible by cv(w.isHeaderVisible).readOnlyUnless(w.isHeaderAllowed)
         .def(name = "Show header", info = "Whether header is visible. Otherwise it will auto-hide.")

      init {
         w.isMain attachFalse { main.value = it } on onClose
         main attachTrue { APP.windowManager.setAsMain(w) }

         opacity.override attach { w.opacityOverride = it } on onClose
         opacity attachTo w.opacity on onClose

         transparency.override attach { w.stageStyleOverride = it } on onClose
         transparency attach {
            w.also { ow ->
               val nw = w.recreateWith(if (it) TRANSPARENT else UNDECORATED, false)

               ow.opacity.value = 0.0
               nw.update()
               nw.show()
               nw.initLayout(ow.detachLayout())
               if (ow.isMain.value) APP.windowManager.setAsMain(nw)
               nw.stage.onIsShowing1st {
                  ow.close()
                  openWindowSettings(nw, nw.root)
               }
            }
         }
      }
   }

   PopWindow().apply {
      val form = form(c, null).apply {
         editorOrder = compareByDeclaration
         onExecuteDone = { if (it.isOk && isShowing) hide() }
      }

      content.value = form
      title.value = "Window settings"
      isAutohide.value = false
      onHiding += onClose
      show(if (eventSource==null) SCREEN_ACTIVE(CENTER) else DOWN_CENTER(eventSource))

      form.focusFirstConfigEditor()
   }
}