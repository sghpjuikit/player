package libraryView

import sp.it.pl.audio.tagging.Metadata.Field as MField
import sp.it.pl.audio.tagging.MetadataGroup.Field as MgField
import sp.it.pl.ui.objects.table.TableColumnInfo as ColumnState
import java.util.Comparator.comparing
import java.util.function.BiFunction
import java.util.function.Supplier
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.control.Menu
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.TransferMode.ANY
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import javafx.stage.WindowEvent.WINDOW_SHOWING
import javafx.util.Callback
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.CATEGORY
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.MetadataGroup.Companion.ungroup
import sp.it.pl.audio.tagging.MetadataGroup.Field.AVG_RATING
import sp.it.pl.audio.tagging.MetadataGroup.Field.VALUE
import sp.it.pl.audio.tagging.MetadataGroup.Field.W_RATING
import sp.it.pl.audio.tagging.removeMissingFromLibTask
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppProgress
import sp.it.pl.main.Css.Pseudoclasses.played
import sp.it.pl.main.Df
import sp.it.pl.main.HelpEntries
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconUN
import sp.it.pl.main.WidgetTags.LIBRARY
import sp.it.pl.main.Widgets.SONG_GROUP_TABLE_NAME
import sp.it.pl.main.contains
import sp.it.pl.main.emScaled
import sp.it.pl.main.installDrag
import sp.it.pl.main.isPlaying
import sp.it.pl.main.setSongsAndFiles
import sp.it.pl.main.showConfirmation
import sp.it.pl.ui.itemnode.FieldedPredicateItemNode.PredicateData
import sp.it.pl.ui.nodeinfo.ListLikeViewInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem.Companion.buildSingleSelectionMenu
import sp.it.pl.ui.objects.contextmenu.ValueContextMenu
import sp.it.pl.ui.objects.rating.RatingCellFactory
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.objects.table.ImprovedTable.PojoV
import sp.it.pl.ui.objects.table.buildFieldedCell
import sp.it.pl.ui.objects.tablerow.SpitTableRow
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.Sort.DESCENDING
import sp.it.util.access.OrV.OrValue.Initial.Inherit
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
import sp.it.util.conf.cOr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.defInherit
import sp.it.util.conf.noUi
import sp.it.util.conf.values
import sp.it.util.functional.Util.SAME
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.invoke
import sp.it.util.functional.nullsFirst
import sp.it.util.functional.nullsLast
import sp.it.util.functional.orNull
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.text.*
import sp.it.util.type.isSubclassOf
import sp.it.util.ui.dsl
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.show
import sp.it.util.ui.tableColumn
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.toHMSMs
import sp.it.util.units.version
import sp.it.util.units.year

private typealias Metadatas = List<Metadata>
private typealias MetadataGroups = List<MetadataGroup>
private typealias CellFactory<T> = Callback<TableColumn<MetadataGroup, T>, TableCell<MetadataGroup, T>>

class LibraryView(widget: Widget): SimpleController(widget) {

   private val table = FilteredTable(MetadataGroup::class.java, VALUE)
   private val inputItems = io.i.create("To display", listOf<Metadata>()) { setItems(it) }
   private val outputSelectedGroup = io.o.create("Selected", listOf<MetadataGroup>())
   private val outputSelectedSongs = io.io.mapped(outputSelectedGroup, "As Songs") { filterList(inputItems.value, true) }

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

      // table properties
      table.selectionModel.selectionMode = MULTIPLE
      table.search.setColumn(VALUE)
      table.itemsComparatorFieldFactory.value = BiFunction { field, sort ->
         when(field) {
            VALUE -> when {
               fieldFilter.value.typeGrouped.isSubclassOf<Comparable<*>>() -> comparing<MetadataGroup, Comparable<Any?>?> { if (it==null) null.asIs() else VALUE.getOfS(it, "").asIs() }.let { if (sort===DESCENDING) it.nullsFirst() else it.nullsLast() }
               else -> SAME
            }
            else -> field.comparator { c -> if (sort===DESCENDING) c.nullsFirst() else c.nullsLast() }
         }
      }
      table.items_info.textFactory = { all, list ->
         val allItem = list.find { it.isAll }
         DEFAULT_TEXT_FACTORY(all, list) + " " +
            "song".pluralUnit(allItem?.itemCount?.toInt() ?: list.sumOf { it.itemCount }.toInt())  + " - " +
            (allItem?.lengthInMs ?: list.sumOf { it.lengthInMs }).millis.toHMSMs()
      }

