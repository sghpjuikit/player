package libraryView

import javafx.geometry.NodeOrientation
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.Menu
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.TransferMode.ANY
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import javafx.stage.WindowEvent.WINDOW_SHOWING
import javafx.util.Callback
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.CATEGORY
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.MetadataGroup.Companion.ungroup
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.AVG_RATING
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.VALUE
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.W_RATING
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.LIBRARY
import sp.it.pl.layout.widget.Widget.Info
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.pl.main.Widgets
import sp.it.pl.main.emScaled
import sp.it.pl.main.isPlaying
import sp.it.pl.main.setSongsAndFiles
import sp.it.pl.main.showConfirmation
import sp.it.pl.ui.itemnode.FieldedPredicateItemNode.PredicateData
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem.buildSingleSelectionMenu
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.rating.RatingCellFactory
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.objects.table.ImprovedTable.PojoV
import sp.it.pl.ui.objects.table.buildFieldedCell
import sp.it.pl.ui.objects.tablerow.ImprovedTableRow
import sp.it.util.access.OrV
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.runIO
import sp.it.util.async.runNew
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.noUi
import sp.it.util.conf.values
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.ifNull
import sp.it.util.functional.invoke
import sp.it.util.functional.orNull
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncTo
import sp.it.util.text.pluralUnit
import sp.it.util.type.isSubclassOf
import sp.it.util.type.rawJ
import sp.it.util.ui.dsl
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.x
import java.util.function.Supplier
import kotlin.streams.toList
import sp.it.pl.audio.tagging.Metadata.Field as MField
import sp.it.pl.audio.tagging.MetadataGroup.Field as MgField
import sp.it.pl.ui.objects.table.TableColumnInfo as ColumnState

private typealias Metadatas = List<Metadata>
private typealias MetadataGroups = List<MetadataGroup>
private typealias CellFactory<T> = Callback<TableColumn<MetadataGroup, T>, TableCell<MetadataGroup, T>>

@Info(
   author = "Martin Polakovic",
   name = Widgets.SONG_GROUP_TABLE_NAME,
   description = "Provides database filtering.",
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
      "    Click column + SHIFT : Sorts by multiple columns\n",
   version = "0.9.0",
   year = "2015",
   group = LIBRARY
)
class LibraryView(widget: Widget): SimpleController(widget) {

   private val table = FilteredTable(MetadataGroup::class.java, VALUE)
   private val inputItems = io.i.create("To display", listOf<Metadata>()) { setItems(it) }
   private val outputSelectedGroup = io.o.create("Selected", listOf<MetadataGroup>())
   private val outputSelectedSongs = io.io.mapped(outputSelectedGroup, "As Songs") { filterList(inputItems.value, true) }

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
   val fieldFilter by cv<MField<*>>(CATEGORY).values(MField.all.filter { it.isTypeStringRepresentable() })
      .def(name = "Field", info = "Field by which the table groups the songs") attach {
         applyData()
      }

   // restoring selection if table items change, we want to preserve as many selected items as possible - when selection
   // changes, we select all items (previously selected) that are still in the table
   private var selIgnore = false
   private var selOld = setOf<Any?>()
   // restoring selection from previous session, we serialize string representation and try to restore when application runs again we restore only once
   private var selLast by c("null").noUi().def(name = "Last selected", editable = EditMode.APP)
   private var selLastRestored = false

