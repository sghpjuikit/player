package sp.it.pl.main

import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.control.Label
import javafx.scene.layout.Pane
import javafx.stage.FileChooser.ExtensionFilter
import sp.it.pl.audio.Item
import sp.it.pl.audio.Player
import sp.it.pl.audio.SimpleItem
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.isPlaylistFile
import sp.it.pl.audio.playlist.readPlaylist
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataReader
import sp.it.pl.gui.nodeinfo.ConvertTaskInfo
import sp.it.pl.gui.pane.ActionPane
import sp.it.pl.gui.pane.ComplexActionData
import sp.it.pl.gui.pane.ConfigPane
import sp.it.pl.gui.pane.FastAction
import sp.it.pl.gui.pane.FastColAction
import sp.it.pl.gui.pane.SlowAction
import sp.it.pl.gui.pane.SlowColAction
import sp.it.pl.gui.pane.register
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
import sp.it.pl.util.access.v
import sp.it.pl.util.action.Action
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.async.future.Fut.Companion.fut
import sp.it.pl.util.async.runLater
import sp.it.pl.util.conf.ConfigurableBase
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.readOnlyIf
import sp.it.pl.util.conf.readOnlyUnless
import sp.it.pl.util.file.AudioFileFormat
import sp.it.pl.util.file.AudioFileFormat.Use
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.FileType.DIRECTORY
import sp.it.pl.util.file.FileType.FILE
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.Util.getCommonRoot
import sp.it.pl.util.file.Util.getFilesAudio
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.functional.Try
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.invoke
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.stackPane
import sp.it.pl.util.graphics.vBox
import sp.it.pl.util.math.millis
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.system.browse
import sp.it.pl.util.system.chooseFile
import sp.it.pl.util.system.chooseFiles
import sp.it.pl.util.system.edit
import sp.it.pl.util.system.open
import sp.it.pl.util.system.recycle
import sp.it.pl.util.system.saveFile
import java.io.File
import kotlin.streams.toList

