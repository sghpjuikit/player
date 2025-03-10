package sp.it.pl.main

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import javafx.concurrent.Worker.State.READY
import javafx.concurrent.Worker.State.SCHEDULED
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlinx.coroutines.runBlocking
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.isM3uPlaylist
import sp.it.pl.audio.playlist.readM3uPlaylist
import sp.it.pl.audio.tagging.AddSongsToLibResult
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.addToLibTask
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.feature.ImageDisplayFeature
import sp.it.pl.layout.feature.ImagesDisplayFeature
import sp.it.pl.layout.feature.Opener
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.layout.hasFeature
import sp.it.pl.layout.loadComponentFxwlJson
import sp.it.pl.layout.orNone
import sp.it.pl.main.FileExtensions.fxwl
import sp.it.pl.main.Widgets.SONG_TAGGER
import sp.it.pl.plugin.impl.WallpaperChanger
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.clone
import sp.it.pl.ui.pane.ActionData
import sp.it.pl.ui.pane.ActionData.GroupApply.FOR_ALL
import sp.it.pl.ui.pane.ActionData.Threading.BLOCK
import sp.it.pl.ui.pane.ActionData.Threading.UI
import sp.it.pl.ui.pane.ActionData.UiInput
import sp.it.pl.ui.pane.ActionPane
import sp.it.pl.ui.pane.ComplexActionData
import sp.it.pl.ui.pane.ConfigPane
import sp.it.pl.ui.pane.action
import sp.it.pl.ui.pane.actionAll
import sp.it.pl.ui.pane.nounwrapWrap
import sp.it.pl.ui.pane.register
import sp.it.util.Util.enumToHuman
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.FX
import sp.it.util.async.VT
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.future.runAndGet
import sp.it.util.async.runVT
import sp.it.util.collections.collectionWrap
import sp.it.util.collections.map.KClassListMap
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.readOnlyIf
import sp.it.util.conf.readOnlyUnless
import sp.it.util.dev.fail
import sp.it.util.file.Util.getCommonRoot
import sp.it.util.file.hasExtension
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.setCreated
import sp.it.util.functional.Try
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.flatten
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.net
import sp.it.util.functional.runTry
import sp.it.util.functional.toUnit
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncFrom
import sp.it.util.system.browse
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.type.argOf
import sp.it.util.type.isObject
import sp.it.util.type.isSubtypeOf
import sp.it.util.type.raw
import sp.it.util.ui.hBox
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.stackPane
import sp.it.util.ui.vBox
import sp.it.util.units.millis

private val logger = KotlinLogging.logger { }

object ActionsPaneGenericActions {
   val actionsAll = KClassListMap<ActionData<*, *>> { fail() }

   inline fun <reified T> register(vararg actions: ActionData<T, *>) = actionsAll.accumulate(T::class, actions.asIterable())

   init {
      // register app actions automatically
      listOf(
         APP.actions,
         AppActionsAny,
         AppActionsApp,
         AppActionsAppHelp,
         AppActionsAppOpen,
         AppActionsAppTest,
         AppActionsComponent,
         AppActionsString,
         AppActionsFile,
         AppActionsUnit,
         AppActionsWindow,
      ).forEach { actionHolder ->
         actionHolder::class.memberProperties.forEach {
            if (it.returnType.isSubtypeOf<ActionData<*,*>>())
               it.asIs<KProperty1<Any?, ActionData<Any?,*>>>().get(actionHolder).net {
                  actionsAll.accumulate(
                     when (it.groupApply) {
                        FOR_ALL -> it.type.type.argOf(Collection::class, 0).type?.raw ?: Any::class
                        else -> it.type.raw
                     },
                     it
                  )
               }
         }
      }
   }
}


fun ActionPane.initGenericActions(): ActionPane = also { ap ->
   ActionsPaneGenericActions.actionsAll.forEach { (kClass, actions) -> actions.forEach { ap.register<Any>(kClass.asIs(), it.asIs()) } }
}

