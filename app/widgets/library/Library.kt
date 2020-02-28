package library

import javafx.geometry.NodeOrientation
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.TransferMode.ANY
import javafx.util.Callback
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.RATING
import sp.it.pl.audio.tagging.Metadata.Field.Companion.TITLE
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.gui.nodeinfo.TableInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.pl.gui.objects.contextmenu.ValueContextMenu
import sp.it.pl.gui.objects.rating.RatingCellFactory
import sp.it.pl.gui.objects.table.FilteredTable
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV
import sp.it.pl.gui.objects.table.buildFieldedCell
import sp.it.pl.gui.objects.tablerow.ImprovedTableRow
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.LIBRARY
import sp.it.pl.layout.widget.Widget.Info
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.pl.main.Widgets
import sp.it.pl.main.audioExtensionFilter
import sp.it.pl.main.emScaled
import sp.it.pl.main.setSongsAndFiles
import sp.it.pl.main.showConfirmation
import sp.it.util.access.OrV
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.async.runNew
import sp.it.util.collections.materialize
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.Util.getCommonRoot
import sp.it.util.functional.asIs
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncTo
import sp.it.util.system.chooseFile
import sp.it.util.system.chooseFiles
import sp.it.util.text.pluralUnit
import sp.it.util.ui.dsl
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.toHMSMs
import java.io.File
import sp.it.pl.gui.objects.table.TableColumnInfo as ColumnState

@Info(
   author = "Martin Polakovic",
   name = Widgets.SONG_TABLE_NAME,
   description = "Provides access to database.",
   howto = "Available actions:\n" +
      "    Song left click : Selects item\n" +
      "    Song right click : Opens context menu\n" +
      "    Song double click : Plays item\n" +
      "    Type : search & filter\n" +
      "    Press ENTER : Plays item\n" +
      "    Press ESC : Clear selection & filter\n" +
      "    Scroll : Scroll table vertically\n" +
      "    Scroll + SHIFT : Scroll table horizontally\n" +
      "    Column drag : swap columns\n" +
      "    Column right click: show column menu\n" +
      "    Click column : Sort - ascending | descending | none\n" +
      "    Click column + SHIFT : Sorts by multiple columns\n" +
      "    Menu bar : Opens additional actions\n",
   version = "1.0.0",
   year = "2015",
   group = LIBRARY
)
class Library(widget: Widget): SimpleController(widget), SongReader {

   private val table = FilteredTable(Metadata::class.java, Metadata.EMPTY.getMainField())
   private val outputSelected = io.o.create<Metadata>("Selected", null)
   private val inputItems = io.i.create<List<Metadata>>("To display", listOf()) { setItems(it) }

