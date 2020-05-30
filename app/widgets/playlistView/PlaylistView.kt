package playlistView

import javafx.geometry.NodeOrientation.INHERIT
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.stage.FileChooser
import mu.KLogging
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.PlaylistSong
import sp.it.pl.audio.playlist.PlaylistSong.Field
import sp.it.pl.audio.playlist.writePlaylist
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.PLAYLIST
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.main.Widgets.PLAYBACK_NAME
import sp.it.pl.main.emScaled
import sp.it.pl.main.toUi
import sp.it.pl.ui.nodeinfo.TableInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.pl.ui.objects.table.PlaylistTable
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Sort
import sp.it.util.access.OrV
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
import sp.it.util.reactive.syncFrom
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

class PlaylistView(widget: Widget): SimpleController(widget), PlaylistFeature {

   override val playlist = computeInitialPlaylist(widget.id)
   private val table = PlaylistTable(playlist)
   private var outputSelected = io.o.create<PlaylistSong>("Selected", null)
   private var outputPlaying = io.o.create<PlaylistSong>("Playing", null)

   val tableOrient by cv(INHERIT) { OrV(APP.ui.tableOrient) }
      .def(name = "Table orientation", info = "Orientation of the table.")
   val tableZeropad by cv(true) { OrV(APP.ui.tableZeropad) }
      .def(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
   val tableOrigIndex by cv(true) { OrV(APP.ui.tableOrigIndex) }
      .def(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
   val tableShowHeader by cv(true) { OrV(APP.ui.tableShowHeader) }
      .def(name = "Show table header", info = "Show table header with columns.")
   val tableShowFooter by cv(true) { OrV(APP.ui.tableShowFooter) }
      .def(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menu bar and table content information.")
   val scrollToPlaying by cv(true)
      .def(name = "Scroll to playing", info = "Scroll table to playing item when it changes.")
   val playVisible by cv(false)
      .def(name = "Play displayed only", info = "Only displayed items will be played when filter is active.")
   var lastSavePlaylistLocation by cn<File>(APP.location.user).only(DIRECTORY)
      .def(name = "Default browse location", info = "Opens this location for file dialogs.", editable = EditMode.APP)

   init {
      root.prefSize = 450.emScaled x 600.emScaled
      root.consumeScrolling()

      playlist.playingSong sync { outputPlaying.value = it } on onClose
      playlist.duration attach { table.items_info.updateText() } on onClose
      APP.audio.onSongRefresh { ms ->
         outputPlaying.value?.let { ms.ifHasK(it.uri) { outputPlaying.value = it.toPlaylist() } }
         outputSelected.value?.let { ms.ifHasK(it.uri) { outputSelected.value = it.toPlaylist() } }
      } on onClose

      playVisible sync { pv ->
         playlist.setTransformation(
            if (pv) { _: List<PlaylistSong> -> table.items.materialize().toList() }
            else { it: List<PlaylistSong> -> it.asSequence().sortedWith(table.itemsComparator.value).toList() }
         )
      } on onClose

      table.search.setColumn(Field.NAME)
      table.selectionModel.selectionMode = MULTIPLE
      table.items_info.textFactory = { all, list ->
         DEFAULT_TEXT_FACTORY(all, list) + " - " + list.sumByDouble { it.timeMs }.millis.toHMSMs()
      }
      table.nodeOrientationProperty() syncFrom tableOrient on onClose
      table.zeropadIndex syncFrom tableZeropad on onClose
      table.showOriginalIndex syncFrom tableOrigIndex on onClose
      table.headerVisible syncFrom tableShowHeader on onClose
      table.footerVisible syncFrom tableShowFooter on onClose
      table.scrollToPlaying syncFrom scrollToPlaying on onClose
      table.defaultColumnInfo   // trigger menu initialization
      table.columnState = widget.properties.getS("columns")?.let { ColumnState.fromString(it).orNull() }
         ?: table.defaultColumnInfo

      table.filterPane.buttonAdjuster.value = { i ->
         i.onClickDo { playVisible.toggle() }
         playVisible sync {
            i.icon(if (it) IconFA.FILTER else IconMD.FILTER_OUTLINE)
            i.tooltip(
               if (it) "Disable filter for playback. Causes the playback to ignore the filter."
               else "Enable filter for playback. Causes the playback to play only displayed items."
            )
         }
      }
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
         item("Remove selected") { playlist -= table.selectedItems }
         item("Remove not selected") { playlist.retainAll(table.selectedItems) }
         item("Remove unplayable") { playlist.removeUnplayable() }
         item("Remove duplicates") { playlist.removeDuplicates() }
         item("Remove all") { playlist.clear() }
      }
      table.menuOrder.dsl {
         menu("Order by") {
            items(Field.all.asSequence(), { it.name() }) { table.sortBy(it) }
         }
         item("Order reverse") { playlist.reverse() }
         item("Order randomly") { playlist.randomize() }
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
                  writePlaylist(table.selectedOrAllItemsCopy, file.name, file.parentDirOrRoot)
               }
            }
         }
      }
   }

   override fun getConfigs(): Collection<Config<Any?>> {
      widget.properties["columns"] = table.columnState.toString()
      return super.getConfigs()
   }

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
         addPlaylistSongs(dp, 0)
         updatePlayingItem(dp.indexOfPlaying())
         PlaylistManager.playlists += this
         if (wasActive) PlaylistManager.active = id
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = PLAYBACK_NAME
      override val description = "Contains playlist table"
      override val descriptionLong = "$description. Highlights playing and unplayable songs"
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         Entry("Table", "Filter", keys("CTRL+F")),
         Entry("Table", "Filter (cancel)", KeyCode.ESCAPE.nameUi),
         Entry("Table", "Filter (clear)", KeyCode.ESCAPE.nameUi),
         Entry("Table", "Search", "Type text"),
         Entry("Table", "Search (cancel)", KeyCode.ESCAPE.nameUi),
         Entry("Table", "Selection (cancel)", KeyCode.ESCAPE.nameUi),
         Entry("Table", "Scroll table vertically", keys("Scroll")),
         Entry("Table", "Scroll table horizontally", keys("Scroll+SHIFT")),
         Entry("Table columns", "Show column context menu", SECONDARY.toUi()),
         Entry("Table columns", "Swap columns", "Column drag"),
         Entry("Table columns", "Sort - ${Sort.ASCENDING.toUi()} | ${Sort.DESCENDING.toUi()} | ${Sort.NONE.toUi()}", PRIMARY.nameUi),
         Entry("Table columns", "Sorts by multiple columns", keys("SHIFT+${PRIMARY.nameUi})")),
         Entry("Table row", "Selects item", PRIMARY.nameUi),
         Entry("Table row", "Show context menu", SECONDARY.nameUi),
         Entry("Table row", "Plays item", "2x${PRIMARY.nameUi}"),
         Entry("Table row", "Move song within playlist", keys("Song drag+CTRL")),
         Entry("Table row", "Add songs after row", "Drag & drop songs"),
         Entry("Footer", "Opens additional action menus", "Menu bar")
      )
      override val group = PLAYLIST
   }
}