fun ActionPane.initActionPane(): ActionPane = also { ap ->
   ap.initGenericActions()

   ap.register<Any?>(
      action<Any?>(
         "Set as data",
         "Sets the selected data as input.",
         IconMD.DATABASE,
         constriction = { it!=null && it !is App && !it::class.isObject }
      ) { apOrApp.show(it) }.preventClosing(),
      action(
         "Open in...",
         "Open in widget that can open this data",
         IconMD.SWAP_HORIZONTAL,
         constriction = { it!=null && it !is App && !it::class.isObject }
      ) { f ->
         UiInput(
            null,
            true,
            APP.widgetManager.factories.getFactories().filter { it.hasFeature<Opener>() }.toList().map { wf ->
               action<WidgetFactory<*>>(
                  "Open with ${wf.name}",
                  wf.description,
                  wf.icon ?: IconFA.COPY,
                  UI,
                  { APP.widgetManager.widgets.use({ it.id==wf.id }, ANY) { it.controller.asIs<Opener>().open(f) } }
               )
            }
         )
      }
   )
   ap.register<SongToAdd>(
      actionAll<SongToAdd>(
         "Add to library",
         "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
         IconMD.DATABASE_PLUS
      ) { addToLibraryConsumer(apOrApp) },
   )
   ap.register<Song>(
      actionAll(
         "Add to new playlist",
         "Add songs to new playlist widget.",
         IconMD.PLAYLIST_PLUS
      ) { songs -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addItems(songs) } },
      actionAll(
         "Add to existing playlist",
         "Add songs to existing playlist widget if possible or to a new one if not.",
         IconMD.PLAYLIST_PLUS
      ) { songs -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(songs) } },
      actionAll(
         "Update from file",
         "Updates library data for the specified songs from their file metadata. The difference between the data " +
            "in the database and real metadata cab be a result of a bug or file edited externally. " +
            "After this, the library will be synchronized with the file data.",
         IconFA.REFRESH
      ) { APP.audio.refreshSongs(it) },
      actionAll<Song>(
         "Add to library",
         "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
         IconMD.DATABASE_PLUS
      ) { nounwrapWrap(it.map { SongToAdd(it.uri, APP.db.exists(it)) }) },
      actionAll(
         "Remove from library",
         "Removes all specified songs from library. After this library will contain none of these songs.",
         IconMD.DATABASE_MINUS
      ) { APP.db.removeSongs(it) },
      actionAll(
         "Show",
         "Shows songs in a table.",
         IconMA.COLLECTIONS
      ) { songs ->
         APP.widgetManager.widgets.find(Widgets.SONG_TABLE_NAME, NEW).ifNotNull {
            it.controller!!.io.i.getInput<List<Metadata>>("To display").value = songs.map { it.toMeta() }
         }
      },
      actionAll(
         "Show as Group",
         "Group songs in a table.",
         IconMA.COLLECTIONS
      ) { songs ->
         APP.widgetManager.widgets.find(Widgets.SONG_GROUP_TABLE_NAME, NEW).ifNotNull {
            it.controller!!.io.i.getInput<List<Metadata>>("To display").value = songs.map { it.toMeta() }
         }
      }
   )
   ap.register<File>(
      action("Recycle", "Moves file to recycle bin.", IconMA.DELETE) { it.recycle() },
      action(
         "Set as wallpaper", "Sets image as wallpaper.",
         IconMA.IMAGE_ASPECT_RATIO,
         BLOCK,
         { it.isImage() && APP.plugins.get<WallpaperChanger>()!=null },
      ) { f -> APP.plugins.use<WallpaperChanger> { it.wallpaperFile.value = f } },
      action(
         "Open (OS)", "Opens file in a native program associated with this file type.",
         IconMA.OPEN_IN_NEW,
      ) { it.open() },
      action(
         "Edit (OS)", "Edit file in a native editor program associated with this file type.",
         IconFA.EDIT,
      ) { it.edit() },
      action(
         "Browse (OS)", "Browse file in a native file system browser.",
         IconFA.FOLDER_OPEN_ALT,
      ) { it.browse() },
      actionAll(
         "Add to new playlist",
         "Add songs to new playlist widget.",
         IconMD.PLAYLIST_PLUS,
         constriction = { it.isAudioOrVideo() },
      ) { fs -> APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { it.playlist.addFiles(fs) } },
      action(
         "Open playlist",
         "Add songs to new playlist widget.",
         IconMD.PLAYLIST_PLAY,
         BLOCK,
         constriction = { it.isM3uPlaylist() },
      ) { f -> PlaylistManager.use { it.setAndPlay(readM3uPlaylist(f)) } },
      actionAll<File>(
         "Add to library",
         "Add songs to library. The process is customizable and it is also possible to edit the songs in the tag editor.",
         IconMD.DATABASE_PLUS
      ) {
         nounwrapWrap(findAudio(it).asSequence().map { it.toURI() }.map { SongToAdd(it, APP.db.exists(it)) }.toList())
      },
      actionAll(
         "Add to existing playlist",
         "Add songs to existing playlist widget if possible or to a new one if not.",
         IconMD.PLAYLIST_PLUS,
         constriction = { it.isAudioOrVideo() },
      ) { f -> APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addFiles(f) } },
      action(
         "Apply skin",
         "Apply skin on the application.",
         IconMD.BRUSH,
         constriction = { f -> f.isValidSkinFile() },
      ) { f -> APP.ui.setSkin(f) },
      action(
         "View image",
         "Opens image in an image viewer widget.",
         IconFA.IMAGE,
         constriction = { it.isImage() },
      ) { imgFile -> APP.widgetManager.widgets.use<ImageDisplayFeature>(NO_LAYOUT) { it.showImage(imgFile) } },
      actionAll(
         "View image",
         "Opens images in an image browser widget.",
         IconFA.IMAGE,
         constriction = { it.isImage() },
      ) { imgFiles -> APP.widgetManager.widgets.use<ImagesDisplayFeature>(NO_LAYOUT) { it.showImages(imgFiles) } },
      action(
         "Open widget",
         "Opens exported widget.",
         IconMD.IMPORT,
         constriction = { it hasExtension fxwl },
      ) { it.loadComponentFxwlJson() ui { it.ifNotNull { APP.windowManager.showWindow(it) }.toUnit() } },
      actionAll(
         "Set created to last modified time",
         "Sets created time to last modified time for the file. Useful after a file copy destroyed this value.",
         IconFA.CLOCK_ALT
      ) {
            it.forEach { f ->
               runTry {
                  val time = Files.readAttributes(f.toPath(), BasicFileAttributes::class.java)?.lastModifiedTime()!!
                  f.setCreated(time).ifError { throw it }
               }.ifError {
                  logger.error(it) { "Failed to change the creation time to last modified time file=$f" }
               }
            }
         }
   )
   ap.register<Window>(
      action(
         "Clone",
         "Shows new window with the same content and state as this one.",
         IconFA.CLONE,
      ) { it.clone() }
   )
   ap.register<MultipleFiles>(
      action(
         "Browse each",
         "Browse each file individually. May have performance implications if too many.",
         IconMD.FOLDER_MULTIPLE,
      ) { it.browseEach() },
      action(
         "Browse each location",
         "Browse each unique location. May have performance implications if too many.",
         IconMD.FOLDER_MULTIPLE_OUTLINE,
      ) { it.browseEachLocation() },
      action(
         "Browse shared root",
         "Browse parent location of all files or do nothing if no such single location exists.",
         IconMD.FOLDER_OUTLINE,
      ) { it.browseCommonRoot() }
   )
}

