package sp.it.pl.main

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.sun.tools.attach.VirtualMachine
import javafx.geometry.Pos.CENTER
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.layout.Pane
import javafx.scene.paint.Color.BLACK
import javafx.stage.Screen
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.readAudioFile
import sp.it.pl.layout.widget.WidgetLoader.WINDOW_FULLSCREEN
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.main.Actions.APP_SEARCH
import sp.it.pl.ui.objects.window.ShowArea.SCREEN_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.WebBarInterpreter
import sp.it.util.Util.urlEncodeUtf8
import sp.it.util.action.ActionManager
import sp.it.util.action.ActionRegistrar
import sp.it.util.action.IsAction
import sp.it.util.async.runFX
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.dev.Blocks
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.stacktraceAsString
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1If
import sp.it.util.system.browse
import sp.it.util.system.open
import sp.it.util.system.runCommand
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.bgr
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.stackPane
import sp.it.util.units.millis
import sp.it.util.units.times
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

class AppActions: GlobalSubConfigDelegator("Shortcuts") {

   @IsAction(name = "Open on Github", info = "Opens Github page for this application. For developers.")
   fun openAppGithubPage() {
      APP.githubUri.browse()
   }

   @IsAction(name = "Open app directory", info = "Opens directory from which this application is running from.")
   fun openAppLocation() {
      APP.location.open()
   }

   @IsAction(name = "Open css guide", info = "Opens css reference guide. For developers.")
   fun openCssGuide() {
      URI.create("http://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html").browse()
   }

   @IsAction(name = "Open settings", info = "Opens application settings.")
   fun openSettings() {
      openSettings(null)
   }

   fun openSettings(groupToSelect: String?) {
      APP.widgetManager.widgets.use<ConfiguringFeature>(NO_LAYOUT) { it.configure(APP.configuration, groupToSelect) }
   }

   @IsAction(name = "Open app actions", info = "Actions specific to whole application.")
   fun openActions() {
      APP.ui.actionPane.orBuild.show(APP)
   }

   @IsAction(name = "Open...", info = "Display all possible open actions.", keys = "CTRL+SHIFT+O", global = true)
   fun openOpen() {
      APP.ui.actionPane.orBuild.show(AppOpen)
   }

   @IsAction(name = "Show shortcuts", info = "Display all available shortcuts.", keys = "COMMA")
   fun showShortcuts() {
      val actionsStandard = ActionRegistrar.getActions().map { Entry(it) }
      val actionsHardcoded = listOfNotNull(
         Entry("Window", "Move window", keys("ALT+" + PRIMARY.nameUi) + " (hold)").takeIf { APP.windowManager.windowInteractiveOnLeftAlt.value },
         Entry("Window", "Resize window", keys("ALT+" + SECONDARY.nameUi) + " (hold)").takeIf { APP.windowManager.windowInteractiveOnLeftAlt.value },
         Entry("Ui", "Layout mode", keys(ActionManager.keyManageLayout.nameUi) + " (hold)"),
         Entry("Ui", "Table filter", keys("CTRL+F")),
         Entry("Ui", "Table search", "Type text")
      )
      APP.ui.shortcutPane.orBuild.show(actionsStandard + actionsHardcoded)
   }

   @IsAction(name = "Show system info", info = "Display system information.")
   fun showSysInfo() {
      APP.ui.infoPane.orBuild.show(Unit)
   }

   @IsAction(name = "Show overlay", info = "Display screen overlay.")
   fun showOverlay() {
      val overlays = ArrayList<OverlayPane<Unit>>()
      fun <T> List<T>.forEachDelayed(block: (T) -> Unit) = forEachIndexed { i, it -> runFX(200.millis*i) { block(it) } }
      var canHide = false
      val showAll = {
         overlays.forEachDelayed { it.show(Unit) }
      }
      val hideAll = {
         canHide = true
         overlays.forEachDelayed { it.hide() }
      }
      overlays += Screen.getScreens().asSequence().sortedBy { it.bounds.minX }.map {
         object: OverlayPane<Unit>() {

            init {
               content = stackPane()
               display.value = object: ScreenGetter {
                  override fun computeScreen() = it
               }
            }

            override fun show(data: Unit) {
               super.show()
            }

            override fun hide() {
               if (canHide) super.hide()
               else hideAll()
            }

         }
      }
      showAll()
   }

   @IsAction(name = APP_SEARCH, info = "Display application search.", keys = "CTRL+SHIFT+I", global = true)
   fun showSearchPosScreen() {
      PopWindow().apply {
         content.value = APP.search.buildUi { hide() }
         title.value = "Search for an action or option"
         isAutohide.value = true
         show(SCREEN_ACTIVE(CENTER))
      }
   }

