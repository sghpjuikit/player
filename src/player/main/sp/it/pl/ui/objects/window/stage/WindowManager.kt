package sp.it.pl.ui.objects.window.stage

import com.sun.javafx.scene.control.ContextMenuContent
import com.sun.jna.platform.win32.GDI32
import com.sun.jna.platform.win32.User32
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import javafx.geometry.Insets
import javafx.geometry.Orientation.VERTICAL
import javafx.geometry.Pos
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.ContextMenu
import javafx.scene.image.Image
import javafx.scene.input.DragEvent.DRAG_ENTERED
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyCode.Z
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_RELEASED
import javafx.scene.layout.Pane
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.stage.Screen
import javafx.stage.Stage
import javafx.stage.StageStyle
import javafx.stage.StageStyle.TRANSPARENT
import javafx.stage.StageStyle.UNDECORATED
import javafx.stage.StageStyle.UTILITY
import javafx.stage.Window as WindowFx
import javafx.stage.WindowEvent.WINDOW_SHOWING
import kotlin.math.sqrt
import kotlin.concurrent.withLock
import kotlinx.coroutines.invoke
import sp.it.pl.core.CoreMouse.onNextMouseMoveStop
import sp.it.pl.layout.Component
import sp.it.pl.layout.ComponentFactory
import sp.it.pl.layout.ComponentLoader.CUSTOM
import sp.it.pl.layout.Layout
import sp.it.pl.layout.NoFactoryFactory
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetIoManager
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.exportFxwl
import sp.it.pl.layout.loadComponentFxwlJson
import sp.it.pl.main.APP
import sp.it.pl.main.AppSettings.plugins.screenDock as confDock
import sp.it.pl.main.AppSettings.ui.window as confWindow
import sp.it.pl.main.IconFA
import sp.it.pl.main.Widgets.PLAYBACK
import sp.it.pl.main.emScaled
import sp.it.pl.main.formEditorsUiToggleIcon
import sp.it.pl.main.toUi
import sp.it.pl.main.windowOnTopIcon
import sp.it.pl.main.windowPinIcon
import sp.it.pl.ui.objects.form.Form.Companion.form
import sp.it.pl.ui.objects.hierarchy.Item.CoverStrategy.Companion.VT_IMAGE_THROTTLE
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.NodeShow.DOWN_CENTER
import sp.it.pl.ui.objects.window.dock.DockWindow
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.asPopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.popWindow
import sp.it.pl.ui.objects.window.stage.Window.BgrEffect
import sp.it.pl.ui.objects.window.stage.Window.Transparency
import sp.it.pl.ui.objects.window.stage.Window.pcTransparentBlur
import sp.it.pl.ui.objects.window.stage.Windows10WindowBlur.ACCENT_DISABLED
import sp.it.pl.ui.objects.window.stage.Windows10WindowBlur.ACCENT_ENABLE_ACRYLICBLURBEHIND
import sp.it.pl.ui.objects.window.stage.Windows10WindowBlur.ACCENT_ENABLE_BLURBEHIND
import sp.it.pl.ui.pane.OverlayPane.Companion.asOverlayWindow
import sp.it.pl.ui.pane.OverlayPane.Companion.isOverlayWindow
import sp.it.pl.ui.pane.ScreenBgrGetter
import sp.it.util.access.toggle
import sp.it.util.access.v
import sp.it.util.access.vAlways
import sp.it.util.action.IsAction
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.coroutine.FX
import sp.it.util.async.coroutine.VT
import sp.it.util.async.coroutine.await
import sp.it.util.async.coroutine.launch
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runFX
import sp.it.util.async.runLater
import sp.it.util.async.runVT
import sp.it.util.bool.TRUE
import sp.it.util.collections.ObservableListRO
import sp.it.util.collections.materialize
import sp.it.util.collections.observableList
import sp.it.util.collections.readOnly
import sp.it.util.collections.setToOne
import sp.it.util.conf.Configurable
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.between
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.Util.isValidatedDirectory
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.functional.asIf
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.functional.traverse
import sp.it.util.math.P
import sp.it.util.math.intersectsWith
import sp.it.util.math.isBelow
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.asDisposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.attachTo
import sp.it.util.reactive.on
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventDown1
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemAdded
import sp.it.util.reactive.onItemRemoved
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncBiFrom
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.zip
import sp.it.util.reactive.zip2
import sp.it.util.system.Os
import sp.it.util.text.keys
import sp.it.util.type.atomic
import sp.it.util.ui.anchorPane
import sp.it.util.ui.borderPane
import sp.it.util.ui.flowPane
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.size
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.y
import sp.it.util.units.millis

