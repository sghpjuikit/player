package sp.it.pl.main

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import javafx.concurrent.Worker.State.READY
import javafx.concurrent.Worker.State.SCHEDULED
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.stage.FileChooser.ExtensionFilter
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlinx.coroutines.runBlocking
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.isM3uPlaylist
import sp.it.pl.audio.playlist.readM3uPlaylist
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.addToLibTask
import sp.it.pl.layout.Component
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.exportFxwl
import sp.it.pl.layout.exportFxwlDefault
import sp.it.pl.layout.feature.ImageDisplayFeature
import sp.it.pl.layout.feature.ImagesDisplayFeature
import sp.it.pl.layout.feature.Opener
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.layout.loadComponentFxwlJson
import sp.it.pl.main.FileExtensions.fxwl
import sp.it.pl.main.Widgets.SONG_TAGGER
import sp.it.pl.plugin.impl.WallpaperChanger
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.clone
import sp.it.pl.ui.pane.ActionData
import sp.it.pl.ui.pane.ActionPane
import sp.it.pl.ui.pane.ComplexActionData
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.ui.pane.GroupApply.FOR_ALL
import sp.it.pl.ui.pane.fastAction
import sp.it.pl.ui.pane.fastColAction
import sp.it.pl.ui.pane.register
import sp.it.pl.ui.pane.slowAction
import sp.it.pl.ui.pane.slowColAction
import sp.it.util.Util.enumToHuman
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.access.v
import sp.it.util.action.ActionRegistrar
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.FX
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.future.runGet
import sp.it.util.async.launch
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.async.runLater
import sp.it.util.collections.map.KClassListMap
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyIf
import sp.it.util.conf.readOnlyUnless
import sp.it.util.dev.fail
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.Util.getCommonRoot
import sp.it.util.file.hasExtension
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.setCreated
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.system.browse
import sp.it.util.system.chooseFile
import sp.it.util.system.chooseFiles
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.system.saveFile
import sp.it.util.type.argOf
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.raw
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.millis

object ActionsPaneGenericActions {
   val actionsAll = KClassListMap<ActionData<*, *>> { fail() }

   inline fun <reified T> register(vararg actions: ActionData<T, *>) = actionsAll.accumulate(T::class, actions.asIterable())

   init {
      // register app actions automatically
      APP.actions::class.memberProperties.forEach {
         if (it.returnType.isSubtypeOf<ActionData<*,*>>())
            it.asIs<KProperty1<AppActions, ActionData<Any?,*>>>().get(APP.actions).net {
               actionsAll.accumulate(
                  when (it.groupApply) {
                     FOR_ALL -> it.type.type.argOf(Collection::class, 0).type?.raw ?: Any::class
                     else -> it.type.raw
                  },
                  it
               )
            }
      }

      register<App>(
         fastAction(IconFA.GEARS, ActionRegistrar["Open settings"]),
         fastAction(IconUN(0x1f4c1), ActionRegistrar["Open app event log"]),
      )
   }
}


fun ActionPane.initGenericActions(): ActionPane = also { ap ->
   ActionsPaneGenericActions.actionsAll.forEach { (kClass, actions) -> actions.forEach { ap.register<Any>(kClass.asIs(), it.asIs()) } }
}