   @IsAction(name = "Run system command", info = "Runs command just like in a system's shell's command line.", global = true)
   fun runCommand() {
      configureString("Run system command", "Command") {
         runCommand(it)
      }
   }

   @IsAction(name = "Run as app argument", info = "Equivalent of launching this application with the command as a parameter.")
   fun runAppCommand() {
      configureString("Run app command", "Command") {
         APP.parameterProcessor.process(listOf(it))
      }
   }

   @IsAction(name = "Open web search", info = "Opens website or search engine result for given phrase", keys = "CTRL + SHIFT + W", global = true)
   fun openWebBar() {
      configureString("Open on web...", "Website or phrase") {
         val uriString = WebBarInterpreter.toUrlString(it, DuckDuckGoQBuilder)
         try {
            val uri = URI(uriString)
            uri.browse()
         } catch (e: URISyntaxException) {
            logger.warn(e) { "$uriString is not a valid URI" }
         }
      }
   }

   @IsAction(name = "Open web dictionary", info = "Opens website dictionary for given word", keys = "CTRL + SHIFT + E", global = true)
   fun openDictionary() {
      configureString("Look up in dictionary...", "Word") {
         URI.create("http://www.thefreedictionary.com/${urlEncodeUtf8(it)}").browse()
      }
   }

   @JvmOverloads
   fun openImageFullscreen(image: File, screen: Screen = getScreenForMouse()) {
      APP.widgetManager.widgets.use<ImageDisplayFeature>(NEW(WINDOW_FULLSCREEN(screen))) { f ->
         val w = f.asIs<Controller>().widget
         val window = w.graphics.scene.window
         val root = window.scene.root

         window.scene.fill = BLACK
         root.asIf<Pane>()?.background = bgr(BLACK)

         root.onEventUp(KEY_PRESSED, ENTER) { window.hide() }
         root.onEventUp(KEY_PRESSED, ESCAPE) { window.hide() }

         window.showingProperty().sync1If({ it }) {
            f.showImage(image)
         }
      }
   }

   /**
    * The check whether file exists, is accessible or of correct type/format is left on the caller and behavior in
    * such cases is undefined.
    */
   @Blocks
   fun printAllImageFileMetadata(file: File) {
      failIfFxThread()

      val title = "Metadata of " + file.path
      val text = try {
         val sb = StringBuilder()
         ImageMetadataReader.readMetadata(file)
            .directories
            .forEach {
               sb.append("\nName: ").append(it.name)
               it.tags.forEach { tag -> sb.append("\n\t").append(tag.toString()) }
            }
         title + sb.toString()
      } catch (e: IOException) {
         "$title\n${e.stacktraceAsString}"
      } catch (e: ImageProcessingException) {
         "$title\n${e.stacktraceAsString}"
      }
      runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
   }

   @Blocks
   fun printAllAudioItemMetadata(song: Song) {
      failIfFxThread()

      if (song.isFileBased()) {
         printAllAudioFileMetadata(song.getFile()!!)
      } else {
         val text = "Metadata of ${song.uri}\n<only supported for files>"
         runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
      }
   }

   /**
    * The check whether file exists, is accessible or of correct type/format is left on the caller and behavior in
    * such cases is undefined.
    */
   @Blocks
   fun printAllAudioFileMetadata(file: File) {
      failIfFxThread()

      val title = "Metadata of ${file.path}"
      val content = file.readAudioFile()
         .map { af ->
            "\nHeader:\n" +
               af.audioHeader.toString().split("\n").joinToString("\n\t") +
               "\nTag:" +
               if (af.tag==null) " <none>" else af.tag.fields.asSequence().joinToString("") { "\n\t${it.id}:$it" }
         }
         .getOrSupply { "\n${it.stacktraceAsString}" }
      val text = title + content
      runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
   }

   @IsAction(name = "Print running java processes")
   fun printJavaProcesses() {
      val text = VirtualMachine.list().joinToString("") {
         "\nVM:\n\tid: ${it.id()}\n\tdisplayName: ${it.displayName()}\n\tprovider: ${it.provider()}"
      }
      runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
   }

   fun browseMultipleFiles(files: Sequence<File>) {
      val fs = files.asSequence().toSet()
      when {
         fs.isEmpty() -> Unit
         fs.size==1 -> fs.firstOrNull()?.browse()
         else -> APP.ui.actionPane.orBuild.show(MultipleFiles(fs))
      }
   }

   companion object: KLogging()
}