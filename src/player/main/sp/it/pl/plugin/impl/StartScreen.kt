package sp.it.pl.plugin.impl

import sp.it.pl.core.CoreMouse as sys
import javafx.geometry.Point2D
import javafx.scene.input.Clipboard
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import sp.it.pl.layout.container.Layout
import sp.it.pl.layout.exportFxwl
import sp.it.pl.layout.widget.introWidgetFactory
import sp.it.pl.main.APP
import sp.it.pl.main.Events
import sp.it.pl.main.IconUN
import sp.it.pl.main.Key
import sp.it.pl.main.LazyOverlayPane
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.isOpenChild
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.installWindowInteraction
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.OverlayPane.Display.SCREEN_OF_MOUSE
import sp.it.util.action.IsAction
import sp.it.util.async.FX
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.launch
import sp.it.util.async.runFX
import sp.it.util.file.div
import sp.it.util.functional.net
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attachFalse
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.system.Os
import sp.it.util.ui.anchorPane
import sp.it.util.ui.areaBy
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.x
import sp.it.util.units.millis

class StartScreen: PluginBase() {
   private var corners = screenCorners()
   private var mouseIn = false
   private var mouseXy = Point2D(0.0, 0.0)

   private val overlaySleepHandler = Subscribed {
      APP.actionStream.onEventObject(Events.AppEvent.SystemSleepEvent.Start) { overlay.hide() }
   }
   private val overlayUserHandler = Subscribed {
      APP.actionStream.onEventObject(Events.AppEvent.UserSessionEvent.Stop) { overlay.hide() }
   }
   private val overlayIsActive = Subscribed {
      val shower = fxTimer(500.millis, 1) {
         if (APP.windowManager.dockWindow?.isShowing!=true)
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
               if (!wasMouseIn && mouseIn) {
                  if (APP.windowManager.dockWindow?.isShowing!=true)
                     shower.start()
               }
            }
         },
         Subscription {
            shower.stop()
            overlaySleepHandler.unsubscribe()
            overlayUserHandler.unsubscribe()
         }
      )
   }
   private var widgetLayout: Layout? = null
   private val widgetArea = anchorPane()

   private val overlay = LazyOverlayPane {
      object: OverlayPane<Unit>() {

         fun Any?.showDataInfo() = APP.ui.actionPane.orBuild.show(this)

         fun StackPane.installClipboardSupport() {
            onEventDown(KEY_PRESSED) {
               if (it.code==Key.V && it.isShortcutDown) {
                  it.consume()
                  Clipboard.getSystemClipboard().getAny().showDataInfo()
               }
            }
            installDrag(
               IconUN(0xe295),
               "Drag & drop object",
               { true },
               { it.dragboard.getAny().showDataInfo() }
            )
         }

         init {
            display.value = SCREEN_OF_MOUSE
            content = stackPane {
               isFocusTraversable = true
               onShowed += { requestFocus() }
               installClipboardSupport()
               installWindowInteraction()
               isShowingWithFocus attachFalse {
                  runFX(50.millis) {
                     if (!display.value.isWindowBased() || scene?.window?.net { !it.isFocused && it.isShowing && !it.isOpenChild() }==true)
                        hide()
                  }
               }

               lay += widgetArea.apply {
                  val widgetSubscribed = Subscribed {

                     val ssComponentFile = APP.location.user.tmp/"StartScreen.fxwl"

                     FX.launch {
                        val ssComponent = APP.windowManager.instantiateComponent(ssComponentFile) ?: introWidgetFactory.create()

                        Layout.openStandalone(this@apply).apply {
                           widgetLayout = this
                           widgetArea.scene.root.properties[Window.keyWindowLayout] = this
                           child = ssComponent
                        }
                     }

                     Subscription {
                        widgetLayout?.child?.exportFxwl(ssComponentFile)?.block()
                        widgetLayout?.child?.close()
                        widgetLayout?.close()
                        widgetLayout = null
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
         append("The content is managed like in any other window, using containers and components.")
      }
      override val isSupported = Os.WINDOWS.isCurrent
      override val isSingleton = true
      override val isEnabledByDefault = false
   }
}