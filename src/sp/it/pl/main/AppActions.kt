package sp.it.pl.main

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon
import de.jensd.fx.glyphs.materialicons.MaterialIcon
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.stage.FileChooser.ExtensionFilter
import sp.it.pl.audio.Item
import sp.it.pl.audio.Player
import sp.it.pl.audio.SimpleItem
import sp.it.pl.audio.tagging.MetadataReader
import sp.it.pl.gui.Gui
import sp.it.pl.gui.infonode.ConvertTaskInfo
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.ActionPane.ComplexActionData
import sp.it.pl.gui.pane.ActionPane.FastAction
import sp.it.pl.gui.pane.ActionPane.FastColAction
import sp.it.pl.gui.pane.ActionPane.SlowColAction
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.layout.Component
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetSource.ANY
import sp.it.pl.layout.widget.WidgetSource.NEW
import sp.it.pl.layout.widget.WidgetSource.NO_LAYOUT
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.ImagesDisplayFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.util.access.V
import sp.it.pl.util.action.Action
import sp.it.pl.util.async.FX
import sp.it.pl.util.async.future.Fut.fut
import sp.it.pl.util.conf.Config
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.AudioFileFormat.Use
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.FileType.DIRECTORY
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.Util.getCommonRoot
import sp.it.pl.util.file.Util.getFilesAudio
import sp.it.pl.util.file.endsWithSuffix
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.graphics.Util.layHorizontally
import sp.it.pl.util.graphics.Util.layVertically
import sp.it.pl.util.system.browse
import sp.it.pl.util.system.chooseFile
import sp.it.pl.util.system.chooseFiles
import sp.it.pl.util.system.edit
import sp.it.pl.util.system.open
import sp.it.pl.util.system.recycle
import sp.it.pl.util.system.saveFile
import java.io.File
import java.util.function.Consumer
import kotlin.streams.toList

private typealias IconMA = MaterialIcon
private typealias IconMD = MaterialDesignIcon
private typealias IconFA = FontAwesomeIcon

fun ActionPane.initAppActionPane(): ActionPane = also { ap ->
    ap.register<App>(
            FastAction(
                    "Export widgets",
                    "Creates launcher file in the destination directory for every widget.\n"+
                            "Launcher file is a file that when opened by this application opens the widget. "+
                            "If application was not running before, it will not load normally, but will only "+
                            "open the widget.\n"+"Essentially, this exports the widgets as 'standalone' applications.",
                    IconMD.EXPORT,
                    { app ->
                        chooseFile("Export to...", FileType.DIRECTORY, app.DIR_LAYOUTS, ap.scene.window)
                                .ifOk { dir -> app.widgetManager.factories.getFactories().forEach { it.create().exportFxwlDefault(dir) } }
                    }
            ),
            FastAction(IconMD.KEYBOARD_VARIANT, Action.get("Show shortcuts")),
            FastAction(IconMD.INFORMATION_OUTLINE, Action.get("Show system info")),
            FastAction(IconFA.GITHUB, Action.get("Open on Github")),
            FastAction(IconFA.CSS3, Action.get("Open css guide")),
            FastAction(IconFA.IMAGE, Action.get("Open icon viewer")),
            FastAction(IconMD.FOLDER, Action.get("Open app directory"))
    )
}