@Suppress("RemoveExplicitTypeArguments")
fun ActionPane.initActionPane(): ActionPane = also { ap ->
    ap.register<Any?>(
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
    ap.register<AppOpen>(
            FastAction(
                    "Select file",
                    "Open file chooser to select files",
                    IconMD.FILE,
                    ap.converting { chooseFiles("Select file...", null, ap.scene?.window) }
            ),
            FastAction(
                    "Select directory",
                    "Open file chooser to select directory",
                    IconMD.FOLDER,
                    ap.converting { chooseFile("Select directory...", DIRECTORY, null, ap.scene?.window) }
            ),
            FastAction(
                    "Open widget",
                    "Open file chooser to open an exported widget",
                    IconMA.WIDGETS,
                    ap.converting {
                        chooseFile("Open widget...", FILE, APP.DIR_LAYOUTS, ap.scene?.window, ExtensionFilter("Component", "*.fxwl"))
                                .map { APP.windowManager.launchComponent(it) }
                    }
            ),
            FastAction(
                    "Open skin",
                    "Open file chooser to find a skin",
                    IconMA.BRUSH,
                    ap.converting {
                        chooseFile("Open skin...", FILE, APP.DIR_SKINS, ap.scene?.window, ExtensionFilter("Skin", "*.css"))
                                .map { APP.ui.setSkin(it) }
                    }
            ),
            FastAction(
                    "Open audio files",
                    "Open file chooser to find a audio files",
                    IconMD.MUSIC_NOTE,
                    {
                        val filters = AudioFileFormat.supportedValues(Use.APP).map { it.toExtFilter() }.toTypedArray()
                        chooseFiles("Open audio...", APP.DIR_HOME, ap.scene?.window, *filters)
                                .map { runLater { APP.actionPane.show(it) } }   // may auto-close on finish, delay show()
                    }
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
    ap.register<Widget>(
            FastAction(
                    "Use as default",
                    "Uses settings of this widget as default settings when creating widgets of this type. This " +
                    "overrides the default settings of the widget set by the developer. For using multiple widget " +
                    "configurations at once, use 'Export' instead.",
                    IconMD.SETTINGS_BOX,
                    { it.storeDefaultConfigs() }
            ),
            FastAction(
                    "Clear default",
                    "Removes overridden default settings for this widget. New widgets will start with settings set " +
                    "by the developer.",
                    IconMD.SETTINGS_BOX,
                    { it.clearDefaultConfigs() }
            )
    )
    ap.register<Item>(
            SlowAction(
                    "Print raw metadata", "Prints all audio metadata to console.",
                    IconMA.IMAGE_ASPECT_RATIO,
                    { APP.actions.printAllAudioItemMetadata(it) }
            ),
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
            FastColAction<Item>(
                    "Add to library",
                    "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
                    IconMD.DATABASE_PLUS,
                    { }
            ).preventClosing {
                op -> ComplexActionData(
                    { items -> fut(items).then { it.mapNotNull { it.getFile() } } },
                    addToLibraryConsumer(op).gui
                )
            },
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
                                .find(Widgets.SONG_TABLE, NEW, false)
                                .ifPresent {
                                    it.controller.ownedInputs.getInput<List<Metadata>>("To display").setValue(items.map { it.toMeta() })
                                }
                    }
            ),
            FastColAction(
                    "Show as Group",
                    "Group items in a table.",
                    IconMA.COLLECTIONS,
                    { items -> APP.widgetManager.widgets
                            .find(Widgets.SONG_GROUP_TABLE, NEW, false)
                                .ifPresent {
                                    it.controller.ownedInputs.getInput<List<Metadata>>("To display").setValue(items.map { it.toMeta() })
                                }
                    }
            )
    )
    ap.register<File>(
            FastAction(
                    "Recycle", "Moves file to recycle bin.",
                    IconMA.DELETE,
                    { it.recycle() }
            ),
            SlowAction(
                    "Print raw  metadata", "Prints all image metadata to console.",
                    IconMA.IMAGE_ASPECT_RATIO,
                    { ImageFileFormat.isSupported(it) },
                    { APP.actions.printAllImageFileMetadata(it) }
            ),
            FastAction(
                    "Print raw  metadata", "Prints all audio metadata to console.",
                    IconMA.IMAGE_ASPECT_RATIO,
                    { AudioFileFormat.isSupported(it, Use.APP) },
                    { APP.actions.printAllAudioFileMetadata(it) }
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
                    "Add songs to new playlist widget.",
                    IconMD.PLAYLIST_PLUS,
                    { f -> AudioFileFormat.isSupported(f, Use.APP) },
                    { fs -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addFiles(fs) } }
            ),
            SlowAction(
                    "Open playlist",
                    "Add songs to new playlist widget.",
                    IconMD.PLAYLIST_PLAY,
                    { f -> f.isPlaylistFile() },
                    { f -> PlaylistManager.use { it.setNplay(readPlaylist(f)) } }
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
                    "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
                    IconMD.DATABASE_PLUS,
                    { }
            ).preventClosing { addToLibraryConsumer(it) },
            FastColAction(
                    "Add to existing playlist",
                    "Add items to existing playlist widget if possible or to a new one if not.",
                    IconMD.PLAYLIST_PLUS,
                    { f -> AudioFileFormat.isSupported(f, Use.APP) },
                    { f -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addFiles(f) } }
            ),
            FastAction(
                    "Apply skin",
                    "Apply skin on the application.",
                    IconMD.BRUSH,
                    { f -> f.isValidSkinFile() },
                    { f -> APP.ui.setSkin(f) }
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
                    { it hasExtension "fxwl" },
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
private fun addToLibraryConsumer(actionPane: ActionPane): ComplexActionData<Collection<File>, List<File>> = ComplexActionData(
        { files -> fut(files).then { getFilesAudio(it, AudioFileFormat.Use.APP, Integer.MAX_VALUE).toList() } },
        { files ->

            val executed = v(false)
            val conf = object: ConfigurableBase<Boolean>() {
                @IsConfig(name = "Make files writable if read-only", group ="1") val makeWritable by cv(true).readOnlyIf(executed)
                @IsConfig(name = "Edit in ${Widgets.SONG_TAGGER}", group ="2") val editInTagger by cv(false)
                @IsConfig(name = "Edit only added files", group ="3") val editOnlyAdded by cv(false).readOnlyUnless(editInTagger)
                @IsConfig(name = "Enqueue in playlist", group ="4") val enqueue by cv(false)
            }
            val task = MetadataReader.buildAddItemsToLibTask()
            val info = ConvertTaskInfo(
                    title = null,
                    message = Label(),
                    skipped = Label(),
                    state = Label(),
                    pi = appProgressIndicator()
            ).apply {
                bind(task)
            }

            hBox(50, CENTER) {
                val content = this
                lay += vBox(50, CENTER) {
                    lay += ConfigPane(conf)
                    lay += vBox(10, CENTER_LEFT) {
                        opacity = 0.0

                        lay += info.state!!
                        lay += hBox(10, CENTER_LEFT) {
                            lay += info.message!!
                            lay += info.progress!!
                        }
                        lay += info.skipped!!
                    }
                    lay += formIcon(IconFA.CHECK, "Execute") {
                        executed.value = true

                        anim(500.millis) {
                            content.children.getOrNull(0).asIf<Pane>()?.children?.getOrNull(0)?.opacity = (1-it)*(1-it)
                            content.children.getOrNull(0).asIf<Pane>()?.children?.getOrNull(1)?.opacity = it*it
                            content.children.getOrNull(0).asIf<Pane>()?.children?.getOrNull(2)?.opacity = (1-it)*(1-it)
                        }.play()

                        fut(files())
                                .use { if (conf.makeWritable.value) it.forEach { it.setWritable(true) } }
                                .then { task(it.map { SimpleItem(it) }) }
                                .ui { result ->
                                    if (conf.editInTagger.value) {
                                        val tagger = APP.widgetManager.factories.getFactoryByGuiName(Widgets.SONG_TAGGER)?.create()
                                        val items = if (conf.editOnlyAdded.value) result.converted else result.all
                                        if (tagger!=null) {
                                            anim(500.millis) {
                                                content.children[0].opacity = it*it
                                                content.children[1].opacity = it*it
                                            }.apply {
                                                playAgainIfFinished = false
                                            }.playCloseDoOpen {
                                                content.children[1].asIf<Pane>()!!.lay += tagger.load()
                                                (tagger.controller as SongReader).read(items)
                                            }
                                        }
                                    }
                                    if (conf.enqueue.value && !result.all.isEmpty()) {
                                        APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(result.all) }
                                    }
                                }
                                .showProgress(actionPane.actionProgress)
                    }.apply {
                        disableProperty() syncFrom executed
                    }
                }
                lay += stackPane()
            }
        }
)

/** Denotes action pane data for 'Open...' actions. */
object AppOpen

/** Denotes action pane data representing multiple files for browse actions. */
class MultipleFiles(val files: Set<File>) {

    fun browseEach() = files.forEach { it.browse() }

    fun browseEachLocation() = files.map { if (it.isFile) it.parentDirOrRoot else it }.distinct().forEach { it.browse() }

    fun browseCommonRoot() {
        getCommonRoot(files)?.browse()
    }

}