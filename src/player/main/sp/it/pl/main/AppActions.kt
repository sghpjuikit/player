package sp.it.pl.main

import com.twelvemonkeys.image.ResampleOp
import java.io.File
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
import javax.imageio.ImageIO
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.layout.ComponentLoader.WINDOW_FULLSCREEN
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.controller.Controller
import sp.it.pl.layout.feature.ImageDisplayFeature
import sp.it.pl.layout.feature.ObjectDetail
import sp.it.pl.main.Actions.APP_SEARCH
import sp.it.pl.plugin.impl.Notifier
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.SpitText
import sp.it.pl.ui.objects.form.Validated
import sp.it.pl.ui.objects.window.ShowArea.SCREEN_ACTIVE
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.asPopWindow
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.popWindow
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.ALL
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized.NONE
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.OverlayPane
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.pl.ui.pane.action
import sp.it.pl.ui.pane.actionAll
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.WebBarInterpreter
import sp.it.util.Sort
import sp.it.util.Util.urlEncodeUtf8
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.action.ActionManager
import sp.it.util.action.ActionRegistrar
import sp.it.util.action.IsAction
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.async.runIoParallel
import sp.it.util.async.runVT
import sp.it.util.conf.CheckList
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor
import sp.it.util.conf.EditMode
import sp.it.util.conf.GlobalSubConfigDelegator
import sp.it.util.conf.ValueConfig
import sp.it.util.conf.but
import sp.it.util.conf.c
import sp.it.util.conf.cCheckList
import sp.it.util.conf.cn
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.conf.readOnly
import sp.it.util.conf.values
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.fail
import sp.it.util.file.creationTime
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.setCreated
import sp.it.util.file.type.MimeExt
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.functional.Try
import sp.it.util.functional.Try.Error
import sp.it.util.functional.Try.Ok
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.SHORTCUT
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1If
import sp.it.util.system.Os
import sp.it.util.system.browse
import sp.it.util.system.runCommand
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.type.type
import sp.it.util.ui.bgr
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.hyperlink
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.millis
import sp.it.util.units.times
import sp.it.util.units.uri

