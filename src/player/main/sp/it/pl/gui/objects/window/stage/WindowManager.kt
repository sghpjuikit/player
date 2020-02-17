package sp.it.pl.gui.objects.window.stage

import javafx.collections.FXCollections.observableArrayList
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.layout.Region
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.StageStyle.UNDECORATED
import javafx.stage.StageStyle.UTILITY
import javafx.stage.WindowEvent.WINDOW_SHOWING
import javafx.util.Duration.ZERO
import mu.KLogging
import sp.it.pl.gui.objects.form.Form.Companion.form
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.gui.objects.window.ShowArea.WINDOW_ACTIVE
import sp.it.pl.gui.objects.window.popup.PopWindow
import sp.it.pl.layout.Component
import sp.it.pl.layout.ComponentDb
import sp.it.pl.layout.container.Layout
import sp.it.pl.layout.widget.NoFactoryFactory
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetLoader.CUSTOM
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.feature.HorizontalDock
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.main.emScaled
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.action.IsAction
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.future.Fut
import sp.it.util.async.runIO
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
import sp.it.util.conf.Configurable
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.between
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyUnless
import sp.it.util.conf.valuesIn
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.readTextTry
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.math.P
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventDown1
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.anchorPane
import sp.it.util.ui.borderPane
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.minus
import java.io.File
import java.util.HashSet
import java.util.Optional
import javafx.stage.Window as WindowFX
import sp.it.pl.main.AppSettings.plugins.screenDock as confDock
import sp.it.pl.main.AppSettings.ui.window as confWindow

class WindowManager: GlobalSubConfigDelegator(confWindow.name) {

   @JvmField var screenMaxScaling = 0.0
   /** Observable list of all application windows. For list of all windows use [javafx.stage.Stage.getWindows]. */
   @JvmField val windows = observableArrayList<Window>()!!
   /** Dock window or null if none. */
   @JvmField var dockWindow: Window? = null
   /** Main application window, see [sp.it.pl.gui.objects.window.stage.Window.isMain]. */
   private var mainWindow: Window? = null
   /** 128x128 icon of the application */
   private val windowIcon by lazy { Image(File("resources/icons/icon128.png").toURI().toString()) }

   val windowOpacity by cv(1.0).between(0.1, 1.0).def(name = "Opacity", info = "Window opacity.")
   val windowHeaderless by cv(false).def(name = "Headerless", info = "Affects window header visibility for new windows.")

   private var dockIsTogglingWindows = false
   private var dockHiddenWindows = ArrayList<Window>()
   private val dockToggleWindows = v(true).apply {
      attach {
         if (APP.isStateful) {
            if (it) {
               dockHiddenWindows.forEach { it.show() }
            } else {
               dockHiddenWindows setTo windows.asSequence().filter { it!==dockWindow }
               dockIsTogglingWindows = true
               dockHiddenWindows.forEach { it.hide() }
               dockIsTogglingWindows = false
            }
         }
      }
   }
   val dockHoverDelay by cv(700.millis).def(confDock.showDelay)
   val dockHideInactive by cv(true).def(confDock.hideOnIdle)
   val dockHideInactiveDelay by cv(1500.millis).def(confDock.hideOnIdleDelay).readOnlyUnless(dockHideInactive)
   private val dockWidgetInitialValue = PLAYBACK.withFeature<HorizontalDock>()
   val dockWidget by cv(dockWidgetInitialValue).def(confDock.content).valuesIn {
      APP.widgetManager.factories.getFactoriesWith<HorizontalDock>()
         .filter { it.id == dockWidgetInitialValue.id }
         .plus(dockWidgetInitialValue) // add if not available
   }
   val dockShow by cv(false).def(confDock.enable) sync { showDockImpl(it) }

   /** @return main window or null if no main window (only possible when no window is open) */
   fun getMain(): Optional<Window> = Optional.ofNullable(mainWindow)