   init {
      root.prefSize = 600.emScaled x 600.emScaled
      root.lay += table.root

      table.selectionModel.selectionMode = MULTIPLE
      table.search.setColumn(VALUE)
      tableOrient syncTo table.nodeOrientationProperty() on onClose
      tableZeropad syncTo table.zeropadIndex on onClose
      tableOrigIndex syncTo table.showOriginalIndex on onClose
      tableShowHeader syncTo table.headerVisible on onClose
      tableShowFooter syncTo table.footerVisible on onClose

      // set up table columns
      table.setKeyNameColMapper { name -> if (ColumnField.INDEX.name()==name) name else MgField.valueOf(name).name() }
      table.setColumnFactory { f ->
         if (f is MgField<*>) {
            val mf = fieldFilter.value
            TableColumn<MetadataGroup, Any>(f.toString(mf)).apply {
               cellValueFactory = Callback { it.value?.let { PojoV(f.getOf(it)) } }
               cellFactory = when (f) {
                  AVG_RATING -> RatingCellFactory.asIs()
                  W_RATING -> CellFactory<Double?> {
                     object: TableCell<MetadataGroup, Double?>() {
                        init {
                           alignment = CENTER_RIGHT
                        }

                        override fun updateItem(item: Double?, empty: Boolean) {
                           super.updateItem(item, empty)
                           text = if (empty) null else String.format("%.2f", item)
                        }
                     }
                  }.asIs()
                  else -> CellFactory {
                     f.buildFieldedCell().apply {
                        alignment = if (f.getMFType(mf).isSubclassOf<String>()) CENTER_LEFT else CENTER_RIGHT
                     }
                  }
               }
            }
         } else {
            TableColumn<MetadataGroup, Any>(f.name()).apply {
               cellValueFactory = Callback { it.value?.let { PojoV(f.getOf(it)) } }
               cellFactory = Callback { f.buildFieldedCell() }
            }
         }
      }
      table.rowFactory = Callback { t ->
         ImprovedTableRow<MetadataGroup>().apply {
            styleRuleAdd(pcPlaying) { it.isPlaying() }
            onLeftDoubleClick { _, _ -> playSelected() }
            onRightSingleClick { row, e ->
               // prep selection for context menu
               if (!row.isSelected)
                  t.selectionModel.clearAndSelect(row.index)

               t.selectionModel.selectedItems.takeIf { it.size==1 }?.first()
                  .ifNotNull {
                     contextMenuInstance.setItemsFor(it)
                     contextMenuInstance.show(table, e)
                  }
                  .ifNull {
                     contextMenuInstance.setItemsFor(MetadataGroup.groupOfUnrelated(filerSortInputList()))
                     contextMenuInstance.show(table, e)
                  }
            }
         }
      }
      APP.audio.playingSong.onUpdate { _, _ -> table.updateStyleRules() } on onClose

      table.defaultColumnInfo   // trigger menu initialization
      table.columnState = widget.properties.getS("columns")?.let { ColumnState.fromString(it).orNull() } ?: table.defaultColumnInfo

      // column context menu - add group by menu
      val fieldMenu = Menu("Group by")
      table.columnMenu.items.add(fieldMenu)
      table.columnMenu.onEventDown(WINDOW_HIDDEN) { fieldMenu.items.clear() }
      table.columnMenu.onEventDown(WINDOW_SHOWING) {
         fieldMenu.items setTo buildSingleSelectionMenu(
            MField.all,
            fieldFilter.value,
            { it.name() },
            { fieldFilter.setValue(it) }
         )
      }

      // add menu items
      table.menuRemove.dsl {
         item("Remove songs in selected groups from library") { removeSongs(ungroup(table.selectedItems)) }
         item("Remove songs in all shown groups from library") { removeSongs(ungroup(table.items)) }
         item("Remove all songs from library") { APP.db.removeAllSongs() }
         item("Remove missing songs from library") {
            val task = Song.removeMissingFromLibTask()
            runNew(task)
            AppProgress.start(task)
         }
      }

      table.onEventDown(KEY_PRESSED, ENTER) {
         if (!it.isConsumed) {
            playSelected()
         }
      }
      table.onEventDown(KEY_PRESSED, DELETE) {
         if (!it.isConsumed) {
            APP.db.removeSongs(table.selectedItems.flatMap { it.grouped })
         }
      }
      table.onEventDown(DRAG_DETECTED, PRIMARY, false) {
         if (!table.selectedItems.isEmpty() && table.isRowFull(table.getRowS(it.sceneX, it.sceneY))) {
            table.startDragAndDrop(*ANY).setSongsAndFiles(filerSortInputList())
            it.consume()
         }
      }

      table.consumeScrolling()

      // resizing
      table.columnResizePolicy = Callback { resize ->
         UNCONSTRAINED_RESIZE_POLICY(resize).apply {
            val t = table
            // resize index column
            t.getColumn(ColumnField.INDEX).ifPresent { it.setPrefWidth(t.computeIndexColumnWidth()) }
            // resize main column to span remaining space
            t.getColumn(VALUE).ifPresent { c ->
               val sumW = t.columns.asSequence().filter { it.isVisible }.sumByDouble { it.width }
               val sbW = t.vScrollbarWidth
               c.setPrefWidth(t.width - (sbW + sumW - c.width))
            }
         }
      }

      // sync outputs
      val selectedItemsReducer = EventReducer.toLast<Void>(100.0) {
         outputSelectedGroup.value = table.selectedItemsCopy
      }
      table.selectedItems.onChange { if (!selIgnore) selectedItemsReducer.push(null) } on onClose
      table.selectionModel.selectedItemProperty() attach { selLast = it?.getValueS("") ?: "null" } on onClose
      root.sync1IfInScene { inputItems.bindDefaultIf1stLoad(APP.db.songs.o) } on onClose

      applyData(false)
   }