@Suppress("RemoveExplicitTypeArguments")
class AppActions: GlobalSubConfigDelegator("Shortcuts") {
   /** [ActionManager] */
   val manager = ActionManager
   /** [ActionRegistrar] */
   val registrar = ActionRegistrar
   /** [AppActionsAny] */
   val any = AppActionsAny
   /** [AppActionsApp] */
   val app = AppActionsApp
   /** [AppActionsAppHelp] */
   val appHelp = AppActionsAppHelp
   /** [AppActionsAppOpen] */
   val appOpen = AppActionsAppOpen
   /** [AppActionsAppTest] */
   val appTest = AppActionsAppTest
   /** [AppActionsComponent] */
   val component = AppActionsComponent
   /** [AppActionsString] */
   val string = AppActionsString
   /** [AppActionsFile] */
   val file = AppActionsFile
   /** [AppActionsUnit] */
   val unit = AppActionsUnit
   /** [AppActionsWindow] */
   val window = AppActionsWindow

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
         Entry("Playback", "Volume up", keys("Scroll Up (supported content only)")),
         Entry("Playback", "Volume down", keys("Scroll Down (supported content only)")),
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
         Entry("Ui", "Increment font size (if overridden)", keys("SHIFT+Scroll up")),
         Entry("Ui", "Decrement font size (if overridden)", keys("SHIFT+Scroll down")),
         Entry("Ui", "Show application help", F1.nameUi),
         Entry("Ui", "Show application help", ActionManager.keyShortcuts.nameUi),
         Entry("Ui", "Show focused widget help", F2.nameUi),
         Entry("Ui", "Show focused widget help", ActionManager.keyShortcutsComponent.nameUi),
         Entry("Ui", "Show focused widget actions", ActionManager.keyActionsComponent.nameUi),
         Entry("Ui > Layout", "Layout mode", ActionManager.keyManageLayout.nameUi),
         Entry("Ui > Layout", "Widget context menu", keys("${PRIMARY.nameUi} Press + ${SECONDARY.nameUi}")),
         Entry("Ui > Layout", "Widget context menu", keys(SHIFT, F10)).takeIf { Os.WINDOWS.isCurrent },
         Entry("Ui > Controls > Button | Icon", "Run action", PRIMARY.nameUi),
         Entry("Ui > Controls > Button | Icon", "Run action", ENTER.nameUi),
         Entry("Ui > Controls > Button | Icon", "Run action", SPACE.nameUi),
         Entry("Ui > Controls > Button | Icon", "Run action (default)", ENTER.nameUi + " (anywhere)"),
         Entry("Ui > Controls > TextField", "Autocomplete show", keys(CONTROL, SPACE)),
         Entry("Ui > Controls > TextField", "Autocomplete accept", keys(ENTER)),
         Entry("Ui > Controls > Combobox", "Suggestions", keys(SPACE)),
         Entry("Ui > Controls > Combobox", "Suggestions (editable)", keys(CONTROL, SPACE)),
         Entry("Ui > Controls > List | Table | TreeTable", "Select", PRIMARY.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable", "Selection (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable", "Show item context menu", SECONDARY.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable", "Scroll vertically", keys("Scroll")),
         Entry("Ui > Controls > List | Table | TreeTable", "Scroll horizontally", keys("Scroll+SHIFT")),
         Entry("Ui > Controls > List | Table | TreeTable", "Filter", keys(CONTROL, F)),
         Entry("Ui > Controls > List | Table | TreeTable", "Filter (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable", "Filter (clear)", ESCAPE.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable", "Search", "Type text"),
         Entry("Ui > Controls > List | Table | TreeTable", "Search (cancel)", ESCAPE.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable > Header", "Show column menu", SECONDARY.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable > Header", "Swap columns", "Column drag"),
         Entry("Ui > Controls > List | Table | TreeTable > Header", "Sort - ${Sort.ASCENDING.toUi()} | ${Sort.DESCENDING.toUi()} | ${Sort.NONE.toUi()}", PRIMARY.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable > Header", "Sorts by multiple columns", keys("SHIFT+LMB")),
         Entry("Ui > Controls > List | Table | TreeTable > Footer", "Opens additional action menus", "Menu bar"),
         Entry("Ui > Controls > List | Table | TreeTable > Row", "Selects item", PRIMARY.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable > Row", "Show context menu", SECONDARY.nameUi),
         Entry("Ui > Controls > List | Table | TreeTable > Row", "Move song within playlist", keys("CTRL+Drag")),
         Entry("Ui > Controls > List | Table | TreeTable > Row", "Add items after row", "Drag & drop items"),
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
         Entry("Ui", "Show widget actions", F3.nameUi),
         Entry("Ui", "Show widget actions", ActionManager.keyActionsComponent.nameUi),
      )
      APP.ui.shortcutPane.orBuild.show(ShortcutPane.Info(t, actionsHardcoded + widget.factory.summaryActions))
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
         popWindow {
            content.value = APP.search.buildUi { hide() }
            title.value = "Search for an action or option"
            isAutohide.value = true
            ignoreAsOwner = true
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
         uri("https://www.thefreedictionary.com/${urlEncodeUtf8(it)}").browse()
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

   val printAllAudioMetadata = action<Song>("Show metadata", "Show metadata", IconFA.INFO) {
      APP.widgetManager.widgets.use<ObjectDetail>(NEW) { w -> w.showDetail(it) }
   }

   val printMetadata = action<File>("Show metadata", "Show metadata", IconFA.INFO) {
      APP.widgetManager.widgets.use<ObjectDetail>(NEW) { w -> w.showDetail(it) }
   }

   val openMarkdownFile = action<File>("Open markdown", "Opens markdown file.", IconOC.MARKDOWN, constriction = { it hasExtension MimeExt.md }) { mdFile ->
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

   val fileSyncFileTimes = action<File>(
      "Restore file time",
      "Restores creation/modification times. Useful after file or directory copy.\n" +
         "Sets times for destination files to those of source files. Runs for each directory and file, recursively. " +
         "Matches by path relative to source/destination directory.",
      IconFA.FILES_ALT,
   ) { file ->
      object: ConfigurableBase<File>(), Validated {

         val srcFileC by c<File>(file).readOnly()
            .def(name = "Source", info = "File or directory that will be used as source", editable = EditMode.NONE)
         val dstFileC by cvn<File>(null)
            .def(name = "Destination", info = "File or directory that will have it's time restored")
         var strategyC by c(FileFlatter.ALL)
            .def(name = "Strategy", info = "Strategy to obtain files from the source/destination")
         val restoreC by cCheckList(CheckList.nonNull(type<String>(), listOf("Created Time", "Modified Time"), listOf(true, true)))
            .def(name = "Restore")

         override fun isValid(): Try<*,String> = when {
            dstFileC.value==null -> Error("Destination must not be null")
            restoreC.selections.none { it } -> Error("At least one item must be selected")
            else -> Ok(null)
         }
      }.configure("Synchronize file times") {
         val srcFile = it.srcFileC
         val dstFile = it.dstFileC.value!!
         val strategy = it.strategyC
         val restoreModified = it.restoreC.isSelected("Modified Time")
         val restoreCreated = it.restoreC.isSelected("Created Time")
         val srcPath = srcFile.absolutePath.trimEnd('\\')
         val dstPath = dstFile.absolutePath.trimEnd('\\')
         runVT {
            val src = strategy.flatten(listOf(srcFile)).associateBy { it.absolutePath.substringAfter(srcPath) }
            val dst = strategy.flatten(listOf(dstFile)).associateBy { it.absolutePath.substringAfter(dstPath) }
            runIoParallel(items = dst.entries) { (path, df) ->
               src[path].ifNotNull { sf ->
                  if (restoreModified) df.setLastModified(sf.lastModified())
                  if (restoreCreated) df.setCreated(sf.creationTime().orNull()!!)
               }
            }.block()
         }
      }
   }.preventClosing()

   val fileFlatten = actionAll<File>("Flatten tree", "Flattens file hierarchy", IconFA.FILES_ALT, BLOCK) { files ->
      ValueConfig(type(), "Strategy", FileFlatter.ALL, "").configure("Flatten tree") {
         runIO {
            it.value.flatten(files).toList()
         } ui {
            apOrApp.show(it)
         }
      }
   }.preventClosing()

//   private var lastAddFilesLocation by cn<File>(APP.location.user).noUi()
//      .def(name = "Last add songs browse location", editable = EditMode.APP)
//   private var lastAddDirLocation by cn<File>(APP.location.user).only(FileType.DIRECTORY).noUi()
//      .def(name = "Last add directory browse location", editable = EditMode.APP)
//   fun addDirectory() = chooseFile("Add folder to library", FileType.DIRECTORY, lastAddDirLocation, root.scene.window).ifOk {
//      APP.ui.actionPane.orBuild.show(it)
//      lastAddDirLocation = it.parentFile
//   }
//
//   fun addFiles() = chooseFiles("Add files to library", lastAddFilesLocation, root.scene.window, audioExtensionFilter()).ifOk {
//      APP.ui.actionPane.orBuild.show(it)
//      lastAddFilesLocation = Util.getCommonRoot(it)
//   }

   val convertImage = actionAll<File>("Convert image", "Converts the image into a different type.", IconFA.EXCHANGE, constriction = { it.isImage12Monkey() }) { ii ->
      object: ConfigurableBase<Any?>() {
         val fileFrom by cn(ii.firstOrNull()).but { if (ii.size!=1) noUi() }.def(name = "Source image file", editable = EditMode.NONE)
         val typeFrom by cn(ii.firstOrNull()?.mimeType()).but { if (ii.size!=1) noUi() }.def(name = "Source image type", editable = EditMode.NONE)
         var dirTo by c(ii.first().parentDirOrRoot).only(FileActor.DIRECTORY).def(name = "Destination image folder")
         var typeTo by c(MimeType.`image∕png`).values(listOf(MimeType.`image∕png`, MimeType.`image∕jpeg`)).def(name = "Destination image type")
         var sizeTo by cn<ImageSize>(null).def(name = "Destination image size", info = "Destination image size or null if no scaling should be applied")
         var preserveTimeCreated by c(true).def(name = "Preserve '${FileField.TIME_CREATED}'")
         var preserveTimeModified by c(true).def(name = "Preserve '${FileField.TIME_MODIFIED}'")
         var parallel by c(true).def(name = "Run in parallel", info = "Recommended for SSD, but may slow down HDD.")
      }.configure("Convert image") {
         val suffix = it.typeTo.extension!!
         runIoParallel(if (it.parallel) Runtime.getRuntime().availableProcessors() else 1, items = ii) { i ->
            val fileTo = it.dirTo / "${i.nameWithoutExtension}.$suffix"
            val imgFrom = ImageIO.read(i)
            val imgFromScaled = it.sizeTo?.net { ResampleOp(it.width.toInt(), it.height.toInt()).filter(imgFrom, null).apply { imgFrom.flush() } } ?: imgFrom
            val r = ImageIO.write(imgFromScaled, suffix, fileTo)
            if (r==false) fail { "No appropriate writer found for $suffix" }
            if (it.preserveTimeCreated) i.creationTime().orNull().ifNotNull { t -> fileTo.setCreated(t) }
            if (it.preserveTimeModified) fileTo.setLastModified(i.lastModified())
         }
      }
   }.preventClosing()

   fun browseMultipleFiles(files: Sequence<File>) {
      val fs = files.toSet()
      when {
         fs.isEmpty() -> Unit
         fs.size==1 -> fs.firstOrNull()?.browse()
         else -> APP.ui.actionPane.orBuild.show(MultipleFiles(fs))
      }
   }

   companion object: KLogging()
}