class WindowManager: GlobalSubConfigDelegator(confWindow.name) {

   @JvmField var screenMaxScaling = 1.0
   /** Observable list of all JavaFX application windows, i.e., [javafx.stage.Stage.getWindows]. */
   @JvmField val windowsFx: ObservableListRO<WindowFx> = WindowFx.getWindows().readOnly()
   /** Observable list of all application windows. For list of all windows use [javafx.stage.Stage.getWindows]. */
   @JvmField val windows = observableList<Window>()
   /** Dock window or null if none. */
   @JvmField var dockWindow: DockWindow? = null
   /** Main application window, see [sp.it.pl.ui.objects.window.stage.Window.isMain]. */
   private var mainWindow: Window? = null
   /** 128x128 icon of the application */
   private val windowIcon by lazy { Image(File("resources/icons/icon128.png").toURI().toString()) }

   /** Required by skins that want to use transparent background colors. Determines [windowStyle]. Default false. */
   val windowStyleAllowTransparency by cv(false).attach { APP.actions.showSuggestRestartNotification() }.def(
      name = "Window allow transparency",
      info = "Required by skins that want to use transparent backgrounds. May degrade performance. A window may override this setting. Changing globally requires application restart."
   )
   /** Global overridable value for [Window.transparency]. */
   val windowTransparency by cv(Transparency.OFF).def(
      name = "Window transparency",
      info = "Whether content decoration is forcefully transparent. Useful together with 'On top' window settings to get overlay windows."
   )
   /** Global overridable value for [Window.transparency]. */
   val windowEffect by cv(BgrEffect.OFF).def(
      name = "Window effect",
      info = "Global overridable value for window effect"
   )
   /** Whether [Window.transformBgrWithContent] is true. Applies to all windows. Default false. */
   val windowStyleBgrWithContentTransformation by cv(false).def(
      name = "Allow bgr transformations",
      info = "Enables depth effect, where the window background moves and zooms with the window content. A non uniform bgr needs to be set for the effect to be visible"
   )
   /** Window [StageStyle] set at window creation time. Determined by [windowStyleAllowTransparency]. */
   val windowStyle = windowStyleAllowTransparency
      .map { it.toWindowStyle() }
   /** Any new application window will be created and maintained with this [Window.opacity]. */
   val windowOpacity by cv(1.0).between(0.1, 1.0)
      .def(name = "Opacity", info = "Any new application window will be created and maintained with this opacity, unless overridden by the window.")
   /** Any new application window will be created with this [Window.isHeaderVisible]. */
   val windowHeaderless by cv(false)
      .def(name = "Headerless", info = "Affects window header visibility for new windows.")
   /** Any new application window will move/resize on ALT + MOUSE_DRAG if true. Default true on non-Linux platforms. */
   val windowInteractiveOnLeftAlt by cv(!Os.UNIX.isCurrent).def(
      name = "Interacts on ${keys("Alt")} + Mouse Drag",
      info = "Simulates Linux move/resize behavior. LMB Mouse Drag moves window. RMB Drag resizes window. RMB during move toggles maximize."
   )
   /** Automatically closes non-main window if it becomes empty. */
   val windowDisallowEmpty by cv(true)
      .def(name = "Allow empty window", info = "Automatically closes non-main window if it becomes empty.")

   val dockHoverDelay by cv(700.millis).def(confDock.showDelay)

   val dockShow by cv(false).def(confDock.enable) sync { showDockImpl(it) }

   val dockHeight by cv(40.0)

   /** @return main window or null if no main window (only possible when no window is open) */
   fun getMain(): Window? = mainWindow

   /** @return focused window or null if none focused */
   fun getFocused(): Window? = windows.find { it.focused.value }

   /** @return focused window or window with focused popup or null if none focused */
   fun getFocusedWithChild(): Window? = null
      ?: windows.find { it.focused.value }
      ?: windowsFx.find { it.isFocused }?.let {
         it.traverse { it.popWindowOwner ?: it.asIf<Stage>()?.owner }.mapNotNull { it.asAppWindow() }.firstOrNull()
      }