   val tableOrient by cv(NodeOrientation.INHERIT) { OrV(APP.ui.tableOrient) }
      .def(name = "Table orientation", info = "Orientation of the table.")
   val tableZeropad by cv(true) { OrV(APP.ui.tableZeropad) }
      .def(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
   val tableOrigIndex by cv(true) { OrV(APP.ui.tableOrigIndex) }
      .def(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
   val tableShowHeader by cv(true) { OrV(APP.ui.tableShowHeader) }
      .def(name = "Show table header", info = "Show table header with columns.")
   val tableShowFooter by cv(true) { OrV(APP.ui.tableShowFooter) }
      .def(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menu bar and table content information.")
   private var lastAddFilesLocation by cn<File>(APP.location.user).noUi()
      .def(name = "Last add songs browse location", editable = EditMode.APP)
   private var lastAddDirLocation by cn<File>(APP.location.user).only(DIRECTORY).noUi()
      .def(name = "Last add directory browse location", editable = EditMode.APP)

   init {
      root.prefSize = 850.emScaled x 600.emScaled
      root.consumeScrolling()
      root.lay += table.root

      // table properties
      table.selectionModel.selectionMode = MULTIPLE
      table.search.setColumn(TITLE)
      table.nodeOrientationProperty() syncFrom tableOrient on onClose
      table.zeropadIndex syncFrom tableZeropad on onClose
      table.showOriginalIndex syncFrom tableOrigIndex on onClose
      table.headerVisible syncFrom tableShowHeader on onClose
      table.footerVisible syncFrom tableShowFooter on onClose
      table.items_info.textFactory = { all, list ->
         DEFAULT_TEXT_FACTORY(all, list) + " - " + list.sumByDouble { it.getLengthInMs() }.millis.toHMSMs()
      }

      // add more menu items
      table.menuAdd.dsl {
         item("Add files") { addFiles() }
         item("Add directory") { addDirectory() }
      }
      table.menuRemove.dsl {
         item("Remove selected songs from library") { removeSongs(table.selectedItems) }
         item("Remove all shown songs from library") { removeSongs(table.items) }
         item("Remove all songs from library") { APP.db.removeAllSongs() }
         item("Remove missing songs from library") {
            val task = Song.removeMissingFromLibTask()
            runNew(task)
            AppProgress.start(task)
         }
      }

      // set up table columns
      table.setColumnFactory { field ->
         TableColumn<Metadata, Any?>(field.name()).apply {
            cellFactory = when (field) {
               RATING -> RatingCellFactory.asIs()
               else -> Callback { field.buildFieldedCell() }
            }
            cellValueFactory = Callback { it.value?.net { PojoV(field.getOf(it)) } }
         }
      }

      // column resizing
      table.columnResizePolicy = Callback {
         UNCONSTRAINED_RESIZE_POLICY(it).apply {
            table.getColumn(ColumnField.INDEX).orNull()?.prefWidth = table.computeIndexColumnWidth()
         }
      }

      // row behavior
      table.rowFactory = Callback { t ->
         ImprovedTableRow<Metadata>().apply {
            onLeftDoubleClick { r, _ -> PlaylistManager.use { it.setNplayFrom(table.items, r.index) } }
            onRightSingleClick { r, e ->
               // prep selection for context menu
               if (!r.isSelected)
                  t.selectionModel.clearAndSelect(r.index)

               contextMenuInstance.setItemsFor(MetadataGroup.groupOfUnrelated(table.selectedItemsCopy))
               contextMenuInstance.show(table, e)
            }
            styleRuleAdd(pcPlaying) { APP.audio.playingSong.value.same(it) }
         }
      }
      APP.audio.playingSong.onUpdate { _, _ -> table.updateStyleRules() } on onClose

      table.defaultColumnInfo   // trigger menu initialization
      table.columnState = widget.properties.getS("columns")?.let { ColumnState.fromString(it).orNull() } ?: table.defaultColumnInfo

      table.onEventDown(KEY_PRESSED, ENTER, false) {
         if (!table.selectionModel.isEmpty) {
            PlaylistManager.use { it.setNplayFrom(table.items, table.selectionModel.selectedIndex) }
            it.consume()
         }
      }
      table.onEventDown(KEY_PRESSED, DELETE, false) {
         if (!table.selectionModel.isEmpty) {
            removeSongs(table.selectedItems)
            it.consume()
         }
      }

      table.onEventDown(DRAG_DETECTED, PRIMARY, false) {
         if (!table.selectedItems.isEmpty() && table.isRowFull(table.getRowS(it.sceneX, it.sceneY))) {
            table.startDragAndDrop(*ANY).setSongsAndFiles(table.selectedItemsCopy)
            it.consume()
         }
      }

      // sync outputs
      table.selectionModel.selectedItemProperty() sync { outputSelected.value = it } on onClose
      root.sync1IfInScene { inputItems.bindDefaultIf1stLoad(APP.db.songs.o) } on onClose

      // sync library comparator
      table.itemsComparator syncTo APP.db.libraryComparator on onClose

   }

   override fun getConfigs(): Collection<Config<Any?>> {
      widget.properties["columns"] = table.columnState.toString()
      return super.getConfigs()
   }

   override fun read(songs: List<Song>) {
      table.setItemsRaw(songs.map { it.toMeta() })
   }

   fun setItems(items: List<Metadata>?) {
      if (items==null) return
      table.setItemsRaw(items)
   }

   private fun addDirectory() {
      chooseFile("Add folder to library", DIRECTORY, lastAddDirLocation, root.scene.window).ifOk {
         APP.ui.actionPane.orBuild.show(it)
         lastAddDirLocation = it.parentFile
      }
   }

   private fun addFiles() {
      chooseFiles("Add files to library", lastAddFilesLocation, root.scene.window, audioExtensionFilter()).ifOk {
         APP.ui.actionPane.orBuild.show(it)
         lastAddFilesLocation = getCommonRoot(it)
      }
   }

   fun removeSongs(songs: List<Song>) {
      if (songs.isEmpty()) {
         showConfirmation("No songs to remove - selection is empty") {}
      } else {
         val songsM = songs.materialize()
         showConfirmation("Are you sure you want to remove ${"song".pluralUnit(songs.size)} from library?") {
            APP.db.removeSongs(songsM)
         }
      }
   }

   companion object {
      private val pcPlaying = pseudoclass("played")
      private val contextMenuInstance by lazy { ValueContextMenu<MetadataGroup>() }
   }

}