      // set up table columns
      table.setKeyNameColMapper { name -> if (ColumnField.INDEX.name()==name) name else MgField.valueOf(name).name() }
      table.setColumnFactory { f ->
         if (f is MgField<*>) {
            val mf = fieldFilter.value
            tableColumn<MetadataGroup, Any?>(f.toString(mf)) {
               styleClass += when (f) {
                  AVG_RATING, W_RATING -> "column-header-align-right"
                  else -> if (f.getMFType(mf).isSubclassOf<String>()) "column-header-align-left" else "column-header-align-right"
               }
               cellValueFactory = Callback { it.value?.let { PojoV(f.getOf(it)) } }
               cellFactory = when (f) {
                  AVG_RATING -> RatingCellFactory.asIs()
                  W_RATING ->
                     CellFactory<Double?> {
                        object: TableCell<MetadataGroup, Double?>() {
                           init {
                              alignment = CENTER_RIGHT
                           }

                           override fun updateItem(item: Double?, empty: Boolean) {
                              super.updateItem(item, empty)
                              text = if (empty) null else "%.2f".format(item)
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
            tableColumn<MetadataGroup, Any?>(f.name()) {
               styleClass += if (f.type.isSubclassOf<String>()) "column-header-align-left" else "column-header-align-right"
               cellValueFactory = Callback { it.value?.let { PojoV(f.getOf(it)) } }
               cellFactory = Callback { f.buildFieldedCell() }
            }
         }
      }
      table.rowFactory = Callback { t ->
         SpitTableRow<MetadataGroup>().apply {
            styleRuleAdd(pseudoclass(played)) { it.isPlaying() }
            onLeftDoubleClick { _, _ -> playSelected() }
            onRightSingleClick { row, e ->
               if (!row.isSelected) t.selectionModel.clearAndSelect(row.index)
               val data = t.selectionModel.selectedItems.takeIf { it.size==1 }?.first() ?: MetadataGroup.groupOfUnrelated(filerSortInputList())
               ValueContextMenu<MetadataGroup>().apply {
                  setItemsFor(data)
                  show(table, e)
               }
            }
         }
      }
      APP.audio.playingSong.onUpdate { _, _ -> table.updateStyleRules() } on onClose

      table.defaultColumnInfo   // trigger menu initialization
      table.columnState = widget.properties["columns"].asIf<String>()?.let { ColumnState.fromString(it).orNull() } ?: table.defaultColumnInfo

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
      // column context menu - maintain VALUE name
      fun updateValueName() {
         table.columnVisibleMenu.items.find { it.userData == VALUE }?.text = VALUE.toString(fieldFilter.value)
         table.search.menu.items.find { it.userData == VALUE }?.text = VALUE.toString(fieldFilter.value)
      }
      fieldFilter attach { updateValueName() }
      table.columnMenu.onEventDown(WINDOW_SHOWING) { updateValueName() }

      // add menu items
      table.menuRemove.dsl {
         item("Remove songs in selected groups from library (${keys(DELETE)})") { removeSongs(ungroup(table.selectedItems)) }
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

      // drag % drop
      table.onEventDown(DRAG_DETECTED, PRIMARY, false) {
         if (!table.selectedItems.isEmpty() && table.isRowFull(table.getRowS(it.sceneX, it.sceneY))) {
            table.startDragAndDrop(*ANY).setSongsAndFiles(filerSortInputList())
            it.consume()
         }
      }
      // drag % drop - prevent parent
      root.installDrag(IconFA.PLUS, "Add to library", { Df.SONGS in it.dragboard }, { true }, {})

      table.consumeScrolling()

      // resizing
      table.columnResizePolicy = Callback { resize ->
         UNCONSTRAINED_RESIZE_POLICY(resize).apply {
            val t = table
            // resize index column
            t.getColumn(ColumnField.INDEX).ifPresent { it.setPrefWidth(t.computeIndexColumnWidth()) }
            // resize main column to span remaining space
            t.getColumn(VALUE).ifPresent { c ->
               val sumW = t.columns.asSequence().filter { it.isVisible }.sumOf { it.width }
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
      root.sync1IfInScene { inputItems.bindDefaultIf1stLoad(APP.db.songs) } on onClose

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
      table.filterPane.data = MgField.all.map { PredicateData(it.toString(f), it.getMFType(f), it.asIs<ObjectField<MetadataGroup, Any?>>()) }
      table.filterPane.clear()
      if (refreshItems) setItems(inputItems.value)
   }

   /** Populates metadata groups to table from metadata list. */
   private fun setItems(list: Metadatas?) {
      if (list==null) return

      val f = fieldFilter.value
      runIO {
         listOf(MetadataGroup.groupOf(f, list)) + MetadataGroup.groupsOf(f, list)
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

   private fun filerSortInputList() = filterList(inputItems.value, false).sortedWith(APP.audio.songOrderComparator)

   private fun playSelected() = play(filerSortInputList())

   private fun play(items: Metadatas) {
      if (items.isNotEmpty())
         PlaylistManager.use { it.setAndPlay(items) }
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

   override fun focus() = table.requestFocus()

   companion object: WidgetCompanion, KLogging() {
      override val name = SONG_GROUP_TABLE_NAME
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