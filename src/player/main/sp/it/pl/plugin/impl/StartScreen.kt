package sp.it.pl.plugin.impl

import sp.it.pl.core.CoreMouse as sys
import javafx.geometry.Point2D
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import sp.it.pl.layout.Layout
import sp.it.pl.layout.exportFxwl
import sp.it.pl.layout.introWidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.Events
import sp.it.pl.main.IconUN
import sp.it.pl.main.Key
import sp.it.pl.main.LazyOverlayPane
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.main.showAndDetect
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.hierarchy.Item.CoverStrategy.Companion.VT_IMAGE_THROTTLE
import sp.it.pl.ui.objects.window.Resize.NONE
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.installWindowInteraction
import sp.it.pl.ui.objects.window.stage.osHasWindowExclusiveFullScreen
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.OverlayPane.Display.SCREEN_OF_MOUSE
import sp.it.util.action.IsAction
import sp.it.util.async.coroutine.await
import sp.it.util.async.coroutine.runSuspendingFx
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.bool.TRUE
import sp.it.util.file.div
import sp.it.util.functional.ifNotNull
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.syncBiFrom
import sp.it.util.system.Os
import sp.it.util.ui.anchorPane
import sp.it.util.ui.areaBy
import sp.it.util.ui.lay
import sp.it.util.ui.pseudoClassToggle
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.millis

class StartScreen: PluginBase() {
   private var corners = screenCorners()
   private var mouseIn = false
   private var mouseXy = Point2D(0.0, 0.0)
   private val onClose = Disposer()

   private val overlaySleepHandler = Subscribed {
      APP.actionStream.onEventObject(Events.AppEvent.SystemSleepEvent.Start) { overlay.hide() }
   }
   private val overlayUserHandler = Subscribed {
      APP.actionStream.onEventObject(Events.AppEvent.UserSessionEvent.Stop) { overlay.hide() }
   }
   private val overlayIsActive = Subscribed {
      fun showerCondition() =
         APP.windowManager.dockWindow?.isShowing!=true &&
         APP.windowManager.windows.none { it.moving.value || it.resizing.value!=NONE } &&
         osHasWindowExclusiveFullScreen() != TRUE

      val shower = fxTimer(500.millis, 1) {
         if (showerCondition())
            overlay.orBuild.show(Unit)
      }
      overlaySleepHandler.subscribe()
      overlayUserHandler.subscribe()

      Subscription(
         sys.screens.onChange {
            corners = screenCorners()
         },
         sys.observeMousePosition { mp ->
            val moved = mp!=mouseXy
            val wasMouseIn = mouseIn
            mouseXy = mp
            if (!moved) {
               mouseIn = corners.any { mp in it }
               if (!wasMouseIn && mouseIn && showerCondition())
                  shower.start()
            }
         },
         Subscription {
            shower.stop()
            overlaySleepHandler.unsubscribe()
            overlayUserHandler.unsubscribe()
            onClose()
         }
      )
   }
   private var widgetLayout: Layout? = null
   private val widgetArea = anchorPane()

   private val overlay = LazyOverlayPane {
      object: OverlayPane<Unit>() {

         fun StackPane.installClipboardSupport() {
            onEventDown(KEY_PRESSED) {
               if (it.code==Key.V && it.isShortcutDown) {
                  it.consume()
                  APP.ui.actionPane.orBuild.showAndDetect(Clipboard.getSystemClipboard().getAny(), it)
               }
            }
            installDrag(
               IconUN(0xe295),
               "Drag & drop object",
               { true },
               { APP.ui.actionPane.orBuild.showAndDetect(it.dragboard.getAny(), it) }
            )
         }

         init {
            display.value = SCREEN_OF_MOUSE
            displayBgr syncBiFrom APP.ui.viewDisplayBgr on onClose
            content = stackPane {
               isFocusTraversable = true
               onShowed += { requestFocus() }
               pseudoClassToggle(PSEUDOCLASS_CONTENT_FULL_SIZE, true)
               installClipboardSupport()
               installWindowInteraction()

               lay += widgetArea.apply {
                  val ssComponentFile = APP.location.user.tmp/"StartScreen.fxwl"
                  val widgetSubscribed = Subscribed {
                     runSuspendingFx {
                        val ssComponent = APP.windowManager.instantiateComponent(ssComponentFile) ?: introWidgetFactory.create()

                        VT_IMAGE_THROTTLE.lockFor(150.millis)

                        Layout.openStandalone(this@apply).apply {
                           widgetLayout = this
                           widgetArea.scene.root.properties[Window.keyWindowLayout] = this
                           child = ssComponent
                           child?.focus()
                        }
                     }

                     Subscription {
                        runSuspendingFx {
                           widgetLayout.ifNotNull {
                              it.child.exportFxwl(ssComponentFile).await()
                              it.child?.close()
                              it.close()
                           }
                           widgetLayout = null
                        }
                     }
                  }
                  onShowed += { widgetSubscribed.subscribe(true) }
                  onHidden += { widgetSubscribed.subscribe(false) }
               }
            }
         }

         override fun show(data: Unit) = super.show()

      }
   }

   override fun start() = overlayIsActive.subscribe()

   override fun stop() = overlayIsActive.unsubscribe()

   @IsAction(name = "Toggle start screen", info = "Toggle start screen on/off", global = true)
   fun overlayToggleVisible() = if (overlay.isShown()) overlay.hide() else overlay.orBuild.show(Unit)

   private fun screenCorners() = Screen.getScreens().map {
      it.bounds.let { it.maxX x it.minY } areaBy (-20 x 20)
   }

   companion object: PluginInfo {
      override val name = "Start Screen"
      override val description = buildString {
         append("Provides start screen overlay similar to the one in Windows 8.").append(" ")
         append("The content is fully customizable like any ordinary window.").append(" ")
         append("The content supports pasing any data from clipboard and drag&drop as well.")
      }
      override val isSupported = Os.WINDOWS.isCurrent
      override val isSingleton = true
      override val isEnabledByDefault = false
   }
}