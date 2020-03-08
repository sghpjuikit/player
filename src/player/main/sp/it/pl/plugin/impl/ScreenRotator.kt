package sp.it.pl.plugin.impl

import javafx.geometry.Insets
import javafx.geometry.Pos.BOTTOM_CENTER
import javafx.geometry.Pos.BOTTOM_RIGHT
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_LEFT
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.StackPane
import javafx.stage.Screen
import mu.KLogging
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.window.ShowArea.SCREEN_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.util.access.Values.previous
import sp.it.util.action.IsAction
import sp.it.util.async.runIO
import sp.it.util.dev.fail
import sp.it.util.functional.runTry
import sp.it.util.reactive.onEventUp
import sp.it.util.system.Os
import sp.it.util.system.open
import sp.it.util.ui.centre
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.screen
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.em
import javafx.scene.input.KeyCode as Key

class ScreenRotator: PluginBase() {
   private val programFile = getResource(PROGRAM_FILE_NAME)
   private val programHelpFile = getResource(PROGRAM_HELP_FILE_NAME)

   @IsAction(name = "Open help", info = "Open technical usage help")
   fun openHelp() = programHelpFile.open()

   @IsAction(name = "Turn off screens", info = "Turns off all screens. They can be turned on again by moving the mouse", global = true, keys = "CTRL+SHIFT+ALT+K")
   fun turnScreensOff() = turnScreens(false)

   fun turnScreens(on: Boolean) {
      runIO {
         runTry {
            ProcessBuilder(programFile.path, if (on) "/power:on" else "/power:off").directory(userLocation).start()
         }.ifError {
            logger.error(it) { "Failed turn screens ${if (on) "on" else "off"}" }
         }
      }
   }

   @IsAction(name = "Start screen saver", info = "Starts screen saver if enabled. It can be stopped by moving the mouse. On some system this can open logon screen", global = true, keys = "CTRL+SHIFT+ALT+L")
   fun startScreenSaver() {
      runIO {
         runTry {
            ProcessBuilder(programFile.path, "/power:saver").directory(userLocation).start()
         }.ifError {
            logger.error(it) { "Failed to start screen saver" }
         }
      }
   }

   fun rotateScreen(screen: Screen, direction: Dir) {
      runIO {
         runTry {
            val scr = indexOfScreen(screen)
            ProcessBuilder(programFile.path, "/rotate:${direction.command}", "/device:$scr").directory(userLocation).start()
         }.ifError {
            logger.error(it) { "Failed to rotate display" }
         }
      }
   }

   @IsAction(name = "Rotate screen", info = "Show 'Rotate Screen' dialog", global = true, keys = "ALT+Semicolon")
   fun showRotateScreenDialog() {
      PopWindow().apply {
         userResizable.value = false
         userMovable.value = false
         headerVisible.value = false
         content.value = StackPane().also { root ->

            root.lay(TOP_LEFT) += Icon(IconMA.SCREEN_ROTATION, 20.em).apply { isDisable = true }
            root.lay += stackPane {
               padding = Insets(10.em)

               lay += vBox(2.em, CENTER) {
                  lay += Icon(IconMA.SCREEN_ROTATION, 8.em).apply {
                     onClickDo { rotateScreen(scene.window.screen, Dir.CCW) }
                     root.onEventUp(KEY_PRESSED, Key.UP) { requestFocus() }
                  }
                  lay += hBox(2.em, CENTER) {
                     lay += Icon(IconMD.BORDER_LEFT, 8.em).apply {
                        isDisable = Screen.getScreens().size==1
                        onClickDo { scene.window.centre = previous(Screen.getScreens(), scene.window.screen).bounds.centre }
                        root.onEventUp(KEY_PRESSED, Key.LEFT) { requestFocus() }
                     }
                     lay += vBox(0.em, BOTTOM_CENTER) {
                        isMouseTransparent = true
                        isDisable = true

                        lay += hBox(0.0, BOTTOM_CENTER) {
                           lay += Icon(IconFA.ARROW_CIRCLE_UP)
                        }
                        lay += hBox(0.0, BOTTOM_RIGHT) {
                           lay += Icon(IconFA.ARROW_CIRCLE_LEFT)
                           lay += Icon(null).apply { isVisible = false }
                           lay += Icon(IconFA.ARROW_CIRCLE_RIGHT)
                        }
                        lay += hBox(0.0, TOP_CENTER) {
                           lay += Icon(IconFA.ARROW_CIRCLE_DOWN)
                        }
                     }
                     lay += Icon(IconMD.BORDER_RIGHT, 8.em).apply {
                        isDisable = Screen.getScreens().size==1
                        onClickDo { scene.window.centre = previous(Screen.getScreens(), scene.window.screen).bounds.centre }
                        root.onEventUp(KEY_PRESSED, Key.RIGHT) { requestFocus() }
                     }
                  }
                  lay += Icon(IconMA.SCREEN_ROTATION, 8.em).apply {
                     scaleX = -1.0
                     onClickDo { rotateScreen(scene.window.screen, Dir.CW) }
                     root.onEventUp(KEY_PRESSED, Key.DOWN) { requestFocus() }
                  }
               }
            }
         }
         show(SCREEN_ACTIVE(CENTER))
      }
   }

   private fun indexOfScreen(screen: Screen): Int {
      return if (Screen.getScreens().size==1) {
         1
      } else {
         try {
            val bounds = screen.bounds
            ProcessBuilder(programFile.path, "/listdevices", "/properties").directory(userLocation).start()
               .inputStream.bufferedReader()
               .useLines {
                  1 + it.filter { it.contains("Display Area Size:") }
                     .indexOfFirst {
                        val corners = it.substringAfter(": ").substringBefore(" (").split(" - ")
                        val minX = corners[0].split(",")[0].toDouble()
                        val minY = corners[0].split(",")[1].toDouble()
                        val maxX = corners[1].split(",")[0].toDouble()
                        val maxY = corners[1].split(",")[1].toDouble()
                        bounds.minX==minX && bounds.minY==minY && bounds.maxX==maxX && bounds.maxY==maxY
                     }
               }
         } catch (e: Throwable) {
            logger.error(e) { "Failed to list monitors" }
            fail { "Failed to list monitors" }
         }
      }
   }

   enum class Dir(val command: String, val text: String) {
      CW("cw", "clockwise"),
      CCW("ccw", "counter-clockwise")
   }

   companion object: KLogging(), PluginInfo {
      override val name = "Screen Rotator"
      override val description = "Provides actions to turn on screen saver, sleep displays or rotate any screen using convenient visual aid"
      override val isSupported = Os.WINDOWS.isCurrent
      override val isSingleton = false
      override val isEnabledByDefault = false

      private const val PROGRAM_FILE_NAME = "display.exe"
      private const val PROGRAM_HELP_FILE_NAME = "display.htm"
   }

}