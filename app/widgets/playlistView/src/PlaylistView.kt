package playlistView

import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.input.MouseButton.PRIMARY
import javafx.stage.FileChooser
import mu.KLogging
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.PlaylistSong.Field
import sp.it.pl.audio.playlist.writeM3uPlaylist
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.layout.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Widgets.PLAYLIST_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.ui.nodeinfo.ListLikeViewInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.pl.ui.objects.table.PlaylistTable
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.toggle
import sp.it.util.async.runNew
import sp.it.util.collections.materialize
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.parentDirOrRoot
import sp.it.util.functional.asIf
import sp.it.util.functional.orNull
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.sync
import sp.it.util.system.saveFile
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.dsl
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.toHMSMs
import sp.it.util.units.version
import sp.it.util.units.year
import java.io.File
import java.util.UUID
import sp.it.pl.ui.objects.table.TableColumnInfo as ColumnState
import java.util.function.Consumer
import sp.it.pl.audio.Song
import sp.it.pl.main.WidgetTags.AUDIO
import sp.it.pl.main.HelpEntries
import sp.it.pl.main.IconMA
import sp.it.pl.main.Key
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.conf.cOr
import sp.it.util.conf.defInherit
import sp.it.util.functional.invoke
import sp.it.util.reactive.sync1If
import sp.it.util.units.NofX

class PlaylistView(widget: Widget): SimpleController(widget), PlaylistFeature {

   override val playlist = computeInitialPlaylist(widget.id)
   private val table = PlaylistTable(playlist)
   private var outputSelected = io.o.create<PlaylistSong?>("Selected", null)
   private var outputPlaying = io.o.create<PlaylistSong?>("Playing", null)

   val tableOrient by cOr(APP.ui::tableOrient, table.nodeOrientationProperty(), Inherit(), onClose)
      .defInherit(APP.ui::tableOrient)
   val tableZeropad by cOr(APP.ui::tableZeropad, table.zeropadIndex, Inherit(), onClose)
      .defInherit(APP.ui::tableZeropad)
   val tableOrigIndex by cOr(APP.ui::tableOrigIndex, table.showOriginalIndex,  Inherit(), onClose)
      .defInherit(APP.ui::tableOrigIndex)
   val tableShowHeader by cOr(APP.ui::tableShowHeader, table.headerVisible, Inherit(), onClose)
      .defInherit(APP.ui::tableShowHeader)
   val tableShowFooter by cOr(APP.ui::tableShowFooter, table.footerVisible, Inherit(), onClose)
      .defInherit(APP.ui::tableShowFooter)
   val scrollToPlaying by cv(table.scrollToPlaying)
      .def(name = "Scroll to playing", info = "Scroll table to playing item when it changes.")
   val playVisible by cv(false)
      .def(name = "Play displayed only", info = "Only displayed items will be played when filter is active.")
   var lastSavePlaylistLocation by cn<File>(APP.location.user).only(DIRECTORY)
      .def(name = "Default browse location", info = "Opens this location for file dialogs.", editable = EditMode.APP)