@Suppress("RemoveExplicitTypeArguments")
fun ActionPane.initActionPane(): ActionPane = also { ap ->
   ap.initGenericActions()

   ap.register<Any?>(
      fastColAction<Any?>(
         "Set as data",
         "Sets the selected data as input.",
         IconMD.DATABASE,
         { it !is App && it !is AppHelp && it !is AppOpen },
         { apOrApp.show(it) }
      ).preventClosing(),
      fastColAction(
         "Open in Converter",
         "Open data in Converter.",
         IconMD.SWAP_HORIZONTAL,
         { it !is App && it !is AppHelp && it !is AppOpen },
         { f -> APP.widgetManager.widgets.use<Opener>(ANY) { it.open(f) } }   // TODO: make sure it opens Converter or support multiple Opener types
      )
   )
   ap.register<App>(
      fastAction(IconFA.FOLDER, ActionRegistrar["Open app directory"]),
      fastAction<App>("Help", "Set of actions for advanced users", IconOC.CIRCUIT_BOARD, { ap.show(AppHelp) }).preventClosing(),
      fastAction<App>("Open...", "Set of actions to open things", IconMD.OPEN_IN_APP, { ap.show(AppOpen) }).preventClosing(),
      fastAction("Open help", "Display all available shortcuts", IconMD.KEYBOARD_VARIANT) { it.actions.showShortcuts() },
   )
   ap.register<AppOpen>(
      fastAction<AppOpen>(
         "Select file",
         "Open file chooser to select files",
         IconUN(0x1f4c4),
         { chooseFiles("Select file...", null, window).ifOk(apOrApp::show) }
      ).preventClosing(),
      fastAction<AppOpen>(
         "Select directory",
         "Open file chooser to select directory",
         IconUN(0x1f4c1),
         { chooseFile("Select directory...", DIRECTORY, null, window).ifOk(apOrApp::show) }
      ).preventClosing(),
      fastAction(
         "Open widget",
         "Open file chooser to open an exported widget",
         IconMA.WIDGETS,
         {

            chooseFile("Open widget...", FILE, APP.location.user.layouts, window, ExtensionFilter("Component", "*.fxwl"))
               .ifOk { FX.launch { APP.windowManager.launchComponent(it) } }
         }
      ),
      fastAction(
         "Open skin",
         "Open file chooser to find a skin",
         IconMA.BRUSH,
         {
            chooseFile("Open skin...", FILE, APP.location.skins, window, ExtensionFilter("Skin", "*.css"))
               .ifOk { APP.ui.setSkin(it) }
         }
      ),
      fastAction(
         "Open audio files",
         "Open file chooser to find a audio files",
         IconMD.MUSIC_NOTE,
         {
            chooseFiles("Open audio...", APP.locationHome, window, audioExtensionFilter())
               .map { runLater { APP.ui.actionPane.orBuild.show(it) } }   // may auto-close on finish, delay show()
         }
      )
   )
   ap.register<Any>(
      fastAction<Any>(
         "Detect content",
         "Tries to detect and convert the content of the text into more specific one",
         IconFA.SEARCH_PLUS,
         { apOrApp.show(it.detectContent()) }
      ).preventClosing()
   )
   ap.register<Song>(
      fastColAction(
         "Add to new playlist",
         "Add songs to new playlist widget.",
         IconMD.PLAYLIST_PLUS,
         { songs -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addItems(songs) } }
      ),
      fastColAction(
         "Add to existing playlist",
         "Add songs to existing playlist widget if possible or to a new one if not.",
         IconMD.PLAYLIST_PLUS,
         { songs -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(songs) } }
      ),
      fastColAction(
         "Update from file",
         "Updates library data for the specified songs from their file metadata. The difference between the data " +
            "in the database and real metadata cab be a result of a bug or file edited externally. " +
            "After this, the library will be synchronized with the file data.",
         IconFA.REFRESH,
         { APP.audio.refreshSongs(it) }
      ),
      fastColAction<Song>(
         "Add to library",
         "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
         IconMD.DATABASE_PLUS,
         { }
      ).preventClosing { op ->
         ComplexActionData(
            { songs -> fut(songs.mapNotNull { it.getFile() }) },
            addToLibraryConsumer(op).gui
         )
      },
      fastColAction(
         "Remove from library",
         "Removes all specified songs from library. After this library will contain none of these songs.",
         IconMD.DATABASE_MINUS,
         { APP.db.removeSongs(it) }
      ),
      fastColAction(
         "Show",
         "Shows songs in a table.",
         IconMA.COLLECTIONS,
         { songs ->
            APP.widgetManager.widgets.find(Widgets.SONG_TABLE_NAME, NEW).ifNotNull {
               it.controller!!.io.i.getInput<List<Metadata>>("To display").value = songs.map { it.toMeta() }
            }
         }
      ),
      fastColAction(
         "Show as Group",
         "Group songs in a table.",
         IconMA.COLLECTIONS,
         { songs ->
            APP.widgetManager.widgets.find(Widgets.SONG_GROUP_TABLE_NAME, NEW).ifNotNull {
               it.controller!!.io.i.getInput<List<Metadata>>("To display").value = songs.map { it.toMeta() }
            }
         }
      )
   )
   ap.register<File>(
      fastAction(
         "Recycle", "Moves file to recycle bin.",
         IconMA.DELETE,
         { it.recycle() }
      ),
      slowAction(
         "Set as wallpaper", "Sets image as wallpaper.",
         IconMA.IMAGE_ASPECT_RATIO,
         { it.isImage() && APP.plugins.get<WallpaperChanger>()!=null },
         { f -> APP.plugins.use<WallpaperChanger> { it.wallpaperFile.value = f } }
      ),
      fastAction(
         "Open (OS)", "Opens file in a native program associated with this file type.",
         IconMA.OPEN_IN_NEW,
         { it.open() }
      ),
      fastAction(
         "Edit (OS)", "Edit file in a native editor program associated with this file type.",
         IconFA.EDIT,
         { it.edit() }
      ),
      fastAction(
         "Browse (OS)", "Browse file in a native file system browser.",
         IconFA.FOLDER_OPEN_ALT,
         { it.browse() }
      ),
      fastColAction(
         "Add to new playlist",
         "Add songs to new playlist widget.",
         IconMD.PLAYLIST_PLUS,
         { it.isAudio() },
         { fs -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addFiles(fs) } }
      ),
      slowAction(
         "Open playlist",
         "Add songs to new playlist widget.",
         IconMD.PLAYLIST_PLAY,
         { it.isM3uPlaylist() },
         { f -> PlaylistManager.use { it.setAndPlay(readM3uPlaylist(f)) } }
      ),
      slowColAction<File>(
         "Find files",
         "Looks for files recursively in the the data.",
         IconMD.FILE_FIND,
         { fs ->
            val rfs = FileFlatter.ALL.flatten(fs).map { CachingFile(it) }.toList()
            runFX { apOrApp.show(rfs) }
         }
      ).preventClosing(),
      slowColAction<File>(
         "Add to library",
         "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
         IconMD.DATABASE_PLUS,
         { }
      ).preventClosing { addToLibraryConsumer(it) },
      fastColAction(
         "Add to existing playlist",
         "Add songs to existing playlist widget if possible or to a new one if not.",
         IconMD.PLAYLIST_PLUS,
         { it.isAudio() },
         { f -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addFiles(f) } }
      ),
      fastAction(
         "Apply skin",
         "Apply skin on the application.",
         IconMD.BRUSH,
         { f -> f.isValidSkinFile() },
         { f -> APP.ui.setSkin(f) }
      ),
      fastAction(
         "View image",
         "Opens image in an image viewer widget.",
         IconFA.IMAGE,
         { it.isImage() },
         { img_file -> APP.widgetManager.widgets.use<ImageDisplayFeature>(NO_LAYOUT) { it.showImage(img_file) } }
      ),
      fastColAction(
         "View image",
         "Opens images in an image browser widget.",
         IconFA.IMAGE,
         { it.isImage() },
         { img_files -> APP.widgetManager.widgets.use<ImagesDisplayFeature>(NO_LAYOUT) { it.showImages(img_files) } }
      ),
      fastAction(
         "Open widget",
         "Opens exported widget.",
         IconMD.IMPORT,
         { it hasExtension fxwl },
         { it.loadComponentFxwlJson() ui { it.ifNotNull { APP.windowManager.showWindow(it) } } }
      ),
      fastColAction(
         "Set created to last modified time",
         "Sets created time to last modified time for the file. Useful after a file copy destroyed this value.",
         IconFA.CLOCK_ALT,
         {
            it.forEach {
               try {
                  val time = Files.readAttributes(it.toPath(), BasicFileAttributes::class.java)?.lastModifiedTime()!!
                  it.setCreated(time).ifError { throw it }
               } catch (e: Throwable) {
                  e.printStackTrace()
                  // TODO: logger.error(e) { "Failed to change the creation time to last modified time file=$it" }
               }
            }
         }
      )
   )
   ap.register<Window>(
      fastAction(
         "Clone",
         "Shows new window with the same content and state as this one.",
         IconFA.CLONE,
         { it.clone() }
      )
   )
   ap.register<MultipleFiles>(
      fastAction(
         "Browse each",
         "Browse each file individually. May have performance implications if too many.",
         IconMD.FOLDER_MULTIPLE,
         { it.browseEach() }
      ),
      fastAction(
         "Browse each location",
         "Browse each unique location. May have performance implications if too many.",
         IconMD.FOLDER_MULTIPLE_OUTLINE,
         { it.browseEachLocation() }
      ),
      fastAction(
         "Browse shared root",
         "Browse parent location of all files or do nothing if no such single location exists.",
         IconMD.FOLDER_OUTLINE,
         { it.browseCommonRoot() }
      )
   )
}

