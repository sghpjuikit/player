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
import javafx.scene.input.TransferMode.COPY
import javafx.stage.WindowEvent.WINDOW_HIDDEN
import javafx.stage.WindowEvent.WINDOW_SHOWING
import javafx.util.Callback
import sp.it.pl.audio.Player
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.CATEGORY
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.MetadataGroup.Companion.ungroup
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.AVG_RATING
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.VALUE
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.W_RATING
import sp.it.pl.gui.itemnode.FieldedPredicateItemNode.PredicateData
import sp.it.pl.gui.objects.contextmenu.SelectionMenuItem.buildSingleSelectionMenu
import sp.it.pl.gui.objects.contextmenu.ValueContextMenu
import sp.it.pl.gui.objects.table.FilteredTable
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV
import sp.it.pl.gui.objects.table.TableColumnInfo
import sp.it.pl.gui.objects.tablecell.NumberRatingCellFactory
import sp.it.pl.gui.objects.tablerow.ImprovedTableRow
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.LIBRARY
import sp.it.pl.layout.widget.Widget.Info
import sp.it.pl.layout.widget.controller.LegacyController
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.main.scaleEM
import sp.it.pl.main.setSongsAndFiles
import sp.it.util.access.VarEnum
import sp.it.util.access.Vo
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.initAttach
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.runNew
import sp.it.util.collections.setTo
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.functional.Util.list
import sp.it.util.functional.Util.stream
import sp.it.util.functional.invoke
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.syncTo
import sp.it.util.ui.Util.menuItem
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.x
import kotlin.streams.asSequence
import kotlin.streams.toList

private typealias CellFactory<T> = Callback<TableColumn<MetadataGroup, T>, TableCell<MetadataGroup, T>>

@Suppress("UNCHECKED_CAST")
@Info(
        author = "Martin Polakovic",
        name = Widgets.SONG_GROUP_TABLE,
        description = "Provides database filtering.",
        howto = "Available actions:\n"+
        "    Song left click : Selects item\n"+
        "    Song right click : Opens context menu\n"+
        "    Song double click : Plays item\n"+
        "    Type : search & filter\n"+
        "    Press ENTER : Plays item\n"+
        "    Press ESC : Clear selection & filter\n"+
        "    Scroll : Scroll table vertically\n"+
        "    Scroll + SHIFT : Scroll table horizontally\n"+
        "    Column drag : swap columns\n"+
        "    Column right click: show column menu\n"+
        "    Click column : Sort - ascending | descending | none\n"+
        "    Click column + SHIFT : Sorts by multiple columns\n",
        version = "0.9.0",
        year = "2015",
        group = LIBRARY)
@LegacyController
class LibraryView(widget: Widget): SimpleController(widget) {

    private val table = FilteredTable(MetadataGroup::class.java, VALUE)
    private val outputSelectedGroup = outputs.create<List<MetadataGroup>>(widget.id, "Selected Groups", listOf())
    private val outputSelectedSongs = outputs.create<List<Metadata>>(widget.id, "Selected Songs", listOf())
    private val inputItems = inputs.create<List<Metadata>>("To display", listOf()) { setItems(it) }

    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    val tableOrient by cv(NodeOrientation.INHERIT) { Vo(APP.ui.tableOrient) }
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    val tableZeropad by cv(true) { Vo(APP.ui.tableZeropad) }
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    val tableOrigIndex by cv(true) { Vo(APP.ui.tableOrigIndex) }
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    val tableShowHeader by cv(true) { Vo(APP.ui.tableShowHeader) }
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menu bar and table content information.")
    val tableShowFooter by cv(true) { Vo(APP.ui.tableShowFooter) }
    @IsConfig(name = "Field")
    val fieldFilter by cv(CATEGORY as Metadata.Field<*>) {
        VarEnum(it, Metadata.Field.FIELDS.filter { it.isTypeStringRepresentable() })
                .initAttach { applyData() }
    }