   init {
      root.prefSize = 450.emScaled x 600.emScaled

      playlist.playingSong sync { outputPlaying.value = it } on onClose
      playlist.duration attach { table.items_info.updateText() } on onClose
      APP.audio.onSongRefresh { ms ->
         outputPlaying.value?.let { ms.ifHasK(it.uri) { outputPlaying.value = it.toPlaylist() } }
         outputSelected.value?.let { ms.ifHasK(it.uri) { outputSelected.value = it.toPlaylist() } }
      } on onClose

      playVisible sync { pv ->
         playlist.setTransformation(
            object: Playlist.Transformer() {
               override fun transform(original: List<PlaylistSong>, then: Consumer<in List<PlaylistSong>>) {
                  table.itemsSorting.sync1If({ it==false }) {
                     then(transformNow(original))
                  }
               }
               override fun transformNow(original: List<PlaylistSong>): List<PlaylistSong> {
                  return if (pv) table.items.materialize()
                  else original.materialize().sortedWith(table.itemsComparator.value)
               }
               override fun index(song: Song): NofX = NofX(playlist.indexOfFirst { it.same(song) }, playlist.size)
            }
         )
      } on onClose

      table.search.setColumn(Field.NAME)
      table.selectionModel.selectionMode = MULTIPLE
      table.items_info.textFactory = { all, list ->
         DEFAULT_TEXT_FACTORY(all, list) + " - " + list.sumOf { it.timeMs }.millis.toHMSMs()
      }

      table.defaultColumnInfo   // trigger menu initialization
      table.columnState = widget.properties["columns"].asIf<String>()?.let { ColumnState.fromString(it).orNull() } ?: table.defaultColumnInfo

      table.filterPane.buttonAdjuster.value = { i ->
         i.onClickDo { playVisible.toggle() }
         playVisible sync {
            i.icon(it, IconFA.FILTER, IconMD.FILTER_OUTLINE)
            i.tooltip(
               if (it) "Disable filter for playback. Causes the playback to ignore the filter."
               else "Enable filter for playback. Causes the playback to play only displayed items."
            )
         }
      }
      table.consumeScrolling()
      onClose += table::dispose
      onClose += table.selectionModel.selectedItemProperty() attach {
         if (!table.movingItems)
            outputSelected.value = it
      }

      root.lay += table.root

      table.menuAdd.dsl {
         item("Add files") { PlaylistManager.chooseFilesToAdd() }
         item("Add directory") { PlaylistManager.chooseFolderToAdd() }
         item("Add URL") { PlaylistManager.chooseUrlToAdd() }
         item("Play files") { PlaylistManager.chooseFilesToPlay() }
         item("Play directory") { PlaylistManager.chooseFolderToPlay() }
         item("Play URL") { PlaylistManager.chooseUrlToPlay() }
         item("Duplicate selected (+)") { playlist.duplicateItemsByOne(table.selectedItems) }
         item("Duplicate selected (*)") { playlist.duplicateItemsAsGroup(table.selectedItems) }
      }
      table.menuRemove.dsl {
         item("Remove selected", keys = keys(Key.DELETE)) { playlist -= table.selectedItems }
         item("Retain selected") { playlist.retainAll(table.selectedItems) }
         item("Retain filtered & cancel filter") {
            playlist.retainAll(table.items.materialize())
            table.filterVisible.value = false
         }
         item("Remove unplayable") { playlist.removeUnplayable() }
         item("Remove duplicates") { playlist.removeDuplicates() }
         item("Remove all") { playlist.clear() }
      }
      table.menuExtra.dsl {
         item("Edit selected") { APP.widgetManager.widgets.use<SongReader>(NO_LAYOUT) { it.read(table.selectedItems) } }
         item("Save playlist") {
            saveFile(
               "Save playlist as...",
               lastSavePlaylistLocation ?: APP.audio.lastSavePlaylistLocation,
               "Playlist",
               root.scene.window,
               FileChooser.ExtensionFilter("m3u8", "m3u8")
            ).ifOk { file ->
               lastSavePlaylistLocation = file.parentDirOrRoot
               APP.audio.lastSavePlaylistLocation = file.parentDirOrRoot
               runNew {
                  writeM3uPlaylist(table.selectedOrAllItemsCopy, file.name, file.parentDirOrRoot)
               }
            }
         }
      }
   }

   override fun getConfigs(): Collection<Config<Any?>> {
      widget.properties["columns"] = table.columnState.toString()
      return super.getConfigs()
   }

   override fun focus() = table.requestFocus()

   private fun computeInitialPlaylist(id: UUID) = null
      ?: PlaylistManager.playlists[id]
      ?: findDanglingPlaylist()?.copyDangling()
      ?: Playlist(id).also { PlaylistManager.playlists += it }

   private fun findDanglingPlaylists(): List<Playlist> {
      val open = APP.widgetManager.widgets.findAll(OPEN).mapNotNull { it.controller.asIf<PlaylistFeature>()?.playlist }
      return PlaylistManager.playlists.filter { it !in open }
   }

   private fun findDanglingPlaylist(): Playlist? = findDanglingPlaylists().let {
      it.find { it.id==PlaylistManager.active } ?: it.firstOrNull()
   }

   private fun Playlist.copyDangling() = let { dp ->
      val wasActive = dp.id==PlaylistManager.active
      PlaylistManager.playlists -= dp
      Playlist(id).apply {
         PlaylistManager.playlists += this
         addPlaylistSongs(dp, 0)
         updatePlayingItem(dp.indexOfPlaying())
         if (wasActive) PlaylistManager.active = id
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = PLAYLIST_NAME
      override val description = "Playlist table controlling playback song order"
      override val descriptionLong = "$description. Highlights playing and unplayable songs"
      override val icon = IconMA.QUEUE_MUSIC
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(AUDIO)
      override val summaryActions = HelpEntries.Table + listOf(
         Entry("Table row", "Move song within playlist", keys("CTRL+Drag")),
         Entry("Table row", "Play item", "2x${PRIMARY.nameUi}"),
      )
   }
}