   /** @return focused javafx window or window with focused popup or null if none focused */
   fun getFocusedFxWithChild(): WindowFx? = null
      ?: windows.find { it.focused.value }?.stage
      ?: windowsFx.find { it.isFocused }?.takeIf { !it.isIgnoredAsOwner() }?.let {
         it.traverse { it.popWindowOwner ?: it.asIf<Stage>()?.owner }.takeWhile { !it.isIgnoredAsOwner() }.firstOrNull()
      }

   private fun WindowFx.isIgnoredAsOwner() = asPopWindow()?.ignoreAsOwner==true

   /** @return focused window or [getMain] if none focused */
   fun getActive(): Window? = getFocused() ?: getMain()

   /** @return focused window or [getMain] if none focused or new window if no main window */
   fun getActiveOrNew(): Window = getActive() ?: createWindow()

   private val serializing = ReentrantLock()
   private var isSerialized by atomic(true)
   private var isSerializedWithAppClose by c(false).noUi()

   init {
      windowsFx.onItemAdded { w ->
         w.asAppWindow().ifNotNull {
            if (it!==dockWindow?.window) {
               windows += it
            }
         }
      }
      windowsFx.onItemRemoved { w ->
         w.asAppWindow().ifNotNull {
            val isMain = it.isMain.value
            if (APP.isUiApp.value) {
               if (isMain) APP.close()
               else windows -= it
            } else {
               if (isMain) serialize(false)
               windows -= it
               if (isMain) windows.materialize().forEach { it.closeWithAnim() }
            }
         }
      }

      if (Os.WINDOWS.isCurrent)
         WindowFx.getWindows().onItemSyncWhile { w ->
            if (w.isOverlayWindow() && w.asOverlayWindow()!!.displayBgr.value!=ScreenBgrGetter.NONE)
               return@onItemSyncWhile Subscription { }

            val wTransparency = null
               ?: w.asAppWindow()?.transparency
               ?: w.asPopWindow()?.transparency
               ?: windowTransparency
            val wEffect = null
               ?: w.asAppWindow()?.effect
               ?: w.asPopWindow()?.effect
               ?: windowEffect
            val wBgr = null
               ?: w.asAppWindow()?.root?.backgroundProperty()
               ?: w.asPopWindow()?.root?.backgroundProperty()
               ?: w.asOverlayWindow()?.backgroundProperty()
               ?: w.asIf<ContextMenu>()?.scene?.root?.childrenUnmodifiable?.find { "context-menu" in it.styleClass }?.asIf<Pane>()?.children?.find { "context-menu" in it.styleClass }?.asIf<ContextMenuContent>()?.backgroundProperty()
               ?: w.sceneProperty().flatMap { it.rootProperty() }.flatMap { it.asIf<Region>()?.backgroundProperty() ?: vAlways(null) }

            wTransparency zip wEffect zip2 wBgr sync { (transparency, effect, bgr) ->
               runLater {
                  w.takeIf { it.isShowing }?.reflectHwnd().ifNotNull {
                     val b = when {
                        // user-set transparent window avoid blur
                        transparency!=Transparency.OFF -> ACCENT_DISABLED
                        // window does not need blur
                        bgr?.fills?.firstOrNull()?.fill?.isOpaque==true -> ACCENT_DISABLED
                        // window does not need blur
                        w.asStage()?.style.net { it!=null && it!=TRANSPARENT } -> ACCENT_DISABLED
                        // decide blur type
                        else -> when (effect) {
                           BgrEffect.OFF -> ACCENT_DISABLED
                           BgrEffect.BLUR -> ACCENT_ENABLE_BLURBEHIND
                           BgrEffect.ACRYLIC -> ACCENT_ENABLE_ACRYLICBLURBEHIND
                        }
                     }
                     it.alpha(0.0f) // remove visual artefact
                     it.applyBlur(b)
                     it.alpha(w.opacity.toFloat()) // restore opacity
                     w.asPopWindow()?.root?.pseudoClassChanged(pcTransparentBlur, b!=ACCENT_DISABLED)
                  }
               }
            }
         }

      APP.mouse.screens.onChangeAndNow {
         screenMaxScaling = Screen.getScreens().asSequence().map { it.outputScaleX max it.outputScaleY }.maxOrNull() ?: 1.0
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

   fun create(canBeMain: Boolean = APP.isUiApp.value, state: WindowDb? = null) = create(
      owner = state?.let { if (!it.isTaskbarVisible) APP.windowManager.createStageOwner() else null },
      style = (state?.transparent ?: windowStyleAllowTransparency.value).toWindowStyle(),
      canBeMain = state?.main ?: canBeMain
   )

   fun create(owner: Stage?, style: StageStyle, canBeMain: Boolean): Window {
      val w = Window(owner, style)

      if (mainWindow==null && APP.isUiApp.value && canBeMain) setAsMain(w)

      w.initialize()

      if (style == TRANSPARENT) w.s.scene.fill = Color.TRANSPARENT
      w.isHeaderVisible.value = windowHeaderless.value
      w.isInteractiveOnLeftAlt syncFrom windowInteractiveOnLeftAlt on w.onClose
      w.stage.title = APP.name
      w.stage.icons setToOne windowIcon
      w.transformBgrWithContent syncFrom windowStyleBgrWithContentTransformation on w.onClose

      return w
   }

   fun createWindow(canBeMain: Boolean): Window {
      logger.debug { "Creating default window" }

      return create(canBeMain).apply {
         setXYSizeInitial()
         initLayoutWithContainerSwitch()
         update()

         show()
         setXYToCenter(getScreenForMouse())
      }
   }

   @IsAction(name = "Open new window", info = "Opens new application window")
   fun createWindow(): Window = createWindow(APP.isUiApp.value)

   fun setAsMain(w: Window) {
      if (mainWindow===w) return
      mainWindow?.let { it.isMainImpl.value = false }
      mainWindow = w
      mainWindow?.let { it.isMainImpl.value = true }
   }

   private fun showDockImpl(enable: Boolean) {
      if (!APP.isStateful) return
      if (!APP.isInitialized.isOk) {
         APP.onStarted += { showDockImpl(enable) }
         return
      }

      if (enable) {
         if (dockWindow?.isShowing==true) return

         val mwAutohide = v(true)
         val mw = dockWindow ?: DockWindow(create(createStageOwner(), windowStyle.value, false)).apply {
            dockWindow = this
            window.apply {
               resizable.value = true
               alwaysOnTop.value = true
               root.styleClass += "window-dock"

               setSize(Screen.getPrimary().bounds.width, dockHeight.value, false)
               dockHeight attachTo H
               H attach { dockHeight.value = it.toDouble() }
            }
         }
         val content = borderPane {
            center = stackPane {
               lay += anchorPane {
                  Layout.openStandalone(this).apply {
                     mw.window.properties[Window.keyWindowLayout] = this
                  }
               }
            }
            right = flowPane {
               orientation = VERTICAL
               lay += vBox {
                  alignment = Pos.TOP_CENTER
                  lay += windowPinIcon(mwAutohide)
                  lay += windowOnTopIcon(mw.window)
               }
               lay += vBox {
                  alignment = Pos.BOTTOM_CENTER
                  lay += Icon().apply {
                     styleclass("header-icon")
                     tooltip("Close dock permanently")
                     onClickDo { dockShow.value = false }
                     hoverProperty() sync { icon(it, IconFA.ANGLE_DOUBLE_DOWN, IconFA.ANGLE_DOWN) } on mw.window.onClose
                  }
               }
            }
         }
         mw.window.setContent(content)

         // content
         val dockComponentFile = APP.location.user.tmp / "DockComponent.fxwl"
         launch(FX) {
            VT_IMAGE_THROTTLE.lockFor(150.millis)
            val dockComponent = APP.windowManager.instantiateComponent(dockComponentFile) ?: APP.widgetManager.widgets.find(PLAYBACK.id, NEW(CUSTOM))
            mw.window.stage.asLayout()?.child = dockComponent ?: NoFactoryFactory(PLAYBACK.id).create()
            mw.window.onClose += {
               mw.window.stage.asLayout()?.child.exportFxwl(dockComponentFile).block()
               mw.window.stage.asLayout()?.child?.close()
            }
         }

         // show and apply state
         mw.window.show()
         mw.window.isHeaderAllowed.value = false
         mw.window.update()
         mw.window.back.style = "-fx-background-size: cover;" // disallow bgr stretching
         mw.window.content.style = "-fx-background-color: -skin-pane-color;" // imitate widget area bgr

         // auto-hiding
         val mwShower = object: Any() {
            private var showValue = 0.0 // 0..1, anything between means the window is transitioning to hide or show
            private val showAnim = anim(300.millis) {
               showValue = it
               mw.isShowing = it!=0.0
               mw.window.stage.opacity = mw.window.opacity.value min 0.1 + 0.9*sqrt(sqrt(it))
               mw.window.setY(computeY(), false)
               mw.window.resizable.value = it!=0.0
            }
            private val shower = {
               showAnim.intpl { sqrt(sqrt(it)) }
               showAnim.playOpenDo {
                  content.isMouseTransparent = false
                  mw.window.focus()
               }
            }
            private val hider = {
               showAnim.intpl { it*it*it*it }
               showAnim.playCloseDo {
                  content.isMouseTransparent = true
               }
            }

            fun computeY() = -(mw.window.H.value-2.0)*(1.0-showValue)
            fun showing() = showValue
            fun hide() = if (showValue==1.0) hider() else Unit
            fun show() = if (showValue==0.0 && windowsFx.none { it.isOverlayWindow() } && osHasWindowExclusiveFullScreen()!=TRUE) shower() else Unit
            fun showWithDelay() = if (showValue==0.0) onNextMouseMoveStop(150.millis, dockHoverDelay.value) { if (!it) show() } else Unit
            fun showInitially() = showAnim.applyAt(0.0).toUnit()

            init {
               APP.mouse.screens.onChangeAndNow {
                  val s = Screen.getPrimary()
                  mw.window.setXYSize(s.bounds.minX, computeY(), s.bounds.width, mw.window.height, false)
               } on mw.window.onClose
            }
         }
         mw.window.closeWithAnim = Runnable { mwShower.hide() }
         mw.window.stage.installHideOnFocusLost(mwAutohide) { mwShower.hide() }
         mw.window.stage.scene.root.onEventDown(DRAG_ENTERED) { mwShower.show() }
         mw.window.stage.scene.root.onEventDown(KEY_RELEASED, ESCAPE) { mwShower.hide() }
         mw.window.stage.scene.root.onEventDown(KEY_RELEASED, SPACE) { mwShower.show() }
         mw.window.stage.scene.root.onEventDown(MOUSE_CLICKED, SECONDARY) { if (!it.isPrimaryButtonDown && !APP.ui.isLayoutMode) mwShower.hide() }
         mw.window.stage.scene.root.onEventDown(MOUSE_RELEASED, SECONDARY) { if (!it.isPrimaryButtonDown && !APP.ui.isLayoutMode) mwShower.hide() }
         mw.window.stage.scene.root.onEventDown(MOUSE_CLICKED, PRIMARY) { mwShower.show() }
         mw.window.stage.scene.root.onEventUp(MOUSE_ENTERED) { mwShower.showWithDelay() }
         mw.window.stage.scene.root.onEventDown(KEY_RELEASED, Z, consume = false) {
            if (it.isMetaDown) {
               mwAutohide.toggle()
               it.consume()
            }
         }
         mwShower.showInitially()
      } else {
         dockWindow?.window?.close()
         dockWindow = null
      }
   }

   fun serialize(appCloses: Boolean): Fut<Unit> {
      failIfNotFxThread()
      isSerializedWithAppClose = !appCloses
      serializing.withLock {
         // prevent serializing multiple times, in ui-less mode this can overwrite serialized state with empty
         if (isSerialized) {
            logger.info { "Serializing windows skipped. Already done." }
            return fut()
         }

         // make sure directory is accessible
         val dir = APP.location.user.layouts.current
         if (!isValidatedDirectory(dir)) {
            logger.error { "Serializing windows failed. $dir not accessible." }
            return fut()
         }

         val filesOld = dir.children().toSet()
         val ws = windows.filter { it!==dockWindow?.window && it.layout!=null }.map { WindowDb(it) }
         logger.info { "Serializing ${ws.size} application windows" }

         return runVT {
            // serialize - for now each window to its own file with .ws extension
            val sessionUniqueName = System.currentTimeMillis().toString()
            var isError = false
            val filesNew = HashSet<File>()
            for (i in ws.indices) {
               val w = ws[i]
               val f = dir/"window_${sessionUniqueName}_$i.ws"
               filesNew += f
               isError = isError or APP.serializerJson.toJson(w, f).isError
               if (isError) break
            }

            // remove unneeded files, either old or new session will remain
            (if (isError) filesNew else filesOld).forEach { it.delete() }

            isSerialized = true
         }
      }
   }

   @ThreadSafe
   fun deserialize(force: Boolean = false): Fut<*> {
      serializing.lock()

      // prevent deserializing multiple times, in ui-less mode this can overwrite serialized state with empty
      if (!isSerialized) {
         logger.info { "Deserializing windows skipped. Already done." }
         return fut()
      }

      // prevent deserializing multiple times, in ui-less mode this can overwrite serialized state with empty
      if (!APP.isStateful) {
         logger.info { "Deserializing windows skipped. App not stateful." }
         return fut()
      }

      // prevent deserializing prematurely
      if (!force && !APP.isUiApp.value && !isSerializedWithAppClose) {
         logger.info { "Deserializing windows skipped. Not forced and not serializedWithAppClose." }
         return fut()
      }

      return runVT {
         logger.info { "Deserializing windows..." }
         val dir = APP.location.user.layouts.current
         if (isValidatedDirectory(dir)) {
            val fs = dir.children().filter { it hasExtension "ws" }.toList()
            val ws = fs.mapNotNull { APP.serializerJson.fromJson<WindowDb>(it).orNull() }
            logger.info { "Deserializing windows ok: ${ws.size}/${fs.size}" }
            ws
         } else {
            logger.error { "Deserializing windows failed: $dir not accessible" }
            listOf()
         }
      }.ui { it ->
         if (it.isEmpty()) {
            if (APP.isUiApp.value)
               createWindow(true)
         } else {
            val ws = it.map { it.toDomain() }
            if (mainWindow==null) setAsMain(ws.first())
            ws.forEach { w -> w.s.onEventDown1(WINDOW_SHOWING) { w.update() } }
            ws.forEach { it.show() }
            WidgetIoManager.requestWidgetIOUpdate()
         }
         getActive()?.focus()
      }.onDone {
         isSerialized = false
         serializing.unlock()
      }
   }

   /** @return whether [deserialize] would have any effect */
   @ThreadSafe
   fun isDeserializeUiPossible() = isSerialized

   fun slideWindow(c: Component): Window {
      val screen = getScreenForMouse()
      val showSide = if (Screen.getScreens().filter { it!==screen && it.bounds.y intersectsWith screen.bounds.y }.all { it.bounds.x isBelow screen.bounds.x }) Side.RIGHT else Side.LEFT
      val mwAutohide = v(true)
      val mw = create(createStageOwner(), windowStyle.value, false).apply {
         resizable.value = true
         alwaysOnTop.value = true
         isHeaderAllowed.value = false
         root.styleClass += "window-slide"

         // show and apply state
         stage.opacity = 0.0
         show()
         update()
         back.style = "-fx-background-size: cover;" // disallow bgr stretching
         content.style = "-fx-background-color: -skin-pane-color;" // imitate widget area bgr
      }
      val content = borderPane {

         fun windowDecoration() = flowPane {
            orientation = VERTICAL
            lay += vBox {
               alignment = Pos.TOP_CENTER
               lay += windowPinIcon(mwAutohide)
               lay += windowOnTopIcon(mw)
            }
         }

         center = stackPane {
            padding = Insets(20.emScaled)
            lay += anchorPane {
               Layout.openStandalone(this).apply {
                  mw.properties[Window.keyWindowLayout] = this
               }
            }
         }
         left = if (showSide!=Side.LEFT) null else windowDecoration()
         right = if (showSide!=Side.RIGHT) null else windowDecoration()
      }
      mw.setContent(content)
      mw.onClose += { mw.stage.asLayout()?.child?.close() }

      // auto-hiding
      val mwh by lazy { mw.stage.lookupHwnd()!! }
      val showAnim = anim(400.millis) {
         val x = if (showSide==Side.RIGHT) screen.bounds.maxX-mw.W.value*it
                 else screen.bounds.minX-mw.W.value*(1-it)

         mw.stage.opacity = mw.opacity.value min 0.1 + 0.9*sqrt(sqrt(it))
         mw.resizable.value = it!=0.0
         mw.setX(x, false)

         // clip window to screen (helps with 3 monitor setup)
         if (Os.WINDOWS.isCurrent) {
            val clip = if (it==1.0) null else GDI32.INSTANCE.CreateRectRgn(if (showSide==Side.RIGHT) 0 else (mw.W.value*(1-it)).toInt(), 0, (mw.W.value).toInt(), mw.height.toInt())
            User32.INSTANCE.SetWindowRgn(mwh, clip, false)
         }
      }
      val shower = {
         VT_IMAGE_THROTTLE.lockFor(500.millis)
         mw.s.asLayout()?.child = c
         mw.focus()
         c.focus()
         runFX(100.millis) {
            showAnim.intpl { sqrt(sqrt(it)) }
            showAnim.playOpenDo {}
         }
      }
      val hider = {
         showAnim.intpl { 1-sqrt(sqrt(1-it)) }
         showAnim.playCloseDo { mw.close() }
      }

      mw.autosize(c, screen)
      mw.stage.height = screen.bounds.height
      mw.stage.width = mw.stage.width min screen.bounds.width/2
      mw.stage.y = screen.bounds.minY
      shower()

      runFX(300.millis) {
         mw.closeWithAnim = Runnable { hider() }
         mw.stage.installHideOnFocusLost(mwAutohide, hider)
         mw.stage.scene.root.onEventDown(KEY_PRESSED, ESCAPE) { hider() }
         mw.stage.scene.root.onEventDown(MOUSE_CLICKED, SECONDARY) { if (!it.isPrimaryButtonDown && !APP.ui.isLayoutMode) hider() }
         mw.stage.scene.root.onEventDown(KEY_RELEASED, Z, consume = false) {
            if (it.isMetaDown) {
               mwAutohide.toggle()
               it.consume()
            }
         }
      }

      return mw
   }

   fun showWindow(c: ComponentFactory<*>): Unit = launch(FX) { showWindow(c.create()) }.toUnit()

   fun showWindow(c: Component): Window {
      return create().apply {
         initLayoutWithContainerUniWith(c)
         show()

         val screen = getScreenForMouse()
         autosize(c, screen)
         setXYToCenter(screen)
      }
   }

   private fun Window.autosize(c: Component, s: Screen) {
      val scrSize = s.bounds.size
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
   }

   /**
    * Shows [sp.it.pl.ui.objects.form.Form] in new [PopWindow] for the specified configurable.
    * The popup title shows the name of the configurable, using [toUi].
    * As such, providing custom name is possible by implementing [sp.it.pl.core.NameUi].
    */
   fun <T> showSettings(c: Configurable<T>, atNode: Node) = popWindow {
      title.value = "${c.toUi()} Settings"
      content.value = form(c).apply {
         prefSize = 600.emScaled x 600.emScaled
         editorUi syncBiFrom APP.ui.formLayout on onHidden.asDisposer()
         headerIcons += formEditorsUiToggleIcon(editorUi)
      }
      show(DOWN_CENTER(atNode))
   }

   /** Create, show and return component specified by the specified factoryId. */
   suspend fun launchComponent(factoryId: String): Component? = FX {
      instantiateComponent(factoryId).ifNotNull { showWindow(it) }
   }

   /** Create, show and return component specified by its launcher file. */
   suspend fun launchComponent(launcher: File): Component? = instantiateComponent(launcher)?.apply { showWindow(this) }

   /** Create component specified by its factoryId. */
   suspend fun instantiateComponent(factoryId: String): Component? = FX {
      val f = null
         ?: APP.widgetManager.factories.getComponentFactoryByName(factoryId)
         ?: APP.widgetManager.factories.getFactory(factoryId).orNull()
      f?.create()
   }

   /** Create component specified by the launcher file. */
   suspend fun instantiateComponent(launcher: File): Component? = FX {
      val id = VT {
         if (!launcher.exists()) null
         else runTry { launcher.useLines { it.take(2).toList().net { if (it.size==1 && !it[0].trimStart().startsWith("{")) it[0] else "" } } }.orNull()
      }

      when (id) {
         null -> null
         "" -> launcher.loadComponentFxwlJson().await()?.apply { fileDeserializing = launcher }
         else -> instantiateComponent(id)
      }
   }

   companion object {
      private val logger = KotlinLogging.logger { }
      private fun Boolean.toWindowStyle(): StageStyle = if (this) TRANSPARENT else UNDECORATED
   }

}