fun ActionPane.initActionPane(): ActionPane = also { ap ->
    ap.register<Void>(
            FastAction(
                    "Select file",
                    "Open file chooser to select files",
                    IconMD.FILE,
                    ap.converting { chooseFiles("Select file...", null, ap.scene.window) }
            ),
            FastAction(
                    "Select directory",
                    "Open file chooser to select directory",
                    IconMD.FOLDER,
                    ap.converting { chooseFile("Select directory...", DIRECTORY, null, ap.scene.window) }
            )
    )
    ap.register<Any>(
            FastColAction(
                    "Set as data",
                    "Sets the selected data as input.",
                    IconMD.DATABASE,
                    ap.converting { Try.ok<Any, Void>(it) }
            ),
            FastColAction(
                    "Open in Converter",
                    "Open data in Converter.",
                    IconMD.SWAP_HORIZONTAL,
                    // TODO: make sure it opens Converter or support multiple Opener types
                    { f -> APP.widgetManager.widgets.use<Opener>(ANY) { it.open(f) } }
            )
    )
    ap.register<Component>(
            FastAction(
                    "Export",
                    "Creates a launcher for this component. \n"+
                    "Opening the launcher with this application will open this component with current settings "+
                    "as if it were a standalone application.",
                    IconMD.EXPORT,
                    { w ->
                        saveFile("Export to...", APP.DIR_LAYOUTS, w.exportName, APP.actionPane.scene.window, ExtensionFilter("Component", "*.fxwl"))
                                .ifOk { w.exportFxwl(it) }
                    }
            )
    )
    ap.register<Widget<*>>(
            FastAction(
                    "Use as default",
                    "Uses settings of this widget as default settings when creating widgets of this type. This " +
                    "overrides the default settings of the widget set by the developer. For using multiple widget " +
                    "configurations at once, use 'Export' instead.",
                    IconMD.SETTINGS_BOX,
                    { w -> w.storeDefaultConfigs() }
            ),
            FastAction(
                    "Clear default",
                    "Removes overridden default settings for this widget. New widgets will start with settings set " +
                    "by the developer.",
                    IconMD.SETTINGS_BOX,
                    { w -> w.clearDefaultConfigs() }
            )
    )
    ap.register<Item>(
            FastColAction(
                    "Add to new playlist",
                    "Add items to new playlist widget.",
                    IconMD.PLAYLIST_PLUS,
                    { items -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addItems(items) } }
            ),
            FastColAction(
                    "Add to existing playlist",
                    "Add items to existing playlist widget if possible or to a new one if not.",
                    IconMD.PLAYLIST_PLUS,
                    { items -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(items) } }
            ),
            FastColAction(
                    "Update from file",
                    "Updates library data for the specified items from their file metadata. The difference between the data "+
                            "in the database and real metadata cab be a result of a bug or file edited externally. "+
                            "After this, the library will be synchronized with the file data.",
                    IconFA.REFRESH,
                    { Player.refreshItems(it) }
            ),
            FastColAction(
                    "Remove from library",
                    "Removes all specified items from library. After this library will contain none of these items.",
                    IconMD.DATABASE_MINUS,
                    { APP.db.removeItems(it) }
            ),
            FastColAction(
                    "Show",
                    "Shows items in a table.",
                    IconMA.COLLECTIONS,
                    { items -> APP.widgetManager.widgets
                                .find("Library", NEW, false)
                                .ifPresent { it.controller.inputs.getInput<Collection<Item>>("To display").setValue(items) }
                    }
            ),
            FastColAction(
                    "Show as Group",
                    "Group items in a table.",
                    MaterialIcon.COLLECTIONS,
                    { items -> APP.widgetManager.widgets
                                .find("Library View", NEW, false)
                                .ifPresent { it.controller.inputs.getInput<Collection<Item>>("To display").setValue(items) }
                    }
            )
    )
    ap.register<File>(
            FastAction(
                    "Recycle", "Moves file to recycle bin.",
                    IconMA.DELETE,
                    { it.recycle() }
            ),
            FastAction(
                    "Read metadata", "Prints all image metadata to console.",
                    IconMA.IMAGE_ASPECT_RATIO,
                    { ImageFileFormat.isSupported(it) },
                    { APP.actions.printAllImageFileMetadata(it) }
            ),
            FastAction(
                    "Open (OS)", "Opens file in a native program associated with this file type.",
                    IconMA.OPEN_IN_NEW,
                    { it.open() }
            ),
            FastAction(
                    "Edit (OS)", "Edit file in a native editor program associated with this file type.",
                    IconFA.EDIT,
                    { it.edit() }
            ),
            FastAction(
                    "Browse (OS)", "Browse file in a native file system browser.",
                    IconFA.FOLDER_OPEN_ALT,
                    { it.browse() }
            ),
            FastColAction(
                    "Add to new playlist",
                    "Add items to new playlist widget.",
                    IconMD.PLAYLIST_PLUS,
                    { f -> AudioFileFormat.isSupported(f, Use.APP) },
                    { fs -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addFiles(fs) } }
            ),
            SlowColAction(
                    "Find files",
                    "Looks for files recursively in the the data.",
                    IconMD.FILE_FIND,
                    // TODO: make fully configurable, recursion depth lvl, filtering, ...
                    ap.converting { fs -> Try.ok<List<File>, Void>(Util.getFilesR(fs, Integer.MAX_VALUE).toList()) }
            ),
            SlowColAction<File>(
                    "Add to library",
                    "Add items to library if not yet contained and edit added items in tag editor. If "+
                            "item already was in the database it will not be added or edited.",
                    IconMD.DATABASE_PLUS,
                    { }
            ).preventClosing { addToLibraryConsumer(it) },
            FastColAction(
                    "Add to existing playlist",
                    "Add items to existing playlist widget if possible or to a new one if not.",
                    IconMD.PLAYLIST_PLUS,
                    { AudioFileFormat.isSupported(it, Use.APP) },
                    { f -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addFiles(f) } }
            ),
            FastAction(
                    "Apply skin",
                    "Apply skin on the application.",
                    IconMD.BRUSH,
                    { Util.isValidSkinFile(it) },
                    { it -> Gui.setSkin(it) }
            ),
            FastAction(
                    "View image",
                    "Opens image in an image viewer widget.",
                    IconFA.IMAGE,
                    { ImageFileFormat.isSupported(it) },
                    { img_file -> APP.widgetManager.widgets.use<ImageDisplayFeature>(NO_LAYOUT) { it.showImage(img_file) } }
            ),
            FastColAction(
                    "View image",
                    "Opens image in an image browser widget.",
                    IconFA.IMAGE,
                    { ImageFileFormat.isSupported(it) },
                    { img_files -> APP.widgetManager.widgets.use<ImagesDisplayFeature>(NO_LAYOUT) { it.showImages(img_files) } }
            ),
            FastAction(
                    "Open widget",
                    "Opens exported widget.",
                    IconMD.IMPORT,
                    { it endsWithSuffix "fxwl" },
                    { APP.windowManager.launchComponent(it) }
            )
    )
    ap.register<MultipleFiles>(
            FastAction(
                    "Browse each",
                    "Browse each file individually. May have performance implications if too many.",
                    IconMD.FOLDER_MULTIPLE,
                    { it.browseEach() }
            ),
            FastAction(
                    "Browse each location",
                    "Browse each unique location. May have performance implications if too many.",
                    IconMD.FOLDER_MULTIPLE_OUTLINE,
                    { it.browseEachLocation() }
            ),
            FastAction(
                    "Browse shared root",
                    "Browse parent location of all files or do nothing if no such single location exists.",
                    IconMD.FOLDER_OUTLINE,
                    { it.browseCommonRoot() }
            )
    )
}

