package sp.it.pl.main

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.sun.tools.attach.VirtualMachine
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.paint.Color.BLACK
import javafx.stage.Screen
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.readAudioFile
import sp.it.pl.layout.ComponentLoader.WINDOW_FULLSCREEN
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.controller.Controller
import sp.it.pl.layout.feature.ConfiguringFeature
import sp.it.pl.layout.feature.ImageDisplayFeature
import sp.it.pl.layout.feature.TextDisplayFeature
import sp.it.pl.main.Actions.APP_SEARCH
import sp.it.pl.plugin.impl.Notifier
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.SpitText
import sp.it.pl.ui.objects.window.ShowArea.SCREEN_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.asPopWindow
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.ALL
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.NONE
import sp.it.pl.ui.pane.FastAction
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.WebBarInterpreter
import sp.it.util.Sort
import sp.it.util.Util.urlEncodeUtf8
import sp.it.util.action.ActionManager
import sp.it.util.action.ActionRegistrar
import sp.it.util.action.IsAction
import sp.it.util.async.runFX
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.dev.Blocks
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.hasExtension
import sp.it.util.file.type.MimeExt
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.reactive.SHORTCUT
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1If
import sp.it.util.system.browse
import sp.it.util.system.open
import sp.it.util.system.runCommand
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.bgr
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.hyperlink
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import sp.it.util.units.times
import sp.it.util.units.uri

class AppActions: GlobalSubConfigDelegator("Shortcuts") {

   @IsAction(name = "Open on Github", info = "Opens Github page for this application. For developers.")
   fun openAppGithubPage() {
      APP.githubUri.browse()
   }

   @IsAction(name = "Open app directory", info = "Opens directory from which this application is running from.")
   fun openAppLocation() {
      APP.location.open()
   }

   @IsAction(name = "Open app event log", info = "Opens application event log.")
   fun openAppEventLog() {
      AppEventLog.showDetailForLast()
   }