   /** @return focused window or null if none focused */
   fun getFocused(): Optional<Window> = windows.stream().filter { it.focused.value }.findAny()

   /** @return focused window or [getMain] if none focused */
   fun getActive(): Optional<Window> = getFocused().or(::getMain)

   /** @return focused window or [getMain] if none focused or new window if no main window */
   fun getActiveOrNew(): Window = getActive().orElseGet(::createWindow)

   init {
      WindowFX.getWindows().onItemAdded { w ->
         w.asAppWindow().ifNotNull {
            if (it!==dockWindow) {
               windows += it
            }
         }
      }
      WindowFX.getWindows().onItemRemoved { w ->
         w.asAppWindow().ifNotNull {
            if (APP.isUiApp && it.isMain.value && !dockIsTogglingWindows) {
               APP.close()
            } else {
               windows -= it
            }
         }
      }

      APP.mouse.screens.onChangeAndNow {
         screenMaxScaling = Screen.getScreens().asSequence().map { it.outputScaleX max it.outputScaleY }.max() ?: 1.0
      }
   }

   /** Create and open small invisible window with empty content, minimal decoration and no taskbar icon. */
   fun createStageOwner(): Stage {
      return createStageOwnerNoShow().apply {
         show()
      }
   }
   fun createStageOwnerNoShow(): Stage {
      return Stage(UTILITY).apply {
         width = 10.0
         height = 10.0
         x = 0.0
         y = 0.0
         opacity = 0.0
         scene = Scene(anchorPane { })
         title = "${APP.name}-StageOwner"
      }
   }

   fun create(canBeMain: Boolean = APP.isUiApp) = create(null, UNDECORATED, canBeMain)

   fun create(owner: Stage?, style: StageStyle, canBeMain: Boolean): Window {
      val w = Window(owner, style)

      if (mainWindow==null && APP.isUiApp && canBeMain) setAsMain(w)

      w.initialize()

      w.stage.opacityProperty() syncFrom windowOpacity on w.onClose
      w.isHeaderVisible.value = windowHeaderless.value
      w.stage.title = APP.name
      w.stage.icons setToOne windowIcon

      return w
   }

   fun createWindow(canBeMain: Boolean): Window {
      logger.debug { "Creating default window" }

      return create(canBeMain).apply {
         setXYScreenCenter()
         initLayout()
         update()

         show()
         setXYToCenter(getScreenForMouse())
      }
   }

   @IsAction(name = "Open new window", info = "Opens new application window")
   fun createWindow(): Window = createWindow(APP.isUiApp)

   fun setAsMain(w: Window) {
      if (mainWindow===w) return
      mainWindow?.let { it.isMainImpl.value = false }
      mainWindow = w
      mainWindow?.let { it.isMainImpl.value = true }
   }

   @IsAction(name = "Close active window", keys = "CTRL+W", info = "Opens new application window")
   private fun closeActiveWindow() {
      getActive().orNull()?.close()
   }

