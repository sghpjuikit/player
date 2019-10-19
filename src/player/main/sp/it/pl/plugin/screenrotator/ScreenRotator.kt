package sp.it.pl.plugin.screenrotator

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
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.window.ShowArea
import sp.it.pl.gui.objects.window.popup.PopWindow
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.plugin.PluginBase
import sp.it.util.access.Values.previous
import sp.it.util.action.Action
import sp.it.util.async.runIO
import sp.it.util.conf.Constraint
import sp.it.util.conf.cr
import sp.it.util.conf.def
import sp.it.util.dev.fail
import sp.it.util.reactive.onEventDown
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
import java.io.IOException
import javafx.scene.input.KeyCode as Key

class ScreenRotator: PluginBase("Screen Rotator", true) {

   private val programFile = getResource(PROGRAM_FILE_NAME)
   private val programHelpFile = getResource(PROGRAM_HELP_FILE_NAME)
   private val actions by lazy {
      setOf(
         Action(
            "Rotate screen",
            { showRotateScreenDialog() },
            "Show 'Rotate Screen' dialog.",
            configurableGroupPrefix,
            "ALT+Semicolon",
            true, false
         ),
         Action(
            "Start screen saver",
            { startScreenSaver() },
            "Starts screen saver if enabled. It can be stopped by moving the mouse. On some system that can open logon screen.",
            configurableGroupPrefix,
            "CTRL+SHIFT+ALT+L",
            true, false
         ).forbidEdit(),
         Action(
            "Turn off screens",
            { turnScreens(false) },
            "Turns off all screens. They can be turned on again by moving the mouse.",
            configurableGroupPrefix,
            "CTRL+SHIFT+ALT+K",
            true,
            false
         ).forbidEdit()
      )
   }

   private val openHelpDo by cr { openHelp() }.def(name = "Open help", info = "Open technical usage help")

   override fun isSupported() = Os.WINDOWS.isCurrent

   override fun onStart() = APP.configuration.collect(actions)

   override fun onStop() = APP.configuration.drop(actions)

   fun openHelp() = programHelpFile.open()

   fun turnScreens(on: Boolean) {
      runIO {
         try {
            ProcessBuilder(programFile.path, if (on) "/power:on" else "/power:off").directory(userLocation).start()
         } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
         }
      }
   }

   fun startScreenSaver() {
      runIO {
         try {
            ProcessBuilder(programFile.path, "/power:saver").directory(userLocation).start()
         } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
         }
      }
   }

   fun rotateScreen(screen: Screen, direction: Dir) {
      runIO {
         val scr = indexOfScreen(screen)
         try {
            ProcessBuilder(programFile.path, "/rotate:${direction.command}", "/device:$scr").directory(userLocation).start()
         } catch (e: IOException) {
            logger.error(e) { "Failed to rotate display" }
         }
      }
   }

   private fun showRotateScreenDialog() {
      PopWindow().apply {
         userResizable.value = false
         userMovable.value = false
         headerVisible.value = false
         content.value = StackPane().also { root ->

            root.lay(TOP_LEFT) += Icon(IconMA.SCREEN_ROTATION, 20.em).apply { isDisable = true }
            root.lay += stackPane {
               padding = Insets(10.em)

               lay += vBox(2.em, CENTER) {
                  lay += Icon(IconMA.ROTATE_LEFT, 8.em).apply {
                     isFocusTraversable = true
                     onClickOrEnter { rotateScreen(scene.window.screen, Dir.CCW) }
                     root.onEventUp(KEY_PRESSED, Key.UP) { requestFocus() }
                  }
                  lay += hBox(2.em, CENTER) {
                     lay += Icon(IconMD.BORDER_LEFT, 8.em).apply {
                        isDisable = Screen.getScreens().size==1
                        isFocusTraversable = true
                        onClickOrEnter { scene.window.centre = previous(Screen.getScreens(), scene.window.screen).bounds.centre }
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
                        isFocusTraversable = true
                        onClickOrEnter { scene.window.centre = previous(Screen.getScreens(), scene.window.screen).bounds.centre }
                        root.onEventUp(KEY_PRESSED, Key.RIGHT) { requestFocus() }
                     }
                  }
                  lay += Icon(IconMA.ROTATE_RIGHT, 8.em).apply {
                     isFocusTraversable = true
                     onClickOrEnter { rotateScreen(scene.window.screen, Dir.CW) }
                     root.onEventUp(KEY_PRESSED, Key.DOWN) { requestFocus() }
                  }
               }
            }
         }
         show(ShowArea.SCREEN_ACTIVE(CENTER))
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

   companion object: KLogging() {
      private const val PROGRAM_FILE_NAME = "display.exe"
      private const val PROGRAM_HELP_FILE_NAME = "display.htm"

      private fun Action.forbidEdit() = apply { addConstraints(Constraint.ReadOnlyIf(true)) }

      private fun Icon.onClickOrEnter(block: () -> Unit) {
         onClickDo { block() }
         onEventDown(KEY_PRESSED, Key.ENTER) { block() }
      }
   }

}