package sp.it.pl.plugin.impl

import javafx.geometry.Insets
import javafx.geometry.Point2D
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.TOP_RIGHT
import javafx.geometry.Side
import javafx.scene.text.TextBoundsType.VISUAL
import javafx.stage.Screen
import sp.it.pl.main.LazyOverlayPane
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.OverlayPane.Display.SCREEN_OF_MOUSE
import sp.it.pl.main.IconWH
import sp.it.pl.main.emScaled
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.JavaLegacy
import sp.it.util.action.IsAction
import sp.it.util.animation.Loop
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.reactive.Handler0
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.onChange
import sp.it.util.system.Os
import sp.it.util.ui.areaBy
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.millis
import java.awt.Desktop
import java.awt.desktop.SystemSleepEvent
import java.awt.desktop.SystemSleepListener
import java.awt.desktop.UserSessionEvent
import java.awt.desktop.UserSessionListener
import java.time.LocalDateTime
import java.time.format.TextStyle.FULL
import java.util.Locale.ENGLISH
import sp.it.pl.core.CoreMouse as sys

class StartScreen: PluginBase() {
   private var corners = screenCorners()
   private var mouseIn = false
   private var mouseXy = Point2D(0.0, 0.0)

   private val overlaySleepHandler = object: SystemSleepListener {
      override fun systemAwoke(e: SystemSleepEvent?) {}
      override fun systemAboutToSleep(e: SystemSleepEvent?) = overlay.hide()
   }
   private val overlayUserHandler = object: UserSessionListener {
      override fun userSessionActivated(e: UserSessionEvent?) {}
      override fun userSessionDeactivated(e: UserSessionEvent?) = overlay.hide()
   }
   private val overlayIsActive = Subscribed {
      val shower = fxTimer(500.millis, 1) { overlay.orBuild.show(Unit) }
      Desktop.getDesktop().addAppEventListener(overlaySleepHandler)
      Desktop.getDesktop().addAppEventListener(overlayUserHandler)

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
               if (!wasMouseIn && mouseIn) shower.start()
            }
         },
         Subscription {
            shower.stop()
            Desktop.getDesktop().removeAppEventListener(overlaySleepHandler)
            Desktop.getDesktop().removeAppEventListener(overlayUserHandler)
         }
      )
   }

   private val overlay = LazyOverlayPane {
      object: OverlayPane<Unit>() {

         init {
            display.value = SCREEN_OF_MOUSE
            content = stackPane {
               lay += stackPane {
                  padding = Insets(60.emScaled)

                  lay(BOTTOM_RIGHT) += vBox(15.emScaled, BOTTOM_RIGHT) {
                     isFillWidth = false

                     var time = LocalDateTime.MIN
                     val update = Handler0()
                     val loop = Loop(Runnable {
                        time = LocalDateTime.now().also {
                           if (time.minute!=it.minute)
                              update()
                        }
                     })
                     onShown += { time = LocalDateTime.now() }
                     onShown += update
                     onShown += loop::start
                     onHidden += loop::stop

                     lay += hBox(15.emScaled, BOTTOM_RIGHT) {
                        lay += text {
                           boundsType = VISUAL
                           style += "-fx-font-size: 2em"
                           update += { text = if (time.hour<12) "AM" else "PM" }
                        }
                        lay += text {
                           boundsType = VISUAL
                           style += "-fx-font-size: 6em"
                           update += { text = "%d:%02d".format(time.hour%12, time.minute) }
                        }
                     }
                     lay += text {
                        boundsType = VISUAL
                        style += "-fx-font-size: 2em"
                        update += { text = "%s, %s %d".format(time.dayOfWeek.getDisplayName(FULL, ENGLISH), time.month.getDisplayName(FULL, ENGLISH), time.dayOfMonth) }
                     }
                  }
                  lay(TOP_RIGHT) += vBox(10, TOP_RIGHT) {
                     isFillWidth = false
                     style += "-fx-font-size: 1.5em;"

                     lay += Icon(IconWH.MOON_27, 5.em.emScaled).onClickDo {
                        JavaLegacy.suspendWindows(false, false, true)
                     }.withText(Side.LEFT, "Sleep")
                     lay += Icon(IconWH.MOON_ALT_WANING_CRESCENT_1, 5.em.emScaled).onClickDo {
                        JavaLegacy.suspendWindows(true, false, true)
                     }.withText(Side.LEFT, "Hibernate")
                     lay += Icon(IconWH.MOON_14, 5.em.emScaled).onClickDo {
                        Runtime.getRuntime().exec("shutdown -s -t 0")
                     }.withText(Side.LEFT, "Shutdown")
                  }
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
      override val description = "Provides start screen overlay similar to the one in Windows 8"
      override val isSupported = Os.WINDOWS.isCurrent
      override val isSingleton = true
      override val isEnabledByDefault = false
   }
}