   override fun getConfigs(): Collection<Config<Any?>> {
      widget.properties["columns"] = table.columnState.toString()
      return super.getConfigs()
   }

   private fun applyData(refreshItems: Boolean = true) {
      // rebuild value column
      table.getColumn(VALUE).ifPresent {
         val t = table.getColumnFactory<Any>().call(VALUE)
         it.text = t.text
         it.cellValueFactory = t.cellValueFactory
         it.cellFactory = t.cellFactory
         table.refreshColumn(it)
      }

      // update filters
      val f = fieldFilter.value
      table.filterPane.inconsistentState = true
      table.filterPane.prefTypeSupplier = Supplier { PredicateData.ofField(VALUE) }
      table.filterPane.data = MgField.all.map { PredicateData(it.toString(f), it.getMFType(f).rawJ, it.asIs<ObjectField<MetadataGroup, Any?>>()) }
      table.filterPane.clear()
      if (refreshItems) setItems(inputItems.value)
   }

   /** Populates metadata groups to table from metadata list. */
   private fun setItems(list: Metadatas?) {
      if (list==null) return

      val f = fieldFilter.value
      runIO {
         listOf(MetadataGroup.groupOf(f, list)) + MetadataGroup.groupsOf(f, list).toList()
      } ui {
         if (it.isNotEmpty()) {
            selectionStore()
            table.setItemsRaw(it)
            selectionReStore()
         }
      }
   }

   private fun filterList(list: Metadatas?, orAll: Boolean): Metadatas = when {
      list==null -> listOf()
      list.isEmpty() -> listOf()
      else -> {
         val mgs = table.getSelectedOrAllItems(orAll).toList()
         when {
            mgs.any { it.isAll } -> list    // selecting "All" row is equivalent to selecting all rows
            else -> mgs.flatMap { it.grouped }
         }
      }
   }

   private fun filerSortInputList() = filterList(inputItems.value, false).sortedWith(APP.db.libraryComparator.value)

   private fun playSelected() = play(filerSortInputList())

   private fun play(items: Metadatas) {
      if (items.isNotEmpty())
         PlaylistManager.use { it.setNplay(items) }
   }

   private fun selectionStore() {
      selOld = table.selectedItems.mapTo(HashSet()) { it.value }
      selIgnore = true
   }

   private fun selectionReStore() {
      if (table.items.isEmpty()) return

      // restore last selected from previous session, runs once
      if (!selLastRestored) {
         selIgnore = false
         selLastRestored = true
         table.items.forEachIndexed { i, mg ->
            if (mg.getValueS("")==selLast) {
               table.selectionModel.select(i)
               table.scrollTo(i)
            }
         }
         return
      }

      // restore selection
      table.items.forEachIndexed { i, mg ->
         if (mg.value in selOld) {
            table.selectionModel.select(i)
         }
      }

      selIgnore = false
      outputSelectedGroup.value = table.selectedItemsCopy
   }

   fun removeSongs(songs: Set<Song>) {
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