   private fun showDockImpl(enable: Boolean) {
      if (!APP.isStateful) return
      if (!APP.isInitialized.isOk) {
         APP.onStarted += { showDockImpl(enable) }
         return
      }

      if (enable) {
         if (dockWindow?.isShowing==true) return

         val mw = dockWindow ?: create(createStageOwner(), UNDECORATED, false).apply {
            dockWindow = this

            resizable.value = true
            isAlwaysOnTop = true

            setSize(Screen.getPrimary().bounds.width, 40.0)
            APP.mouse.screens.onChangeAndNow {
               val s = Screen.getPrimary()
               setXYSize(s.bounds.minX, s.bounds.minY, s.bounds.width, height)
            } on onClose
         }
         val content = borderPane {
            center = stackPane {
               lay += anchorPane {
                  Layout.openStandalone(this).apply {
                     mw.s.properties[Window.keyWindowLayout] = this
                  }
               }
            }
            right = hBox(8.0) {
               alignment = CENTER_RIGHT
               isFillHeight = false
               padding = Insets(5.0, 5.0, 5.0, 25.0)

               lay += Icon(null, 13.0, "Show/hide other windows").onClickDo { dockToggleWindows.toggle() }.apply {
                  hoverProperty() sync { icon(if (it) IconFA.ANGLE_DOUBLE_DOWN else IconFA.ANGLE_DOWN) } on mw.onClose
               }
               lay += Icon(IconFA.CLOSE, 13.0, "Close dock").onClickDo { dockToggleWindows.value = true; dockShow.value = false }
            }
         }
         mw.setContent(content)
         mw.onClose += dockWidget sync {
            mw.s.asLayout()?.child?.close()
            mw.s.asLayout()?.child = APP.widgetManager.widgets.find(it.id, NEW(CUSTOM)) ?: NoFactoryFactory(it.id).create()
         }
         mw.onClose += {
            mw.s.asLayout()?.child?.close()
         }

         // show and apply state
         mw.show()
         mw.isHeaderAllowed.set(false)
         mw.update()
         mw.back.style = "-fx-background-size: cover;" // disallow bgr stretching
         mw.content.style = "-fx-background-color: -skin-pane-color;" // imitate widget area bgr

         // auto-hiding
         val mwh = mw.H.subtract(2) // leave 2 pixels visible
         val mwRoot = mw.stage.scene.root
         val showAnim = anim(200.millis) { mw.setY(-mwh.value*it, false) }

         val hider = fxTimer(ZERO, 1) {
            if (mw.y==0.0) {
               var d = showAnim.currentTime
               if (d==ZERO) d = 300.millis - d
               showAnim.stop()
               showAnim.rate = 1.0
               showAnim.playFrom(300.millis - d)
               showAnim.onFinished = EventHandler { content.isMouseTransparent = true }
            }
         }
         mwRoot.onEventUp(MouseEvent.ANY) {
            if (mwRoot.isHover) {
               hider.stop()
            } else {
               if (dockHideInactive.value)
                  hider.start(dockHideInactiveDelay.value)
            }
         }
         hider.runNow()

         val shower = fxTimer(ZERO, 1) {
            if (mw.y!=0.0 && mwRoot.isHover) {
               var d = showAnim.currentTime
               if (d==ZERO) d = 300.millis - d
               showAnim.stop()
               showAnim.rate = -1.0
               showAnim.playFrom(d)
               showAnim.onFinished = EventHandler { content.isMouseTransparent = false }
            }
         }
         mwRoot.onEventDown(MOUSE_CLICKED, SECONDARY) { hider.runNow() }
         mwRoot.onEventDown(MOUSE_CLICKED, PRIMARY) { shower.runNow() }
         mwRoot.onEventUp(MOUSE_ENTERED) { shower.start(dockHoverDelay.value) }
      } else {
         dockWindow?.close()
         dockWindow = null
      }
   }

   fun serialize() {
      failIfNotFxThread()

      // make sure directory is accessible
      val dir = APP.location.user.layouts.current
      if (!isValidatedDirectory(dir)) {
         logger.error { "Serializing windows failed. $dir not accessible." }
         return
      }

      val filesOld = dir.children().toSet()
      val ws = windows.filter { it!==dockWindow }
      logger.info { "Serializing ${ws.size} application windows" }

      // serialize - for now each window to its own file with .ws extension
      val sessionUniqueName = System.currentTimeMillis().toString()
      var isError = false
      val filesNew = HashSet<File>()
      for (i in ws.indices) {
         val w = ws[i]
         val f = dir/"window_${sessionUniqueName}_$i.ws"
         filesNew += f
         isError = isError or APP.serializerJson.toJson(WindowDb(w), f).isError
         if (isError) break
      }

      // remove unneeded files, either old or new session will remain
      (if (isError) filesNew else filesOld).forEach { it.delete() }
   }