@Suppress("UNCHECKED_CAST")
fun addToLibraryConsumer(actionPane: ActionPane): ComplexActionData<Collection<File>, List<File>> = ComplexActionData(
        { files -> fut(files).map { getFilesAudio(it, AudioFileFormat.Use.APP, Integer.MAX_VALUE).toList() } },
        { files ->
            val makeWritable = V(true)
            val editInTagger = V(true)  // TODO: enable only if Tagger/SongReader is available and avoid casts
            val editOnlyAdded = V(false)
            val enqueue = V(false)
            val task = MetadataReader.buildAddItemsToLibTask()
            val info = ConvertTaskInfo(
                    title = null,
                    message = Label(),
                    skipped = Label(),
                    state = Label(),
                    pi = appProgressIndicator()
            )
            val tagger by lazy { APP.widgetManager.widgets.createNew("Tagger") }

            info.bind(task)
            layHorizontally(50.0, Pos.CENTER,
                    layVertically(50.0, Pos.CENTER,
                            ConfigPane(
                                    Config.forProperty(Boolean::class.java, "Make writable if read-only", makeWritable),
                                    Config.forProperty(Boolean::class.java, "Edit in Tagger", editInTagger),
                                    Config.forProperty(Boolean::class.java, "Edit only added files", editOnlyAdded),
                                    Config.forProperty(Boolean::class.java, "Enqueue in playlist", enqueue)
                            ),
                            layVertically(10.0, Pos.CENTER_LEFT,
                                    info.state,
                                    layHorizontally(10.0, Pos.CENTER_LEFT,
                                            info.message,
                                            info.progress
                                    ),
                                    info.skipped
                            ),
                            Icon(FontAwesomeIcon.CHECK, 25.0).onClick { e ->
                                (e.source as Icon).isDisable = true
                                fut(files())
                                        .use { if (makeWritable.get()) it.forEach { it.setWritable(true) } }
                                        .map { it.map { SimpleItem(it) } }
                                        .map(task)
                                        .showProgress(actionPane.actionProgress)
                                        .use(FX, Consumer { result ->
                                            if (editInTagger.get()) {
                                                val items = if (editOnlyAdded.get()) result.converted else result.all
                                                (tagger.controller as SongReader).read(items)
                                            }
                                            if (enqueue.get() && !result.all.isEmpty()) {
                                                APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(result.all) }
                                            }
                                        })
                            }
                                    .withText("Execute")
                    ),
                    tagger.load()
            )
        }
)

fun browseMultipleFiles(files: Sequence<File>) {
    val fs = files.asSequence().toSet()
    when {
        fs.isEmpty() -> {}
        fs.size==1 -> fs.firstOrNull()?.browse()
        else -> APP.actionPane.show(MultipleFiles(fs))
    }
}

class MultipleFiles(val files: Set<File>) {

    fun browseEach() = files.forEach { it.browse() }

    fun browseEachLocation() = files.map { if (it.isFile) it.parentDirOrRoot else it }.distinct().forEach { it.browse() }

    fun browseCommonRoot() {
        getCommonRoot(files)?.browse()
    }

}