private fun addToLibraryConsumer(actionPane: ActionPane) =
   collectionWrap(actionPane.data).asIs<List<SongToAdd>>().let { songsToAdd ->
         val songs = songsToAdd.map { SimpleSong(it.uri) }
         val executed = v(false)
         val conf = object: ConfigurableBase<Boolean>() {
            val makeWritable by cv(true).readOnlyIf(executed).def(name = "Make files writable if read-only", group = "1")
            val editInTagger by cv(false).def(name = "Edit in ${Widgets.SONG_TAGGER_NAME}", group = "2")
            val editOnlyAdded by cv(false).readOnlyUnless(editInTagger).def(name = "Edit only added files", group = "3")
            val enqueue by cv(false).def(name = "Enqueue in playlist", group = "4")
         }
         val task = Song.addToLibTask(songs)
         val info = object: Any() {
            private val computeProgress = { it: Number ->
               when (task.state) {
                  SCHEDULED, READY -> 1.0
                  else -> it.toDouble()
               }
            }
            val message = label { textProperty() syncFrom task.messageProperty() }
            val state = label { task.stateProperty() sync { text = "State: ${enumToHuman(it)}" } }
            val progress = appProgressIndicator().apply {
               task.progressProperty() sync { progress = computeProgress(it) }
               task.stateProperty() sync { if (it==SCHEDULED || it==READY) progress = 1.0 }
            }
         }

         ActionData.UiResult(
            "Adding songs to library",
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

                     fun loadContent(block: () -> Node) {
                        anim(500.millis) {
                           content.children[0].opacity = it*it
                           content.children[1].opacity = it*it
                        }.apply {
                           playAgainIfFinished = false
                        }.playCloseDoOpen {
                           content.children[1].asIs<Pane>().lay += block()
                        }
                     }

                     runVT {
                        val nonWritable = if (conf.makeWritable.value) songs.filter { it.isFileBased() && !it.getFile()!!.setWritable(true) } else listOf()
                        if (nonWritable.isNotEmpty()) Try.error(nonWritable)
                        else Try.ok(task.runAndGet())
                     }.withAppProgress(task.title).withProgress(actionPane.actionProgress).onDone(FX) {
                        when (val r = it.toTry().flatten()) {
                           is Try.Ok<AddSongsToLibResult> -> {
                              if (conf.editInTagger.value) {
                                 val tagger = runBlocking { APP.widgetManager.factories.getFactory(SONG_TAGGER.id).orNone().create() }
                                 val songsToRead = if (conf.editOnlyAdded.value) r.value.converted else r.value.all
                                 loadContent {
                                    tagger.load().apply {
                                       tagger.controller.asIf<SongReader>()?.read(songsToRead)
                                    }
                                 }
                              }
                              if (conf.enqueue.value && r.value.all.isNotEmpty()) {
                                 APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { it.playlist.addItems(r.value.all) }
                              }
                           }
                           else -> actionPane.show(r)
                        }
                     }
                  }.apply {
                     disableProperty() syncFrom executed
                  }
               }
               lay += stackPane()
            }
         )
      }

data class SongToAdd(val uri: URI, val isInDb: Boolean)

/** Denotes action pane data representing multiple files for browse actions. */
class MultipleFiles(val files: Set<File>) {

   fun browseEach() = files.forEach { it.browse() }

   fun browseEachLocation() = files.map { if (it.isFile) it.parentDirOrRoot else it }.distinct().forEach { it.browse() }

   fun browseCommonRoot() {
      getCommonRoot(files)?.browse()
   }

}