   @ThreadSafe
   fun deserialize(): Fut<*> = runIO {
      logger.info { "Deserializing windows." }
      val dir = APP.location.user.layouts.current
      if (isValidatedDirectory(dir)) {
         val fs = dir.children().filter { it hasExtension "ws" }.toList()
         val ws = fs.mapNotNull { APP.serializerJson.fromJson<WindowDb>(it).orNull() }
         logger.info { "Deserialized ${fs.size}/${ws.size} windows." }
         ws
      } else {
         logger.error { "Deserializing windows failed: $dir not accessible." }
         listOf()
      }
   } ui {
      if (it.isEmpty()) {
         createWindow(true)
      } else {
         val ws = it.map { it.toDomain() }
         if (mainWindow==null) setAsMain(ws.first())
         ws.forEach { w -> w.s.onEventDown1(WINDOW_SHOWING) { w.update() } }
         ws.forEach { it.show() }
         Widget.deserializeWidgetIO()
      }
      getActive().orNull()?.focus()
   }

   fun showWindow(c: Component): Window {
      return create().apply {
         initLayout()
         setContent(c)
         c.focus()

         show()

         val screen = getScreenForMouse()
         val scrSize = screen.bounds.size
         val initialSize = scrSize/2.0
         val newSize = if (c is Widget) {
            val sizeOld = c.load().asIf<Region>()?.size ?: P(0.0, 0.0)
            val sizePref = c.load().asIf<Region>()?.prefSize ?: P(0.0, 0.0)
            val sizeDiff = sizePref - sizeOld
            P(
               if (sizePref.x>0) stage.size.x + sizeDiff.x else initialSize.x,
               if (sizePref.y>0) stage.size.y + sizeDiff.y else initialSize.y
            )
         } else {
            initialSize
         }
         size = newSize min scrSize
         setXYToCenter(screen)
      }
   }

   fun <T> showSettings(c: Configurable<T>, atNode: Node) {
      val form = form(c).apply {
         prefSize = 400.emScaled x 400.emScaled
      }
      PopWindow().apply {
         content.value = form
         title.value = if (c is Component) "${c.name} Settings" else "Settings"
         isAutohide.value = true
         show(DOWN_CENTER(atNode))
      }
   }

   fun showFloating(c: Widget): PopWindow {
      val l = Layout.openStandalone(anchorPane())
      val p = PopWindow().apply {
         content.value = l.root
         title.value = c.custom_name.value
         properties[Window.keyWindowLayout] = l
         onHiding += { properties -= Window.keyWindowLayout }
         onHiding += { l.close() }
      }

      l.child = c
      c.focus()

      p.show(WINDOW_ACTIVE(CENTER))
      return p
   }

   /** Create, show and return component specified by the specified factoryId. */
   fun launchComponent(factoryId: String): Component? = instantiateComponent(factoryId)?.apply { showWindow(this) }

   /** Create, show and return component specified by its launcher file. */
   fun launchComponent(launcher: File): Component? = instantiateComponent(launcher)?.apply { showWindow(this) }

   /** Create component specified by its factoryId. */
   fun instantiateComponent(factoryId: String): Component? {
      val f = null
         ?: APP.widgetManager.factories.getComponentFactoryByName(factoryId)
         ?: APP.widgetManager.factories.getFactory(factoryId).orNull()
      return f?.create()
   }

   /** Create component specified by the launcher file. */
   // TODO: put this on IO thread and reuse File.loadComponentFxwlJson()?
   fun instantiateComponent(launcher: File): Component? {
      if (!launcher.exists()) return null
      val launcherContainsName = launcher.useLines { it.count()==1 }

      return if (launcherContainsName) launcher.readTextTry().orNull()?.let(::instantiateComponent)
      else APP.serializerJson.fromJson<ComponentDb>(launcher).orNull()?.toDomain()
   }

   companion object: KLogging()

}