@Suppress("UNCHECKED_CAST")
private fun addToLibraryConsumer(actionPane: ActionPane): ComplexActionData<Collection<File>, Collection<File>> = ComplexActionData(
   { files -> runIO { findAudio(files).map { CachingFile(it) }.toList() } },
   { audioFiles ->
      val executed = v(false)
      val conf = object: ConfigurableBase<Boolean>() {
         val makeWritable by cv(true).readOnlyIf(executed).def(name = "Make files writable if read-only", group = "1")
         val editInTagger by cv(false).def(name = "Edit in ${Widgets.SONG_TAGGER_NAME}", group = "2")
         val editOnlyAdded by cv(false).readOnlyUnless(editInTagger).def(name = "Edit only added files", group = "3")
         val enqueue by cv(false).def(name = "Enqueue in playlist", group = "4")
      }
      val task = Song.addToLibTask(audioFiles.map { SimpleSong(it) })
      val info = object: Any() {
         private val computeProgress = { it: Number ->
            when (task.state) {
               SCHEDULED, READY -> 1.0
               else -> it.toDouble()
            }
         }
         val message = label { textProperty() syncFrom task.messageProperty() }
         val state = label { task.stateProperty() sync { text = "State: ${enumToHuman(it)}" } }
         val progress = appProgressIndicator().apply { task.progressProperty() sync { progress = computeProgress(it) } }
      }

      hBox(50, CENTER) {
         val content = this
         lay(ALWAYS) += vBox(50, CENTER) {
            lay += ConfigPane(conf)
            lay += vBox(10, CENTER_LEFT) {
               opacity = 0.0

               lay += info.state
               lay += hBox(10, CENTER_LEFT) {
                  lay += info.message
                  lay += info.progress
               }
            }
            lay += formIcon(IconFA.CHECK, "Execute") {
               executed.value = true

               anim(500.millis) {
                  content.children.getOrNull(0).asIf<Pane>()?.children?.getOrNull(0)?.opacity = (1 - it)*(1 - it)
                  content.children.getOrNull(0).asIf<Pane>()?.children?.getOrNull(1)?.opacity = it*it
                  content.children.getOrNull(0).asIf<Pane>()?.children?.getOrNull(2)?.opacity = (1 - it)*(1 - it)
               }.play()


               runIO {
                  if (conf.makeWritable.value) audioFiles.forEach { it.setWritable(true) }
                  task.runGet().toTry().orNull()
               }.ui { result ->
                  if (result!=null) {
                     if (conf.editInTagger.value) {
                        val tagger = runBlocking { APP.widgetManager.factories.getFactory(SONG_TAGGER.id).orNull()?.create() }
                        val songs = if (conf.editOnlyAdded.value) result.converted else result.all
                        if (tagger!=null) {
                           anim(500.millis) {
                              content.children[0].opacity = it*it
                              content.children[1].opacity = it*it
                           }.apply {
                              playAgainIfFinished = false
                           }.playCloseDoOpen {
                              content.children[1].asIs<Pane>().lay += tagger.load()
                              (tagger.controller as SongReader).read(songs)
                           }
                        }
                     }
                     if (conf.enqueue.value && result.all.isNotEmpty()) {
                        APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(result.all) }
                     }
                  }
               }.withProgress(actionPane.actionProgress)
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

/** Denotes action pane data for 'App.Developer' actions. */
object AppHelp

/** Denotes action pane data representing multiple files for browse actions. */
class MultipleFiles(val files: Set<File>) {

   fun browseEach() = files.forEach { it.browse() }

   fun browseEachLocation() = files.map { if (it.isFile) it.parentDirOrRoot else it }.distinct().forEach { it.browse() }

   fun browseCommonRoot() {
      getCommonRoot(files)?.browse()
   }

}