    // restoring selection if table items change, we want to preserve as many
    // selected items as possible - when selection changes, we select all items
    // (previously selected) that are still in the table
    private var selIgnore = false
    private var selOld = setOf<Any?>()
    // restoring selection from previous session, we serialize string
    // representation and try to restore when application runs again
    // we restore only once
    @IsConfig(name = "Last selected", editable = EditMode.APP)
    private var selLast by c("null")
    private var selLastRestored = false

    init {
        root.prefSize = 600.scaleEM() x 600.scaleEM()
        root.consumeScrolling()
        root.lay += table.root

        table.selectionModel.selectionMode = MULTIPLE
        table.search.setColumn(VALUE)
        tableOrient syncTo table.nodeOrientationProperty() on onClose
        tableZeropad syncTo table.zeropadIndex on onClose
        tableOrigIndex syncTo table.showOriginalIndex on onClose
        tableShowHeader syncTo table.headerVisible on onClose
        tableShowFooter syncTo table.footerVisible on onClose

        // set up table columns
        table.setKeyNameColMapper { name -> if (ColumnField.INDEX.name()==name) name else MetadataGroup.Field.valueOf(name).name() }
        table.setColumnFactory { f ->
            if (f is MetadataGroup.Field<*>) {
                val mgf = f as MetadataGroup.Field<*>
                val mf = fieldFilter.value
                TableColumn<MetadataGroup, Any>(mgf.toString(mf)).apply {
                    cellValueFactory = Callback { it.value?.let { PojoV(mgf.getOf(it)) } }
                    cellFactory = when (mgf) {
                        AVG_RATING -> APP.ratingCell.value as CellFactory<Any?>
                        W_RATING -> NumberRatingCellFactory as CellFactory<Any?>
                        else -> CellFactory {
                            table.buildDefaultCell(mgf as ObjectField<in MetadataGroup, Any?>).apply {
                               alignment = if (mgf.getType(mf)==String::class.java) CENTER_LEFT else CENTER_RIGHT
                            }
                        }
                    }
                }
            } else {
                TableColumn<MetadataGroup, Any>(f.toString()).apply {
                    cellValueFactory = Callback { it.value?.let { PojoV(f.getOf(it)) } }
                    cellFactory = Callback { table.buildDefaultCell(f) }
                }
            }
        }
        APP.ratingCell sync { cf -> table.getColumn(AVG_RATING).orNull()?.cellFactory = cf as CellFactory<Double?> } on onClose

        // rows
        table.setRowFactory { t ->
            ImprovedTableRow<MetadataGroup>().apply {
                styleRuleAdd(pcPlaying) { it.isPlaying() }
                onLeftDoubleClick { _, _ -> playSelected() }
                onRightSingleClick { row, e ->
                    // prep selection for context menu
                    if (!row.isSelected)
                        t.selectionModel.clearAndSelect(row.index)

                    contextMenuInstance.setItemsFor(MetadataGroup.groupOfUnrelated(filerSortInputList()))
                    contextMenuInstance.show(table, e)
                }
            }
        }
        Player.playingSong.onUpdate { table.updateStyleRules() } on onClose

        table.defaultColumnInfo   // trigger menu initialization
        table.columnState = widget.properties.getS("columns")?.net { TableColumnInfo.fromString(it) } ?: table.defaultColumnInfo

        // column context menu - add group by menu
        val fieldMenu = Menu("Group by")
        table.columnMenu.items.add(fieldMenu)
        table.columnMenu.onEventDown(WINDOW_HIDDEN) { fieldMenu.items.clear() }
        table.columnMenu.onEventDown(WINDOW_SHOWING) {
            fieldMenu.items setTo buildSingleSelectionMenu(
                    list(Metadata.Field.FIELDS), fieldFilter.value,
                    { it.name() },
                    { fieldFilter.setValue(it) }
            )
        }

        // add menu items
        table.menuRemove.items.addAll(
                menuItem("Remove selected groups from library") { APP.db.removeSongs(ungroup(table.selectedItems)) },
                menuItem("Remove playing group from library") {  APP.db.removeSongs(ungroup(table.items.filter { it.isPlaying() })) },
                menuItem("Remove all groups from library") { APP.db.removeSongs(ungroup(table.items)) }
        )

        table.onEventDown(KEY_PRESSED, ENTER) { playSelected() }
        table.onEventDown(KEY_PRESSED, DELETE) { APP.db.removeSongs(table.selectedItems.flatMap { it.grouped }) }
        table.onEventDown(DRAG_DETECTED, PRIMARY, false) {
            if (!table.selectedItems.isEmpty() && table.isRowFull(table.getRowS(it.sceneX, it.sceneY))) {
                table.startDragAndDrop(COPY).setSongsAndFiles(filerSortInputList())
                it.consume()
            }
        }

        // resizing
        table.setColumnResizePolicy { resize ->
            UNCONSTRAINED_RESIZE_POLICY(resize).apply {
                val t = table
                // resize index column
                t.getColumn(ColumnField.INDEX).ifPresent { it.setPrefWidth(t.computeIndexColumnWidth()) }
                // resize main column to span remaining space
                t.getColumn(VALUE).ifPresent { c ->
                    val sumW = t.columns.asSequence().filter { it.isVisible }.sumByDouble { it.width }
                    val sbW = t.vScrollbarWidth
                    c.setPrefWidth(t.width-(sbW+sumW-c.width))
                }
            }
        }

        // forward selection
        val selectedItemsReducer = EventReducer.toLast<Void>(100.0) {
            outputSelectedGroup.value = table.selectedItemsCopy
            outputSelectedSongs.value = filterList(inputItems.value, true)
        }
        table.selectedItems.onChange { if (!selIgnore) selectedItemsReducer.push(null) } on onClose
        table.selectionModel.selectedItemProperty() sync { selLast = it?.getValueS("") ?: "null" } on onClose

        applyData(false)
    }

