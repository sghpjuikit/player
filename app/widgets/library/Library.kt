package library

import javafx.scene.control.SelectionMode.MULTIPLE
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
import sp.it.pl.audio.tagging.Metadata.Field.RATING
import sp.it.pl.audio.tagging.Metadata.Field.TITLE
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.rating.RatingCellFactory
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.objects.table.ImprovedTable.PojoV
import sp.it.pl.ui.objects.table.buildFieldedCell
import sp.it.pl.ui.objects.tablerow.SpitTableRow
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.LIBRARY
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.pl.main.audioExtensionFilter
import sp.it.pl.main.emScaled
import sp.it.pl.main.setSongsAndFiles
import sp.it.pl.main.showConfirmation
import sp.it.pl.ui.nodeinfo.ListLikeViewInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.async.runNew
import sp.it.util.collections.materialize
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.cn
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
import sp.it.pl.ui.objects.table.TableColumnInfo as ColumnState
import mu.KLogging
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.main.Css.Pseudoclasses.played
import sp.it.pl.main.HelpEntries
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.Widgets.SONG_TABLE_NAME
import sp.it.pl.main.installDrag
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Sort
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.collections.setTo
import sp.it.util.conf.cOr
import sp.it.util.conf.defInherit
import sp.it.util.functional.asIf
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.show
import sp.it.util.ui.tableColumn
import sp.it.util.units.version
import sp.it.util.units.year

class Library(widget: Widget): SimpleController(widget), SongReader {

   private val table = FilteredTable(Metadata::class.java, Metadata.EMPTY.getMainField())
   private val outputSelected = io.o.create<Metadata?>("Selected", null)
   private val inputItems = io.i.create<List<Metadata>>("To display", listOf()) { setItems(it) }

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
   private var lastAddFilesLocation by cn<File>(APP.location.user).noUi()
      .def(name = "Last add songs browse location", editable = EditMode.APP)
   private var lastAddDirLocation by cn<File>(APP.location.user).only(DIRECTORY).noUi()
      .def(name = "Last add directory browse location", editable = EditMode.APP)

   init {
      root.prefSize = 850.emScaled x 600.emScaled
      root.lay += table.root

      // table properties
      table.selectionModel.selectionMode = MULTIPLE
      table.search.setColumn(TITLE)
      table.items_info.textFactory = { all, list -> DEFAULT_TEXT_FACTORY(all, list) + " - " + list.sumOf { it.getLengthInMs() }.millis.toHMSMs() }

      // add menu items
      table.menuAdd.dsl {
         item("Add files") { addFiles() }
         item("Add directory") { addDirectory() }
      }
      table.menuRemove.dsl {
         item("Remove selected songs from library (${keys(DELETE)})") { removeSongs(table.selectedItems) }
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
         tableColumn<Metadata, Any?>(field.name()) {
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
         SpitTableRow<Metadata>().apply {
            onLeftDoubleClick { r, _ -> PlaylistManager.use { it.setAndPlayFrom(table.items, r.index) } }
            onRightSingleClick { r, e ->
               if (!r.isSelected) t.selectionModel.clearAndSelect(r.index)
               ValueContextMenu<MetadataGroup>().apply {
                  setItemsFor(MetadataGroup.groupOfUnrelated(table.selectedItemsCopy))
                  show(table, e)
               }
            }
            styleRuleAdd(pseudoclass(played)) { APP.audio.playingSong.value.same(it) }
         }
      }
      APP.audio.playingSong.onUpdate { _, _ -> table.updateStyleRules() } on onClose

      table.defaultColumnInfo   // trigger menu initialization
      table.columnState = widget.properties["columns"].asIf<String>()?.let { ColumnState.fromString(it).orNull() } ?: table.defaultColumnInfo

      table.onEventDown(KEY_PRESSED, ENTER, false) {
         if (!it.isConsumed) {
            if (!table.selectionModel.isEmpty) {
               PlaylistManager.use { it.setAndPlayFrom(table.items, table.selectionModel.selectedIndex) }
               it.consume()
            }
         }
      }
      table.onEventDown(KEY_PRESSED, DELETE, false) {
         if (!it.isConsumed) {
            if (!table.selectionModel.isEmpty) {
               removeSongs(table.selectedItems)
               it.consume()
            }
         }
      }

      // drag % drop
      table.onEventDown(DRAG_DETECTED, PRIMARY, false) {
         if (!table.selectedItems.isEmpty() && table.isRowFull(table.getRowS(it.sceneX, it.sceneY))) {
            table.startDragAndDrop(*ANY).setSongsAndFiles(table.selectedItemsCopy)
            it.consume()
         }
      }
      // drag % drop - prevent parent
      root.installDrag(IconFA.PLUS, "Add to library", { true }, { true }, {})

      table.consumeScrolling()

      // sync outputs
      table.selectionModel.selectedItemProperty() sync { outputSelected.value = it } on onClose
      root.sync1IfInScene { inputItems.bindDefaultIf1stLoad(APP.db.songs) } on onClose

      // sync library comparator
      table.itemsComparator sync {
         val sorts = table.sortOrder.mapNotNull { c -> c.userData?.asIs<Metadata.Field<*>>()?.net { Sort.of(c) to it } }
         APP.audio.songOrderBys setTo sorts.map { it.second }
         APP.audio.songOrderSorts setTo sorts.map { it.first }
      } on onClose

   }

   override fun getConfigs(): Collection<Config<Any?>> {
      widget.properties["columns"] = table.columnState.toString()
      return super.getConfigs()
   }

   override fun read(songs: List<Song>) {
      inputItems.value = songs.map { it.toMeta() }
   }

   fun setItems(items: List<Metadata>?) {
      if (items==null) return
      table.setItemsRaw(items)
   }

   private fun addDirectory() = chooseFile("Add folder to library", DIRECTORY, lastAddDirLocation, root.scene.window).ifOk {
      APP.ui.actionPane.orBuild.show(it)
      lastAddDirLocation = it.parentFile
   }

   private fun addFiles() = chooseFiles("Add files to library", lastAddFilesLocation, root.scene.window, audioExtensionFilter()).ifOk {
      APP.ui.actionPane.orBuild.show(it)
      lastAddFilesLocation = getCommonRoot(it)
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

   companion object: WidgetCompanion, KLogging() {
      override val name = SONG_TABLE_NAME
      override val description = "Table of songs"
      override val descriptionLong = "$description. Allows access to song database."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(LIBRARY)
      override val summaryActions = HelpEntries.Table + listOf(
         Entry("Table row", "Plays item", "2x${PRIMARY.nameUi}"),
      )
   }

}