   @IsAction(name = "Open css guide", info = "Opens css reference guide. For developers.")
   fun openCssGuide() {
      uri("http://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html").browse()
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

   // hardcoded shortcut, see ActionManager.keyShortcuts
   fun showShortcuts() {
      fun Entry.ifInteractiveOn() = takeIf { APP.windowManager.windowInteractiveOnLeftAlt.value }
      val actionsStandard = ActionRegistrar.getActions().map { Entry(it) }
      val actionsHardcoded = listOfNotNull(
         Entry("Ui > Window", "Move window", keys("ALT+drag " + PRIMARY.nameUi)).ifInteractiveOn(),
         Entry("Ui > Window", "Move window -> toggle maximize", keys("ALT+drag ${PRIMARY.nameUi}+${SECONDARY.nameUi}")).ifInteractiveOn(),
         Entry("Ui > Window", "Resize window", keys("ALT+drag " + SECONDARY.nameUi)).ifInteractiveOn(),
         Entry("Ui > Window", "Resize window", keys("ALT+drag edge " + SECONDARY.nameUi)).ifInteractiveOn(),
         Entry("Ui > Window", "Maximize (toggle ${ALL.toUi()}/${NONE.toUi()})", keys("header 2x${PRIMARY.nameUi}")),
         Entry("Ui > Window", "Header visible (toggle)", keys("header 2x${SECONDARY.nameUi}")),
         Entry("Ui > Window", "Maximize/screen (toggle)", keys("WIN+LEFT")),
         Entry("Ui > Window", "Maximize/screen (toggle)", keys("WIN+RIGHT")),
         Entry("Ui > Window", "Fullscreen", keys(F11)),
         Entry("Ui > Window", "Fullscreen", keys(F12)),
         Entry("Ui > Window", "Minimize", keys(WINDOWS, G)),
         Entry("Ui > Window", "Maximize (toggle)", keys("WIN+F")),
         Entry("Ui > Window", "Maximize (toggle)", keys("WIN+SHIFT+F")),
         Entry("Ui > Window", "Maximize/minimize (toggle)", keys("WIN+UP")),
         Entry("Ui > Window", "Maximize/minimize (toggle)", keys("WIN+DOWN")),
         Entry("Ui > Window", "On top (toggle)", keys("WIN+A")),
         Entry("Ui > Window", "Autohide (toggle)", keys("WIN+Z")),
         Entry("Ui > Window", "Close", keys("WIN+Q")),
         Entry("Ui > Window", "Close", keys("ALT+F4")),
         Entry("Ui > Popup", "Close", ESCAPE.nameUi),
         Entry("Ui > Popup", "Close", keys("ALT+F4")),
         Entry("Ui > Focus", "Traverse next", TAB.nameUi),
         Entry("Ui > Focus", "Traverse previous", keys("SHIFT+TAB")),
         Entry("Ui > Focus", "Traverse next widget", keys("CTRL+TAB")),
         Entry("Ui > Focus", "Traverse previous widget", keys("CTRL+SHIFT+TAB")),
         Entry("Ui", "Show application help", F1.nameUi),
         Entry("Ui", "Show application help", ActionManager.keyShortcuts.nameUi),
         Entry("Ui", "Show focused widget help", F2.nameUi),
         Entry("Ui", "Show focused widget help", ActionManager.keyShortcutsComponent.nameUi),
         Entry("Ui", "Show focused widget actions", ActionManager.keyActionsComponent.nameUi),
         Entry("Ui", "Layout mode", keys(ActionManager.keyManageLayout.nameUi) + " (hold)"),
         Entry("Ui > Controls > Button/Icon", "Run action", PRIMARY.nameUi),
         Entry("Ui > Controls > Button/Icon", "Run action", ENTER.nameUi),
         Entry("Ui > Controls > Button/Icon", "Run action", SPACE.nameUi),
         Entry("Ui > Controls > Button/Icon", "Run action (default)", ENTER.nameUi + " (anywhere)"),
         Entry("Ui > Controls > TextField", "Autocomplete show", keys(CONTROL, SPACE)),
         Entry("Ui > Controls > TextField", "Autocomplete accept", keys(ENTER)),
         Entry("Ui > Controls > Combobox", "Suggestions", keys(SPACE)),
         Entry("Ui > Controls > Combobox", "Suggestions (editable)", keys(CONTROL, SPACE)),
         Entry("Ui > Controls > List/Table/TreeTable", "Select", PRIMARY.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable", "Selection (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable", "Show item context menu", SECONDARY.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable", "Scroll vertically", keys("Scroll")),
         Entry("Ui > Controls > List/Table/TreeTable", "Scroll horizontally", keys("Scroll+SHIFT")),
         Entry("Ui > Controls > List/Table/TreeTable", "Filter", keys(CONTROL, F)),
         Entry("Ui > Controls > List/Table/TreeTable", "Filter (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable", "Filter (clear)", ESCAPE.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable", "Search", "Type text"),
         Entry("Ui > Controls > List/Table/TreeTable", "Search (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable > Header", "Show column menu", SECONDARY.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable > Header", "Swap columns", "Column drag"),
         Entry("Ui > Controls > List/Table/TreeTable > Header", "Sort - ${Sort.ASCENDING.toUi()} | ${Sort.DESCENDING.toUi()} | ${Sort.NONE.toUi()}", PRIMARY.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable > Header", "Sorts by multiple columns", keys("SHIFT+LMB")),
         Entry("Ui > Controls > List/Table/TreeTable > Footer", "Opens additional action menus", "Menu bar"),
         Entry("Ui > Controls > List/Table/TreeTable > Row", "Selects item", PRIMARY.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable > Row", "Show context menu", SECONDARY.nameUi),
         Entry("Ui > Controls > List/Table/TreeTable > Row", "Move song within playlist", keys("CTRL+Drag")),
         Entry("Ui > Controls > List/Table/TreeTable > Row", "Add items after row", "Drag & drop items"),
         Entry("Ui > Controls > Grid", "Select", PRIMARY.nameUi),
         Entry("Ui > Controls > Grid", "Selection (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > Grid", "Show item context menu", SECONDARY.nameUi),
         Entry("Ui > Controls > Grid", "Scroll vertically", keys("Scroll")),
         Entry("Ui > Controls > Grid", "Scroll horizontally", keys("Scroll+SHIFT")),
         Entry("Ui > Controls > Grid", "Filter", keys("CTRL+F")),
         Entry("Ui > Controls > Grid", "Filter (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > Grid", "Filter (clear)", ESCAPE.nameUi),
         Entry("Ui > Controls > Grid", "Search", "Type text"),
         Entry("Ui > Controls > Grid", "Search (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > Grid > Footer", "Opens additional action menus", "Menu bar"),
         Entry("Ui > Controls > Grid > Cell", "Selects item", PRIMARY.nameUi),
         Entry("Ui > Controls > Grid > Cell", "Show context menu", SECONDARY.nameUi),
         Entry("Ui > Controls > Grid > Cell", "Move song within playlist", keys("CTRL+Drag")),
         Entry("Ui > Controls > Grid > Cell", "Add items after row", "Drag & drop items"),
      )
      APP.ui.shortcutPane.orBuild.show(ShortcutPane.Info("", actionsStandard + actionsHardcoded))
   }

   fun showShortcutsFor(widget: Widget) {
      val t = widget.factory.summaryUi

      val actionsHardcoded = listOfNotNull(
         Entry("Layout", "Go to child", keys("${PRIMARY.nameUi} (layout mode)")),
         Entry("Layout", "Go to parent", keys("${SECONDARY.nameUi} (layout mode)")),
         Entry("Layout", "Drags widget to other area", keys("${PRIMARY.nameUi} + Drag")),
         Entry("Layout", "Detach widget", keys("${SHORTCUT.nameUi} + ${PRIMARY.nameUi} + Drag")),
         Entry("Ui", "Show widget help", F2.nameUi),
         Entry("Ui", "Show widget help", ActionManager.keyShortcutsComponent.nameUi),
         Entry("Ui", "Show widget actions", ActionManager.keyActionsComponent.nameUi),
      )
      APP.ui.shortcutPane.orBuild.show(ShortcutPane.Info(t, actionsHardcoded + widget.factory.summaryActions))
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
                  override fun isWindowBased() = true
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
      APP.windowManager.windowsFx.find { it.scene?.root?.properties?.get(APP_SEARCH) == APP_SEARCH }.ifNotNull { it.asPopWindow()?.focus() }.ifNull {
         PopWindow().apply {
            content.value = APP.search.buildUi { hide() }
            title.value = "Search for an action or option"
            isAutohide.value = true
            properties[APP_SEARCH] = APP_SEARCH
            show(SCREEN_ACTIVE(CENTER))
         }
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
            uri(uriString).browse()
         } catch (e: URISyntaxException) {
            logger.warn(e) { "$uriString is not a valid URI" }
         }
      }
   }

   @IsAction(name = "Open web dictionary", info = "Opens website dictionary for given word", keys = "CTRL + SHIFT + E", global = true)
   fun openDictionary() {
      configureString("Look up in dictionary...", "Word") {
         uri("http://www.thefreedictionary.com/${urlEncodeUtf8(it)}").browse()
      }
   }

   @JvmOverloads
   fun openImageFullscreen(image: File, screen: Screen = getScreenForMouse()) {
      APP.widgetManager.widgets.use<ImageDisplayFeature>(NEW(WINDOW_FULLSCREEN(screen))) { f ->
         val w = f.asIs<Controller>().widget
         val window = w.graphics!!.scene.window
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

   private var isShowingRestart = false

   /** Show permanent notification suggesting application restart. */
   @ThreadSafe
   fun showSuggestRestartNotification() {
      runFX {
         if (!isShowingRestart) {
            val root = vBox(10.0, CENTER_LEFT) {
               lay += SpitText("Application requires restart to apply changes").apply {
                  wrappingWithNatural.subscribe()
               }
               lay += hyperlink("Restart") {
                  onEventDown(MOUSE_CLICKED, PRIMARY) { APP.restart() }
               }
            }
            APP.plugins.use<Notifier> {
               val n = it.showNotification("Restart", root, true)
               n.onShown += { isShowingRestart = true }
               n.onHiding += { isShowingRestart = false }
            }
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

   val openMarkdownFile = FastAction<File>(
      "Open markdown",
      "Opens markdown file.",
      IconOC.MARKDOWN,
      { it hasExtension MimeExt.md },
   ) { mdFile ->
      APP.windowManager.createWindow().apply {
         detachLayout()
         setContent(
            MdNode().apply {
               readFile(mdFile)
            }
         )
         show()
      }
   }

   val openMarkdownText = FastAction<String>(
      "Open markdown",
      "Opens markdown text.",
      IconOC.MARKDOWN,
   ) { mdText ->
      APP.windowManager.createWindow().apply {
         detachLayout()
         setContent(
            MdNode().apply {
               readText(mdText)
            }
         )
         show()
      }
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