    override fun getFields(): Collection<Config<Any>> {
        widget.properties["columns"] = table.columnState.toString()
        return super.getFields()
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
        table.filterPane.inconsistent_state = true
        table.filterPane.setPrefTypeSupplier { PredicateData.ofField(VALUE) }
        table.filterPane.data = MetadataGroup.Field.FIELDS.map { PredicateData(it.toString(f), it.getType(f), it as ObjectField<MetadataGroup, Any>) }
        table.filterPane.shrinkTo(0)
        table.filterPane.growTo1()
        table.filterPane.clear()
        if (refreshItems) setItems(inputItems.value)
    }

    /** Populates metadata groups to table from metadata list. */
    private fun setItems(list: List<Metadata>?) {
        if (list==null) return

        val f = fieldFilter.value
        runNew {
            stream(MetadataGroup.groupOf(f, list), MetadataGroup.groupsOf(f, list)).asSequence().toList()
        } ui {
            if (!it.isEmpty()) {
                selectionStore()
                table.setItemsRaw(it)
                selectionReStore()
            }
        }
    }

    private fun filterList(list: List<Metadata>?, orAll: Boolean): List<Metadata> = when {
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

    private fun filerSortInputList(): List<Metadata> = filterList(inputItems.value, false).sortedWith(APP.db.libraryComparator.value)

    private fun playSelected() = play(filerSortInputList())

    private fun play(items: List<Metadata>) {
        if (!items.isEmpty())
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
        // performance optimization - prevents refreshes of a lot of items
        if (table.selectionModel.isEmpty)
            table.selectionModel.select(0)

        selIgnore = false
        outputSelectedGroup.value = table.selectedItemsCopy
        outputSelectedSongs.value = filterList(inputItems.value, true)
    }

    companion object {
        private val pcPlaying = pseudoclass("played")
        private val contextMenuInstance by lazy { ValueContextMenu<MetadataGroup>() }
    }

}