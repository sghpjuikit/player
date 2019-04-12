package library

import javafx.geometry.NodeOrientation
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
import javafx.util.Callback
import sp.it.pl.audio.Player
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.RATING
import sp.it.pl.audio.tagging.Metadata.Field.Companion.TITLE
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.removeMissingSongsFromLibTask
import sp.it.pl.gui.nodeinfo.TableInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.pl.gui.objects.contextmenu.ValueContextMenu
import sp.it.pl.gui.objects.table.FilteredTable
import sp.it.pl.gui.objects.table.ImprovedTable.PojoV
import sp.it.pl.gui.objects.table.TableColumnInfo
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
import sp.it.pl.main.scaleEM
import sp.it.pl.main.setSongsAndFiles
import sp.it.util.access.Vo
import sp.it.util.access.fieldvalue.ColumnField
import sp.it.util.async.runNew
import sp.it.util.conf.Config
import sp.it.util.conf.EditMode
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.only
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.Util.getCommonRoot
import sp.it.util.file.parentDir
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
import sp.it.util.ui.item
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoclass
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.toHMSMs
import sp.it.util.validation.Constraint.FileActor

@Info(
        author = "Martin Polakovic",
        name = Widgets.SONG_TABLE,
        description = "Provides access to database.",
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
                "    Click column + SHIFT : Sorts by multiple columns\n"+
                "    Menu bar : Opens additional actions\n",
        version = "1.0.0",
        year = "2015",
        group = LIBRARY
)
class Library(widget: Widget): SimpleController(widget), SongReader {

    private val table = FilteredTable(Metadata::class.java, Metadata.EMPTY.getMainField())
    private val outputSelected = io.o.create<Metadata>("Selected", null)
    private val inputItems = io.i.create<List<Metadata>>("To display", listOf()) { setItems(it) }

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
    @IsConfig(name = "Last add songs browse location", editable = EditMode.APP)
    private var lastAddFilesLocation by cn(APP.DIR_USERDATA).only(FileActor.ANY)
    @IsConfig(name = "Last add directory browse location", editable = EditMode.APP)
    private var lastAddDirLocation by cn(APP.DIR_USERDATA).only(FileActor.DIRECTORY)

    init {
        root.prefSize = 850.scaleEM() x 600.scaleEM()
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
        table.items_info.textFactory = { all, list -> DEFAULT_TEXT_FACTORY(all, list)+" - "+list.sumByDouble { it.getLengthInMs() }.millis.toHMSMs() }

        // add more menu items
        table.menuAdd.apply {
            item("Add files") { addFiles() }
            item("Add directory") { addDirectory() }
        }
        table.menuRemove.apply {
            item("Remove selected songs from library") { APP.db.removeSongs(table.selectedItems) }
            item("Remove all shown songs from library") { APP.db.removeSongs(table.items) }
            item("Remove all songs from library") { APP.db.removeSongs(table.items) }
            item("Remove missing songs from library") { removeInvalid() }
        }

        // set up table columns
        table.setColumnFactory { field ->
            TableColumn<Metadata, Any?>(field.name()).apply {
                @Suppress("UNCHECKED_CAST")
                cellFactory = when(field) {
                    RATING -> APP.ratingCell.value as Callback<TableColumn<Metadata, Any?>, TableCell<Metadata, Any?>>
                    else -> Callback { table.buildDefaultCell(field) }
                }
                cellValueFactory = Callback { it.value?.net { PojoV(field.getOf(it)) } }
            }
        }
        APP.ratingCell sync { cf -> table.getColumn(RATING).orNull()?.cellFactory = cf } on onClose

        // column resizing
        table.setColumnResizePolicy {
            UNCONSTRAINED_RESIZE_POLICY(it).apply {
                table.getColumn(ColumnField.INDEX).orNull()?.prefWidth = table.computeIndexColumnWidth()
            }
        }

        // row behavior
        table.setRowFactory { t ->
            ImprovedTableRow<Metadata>().apply {
                onLeftDoubleClick { r, _ -> PlaylistManager.use { it.setNplayFrom(table.items, r.index) } }
                onRightSingleClick { r, e ->
                    // prep selection for context menu
                    if (!r.isSelected)
                        t.selectionModel.clearAndSelect(r.index)

                    contextMenuInstance.setItemsFor(MetadataGroup.groupOfUnrelated(table.selectedItemsCopy))
                    contextMenuInstance.show(table, e)
                }
                styleRuleAdd(pcPlaying) { Player.playingSong.value.same(it) }
            }
        }
        Player.playingSong.onUpdate { table.updateStyleRules() } on onClose

        table.defaultColumnInfo   // trigger menu initialization
        table.columnState = widget.properties.getS("columns")?.net { TableColumnInfo.fromString(it) } ?: table.defaultColumnInfo

        table.onEventDown(KEY_PRESSED, ENTER, false) {
            if (!table.selectionModel.isEmpty) {
                PlaylistManager.use { it.setNplayFrom(table.items, table.selectionModel.selectedIndex) }
                it.consume()
            }
        }
        table.onEventDown(KEY_PRESSED, DELETE, false) {
            if (!table.selectionModel.isEmpty) {
                APP.db.removeSongs(table.selectedItems)
                it.consume()
            }
        }
        table.onEventDown(DRAG_DETECTED, PRIMARY, false) {
            if (!table.selectedItems.isEmpty() && table.isRowFull(table.getRowS(it.sceneX, it.sceneY))) {
                table.startDragAndDrop(COPY).setSongsAndFiles(table.selectedItemsCopy)
                it.consume()
            }
        }

        // sync outputs
        table.selectionModel.selectedItemProperty() sync { outputSelected.value = it } on onClose
        outputSelected.bind(Player.librarySelected.i) on onClose
        root.sync1IfInScene { if (!inputItems.isBound()) inputItems.bind(APP.db.songs.o) } on onClose

        // sync library comparator
        table.itemsComparator syncTo APP.db.libraryComparator on onClose

    }

    override fun getFields(): Collection<Config<Any>> {
        widget.properties["columns"] = table.columnState.toString()
        return super.getFields()
    }

    override fun read(items: List<Song>?) {
        if (items==null) return
        table.setItemsRaw(items.map { it.toMeta() })
    }

    fun setItems(items: List<Metadata>?) {
        if (items==null) return
        table.setItemsRaw(items)
    }

    private fun addDirectory() {
        chooseFile("Add folder to library", DIRECTORY, lastAddDirLocation, root.scene.window).ifOk {
            APP.actionPane.show(it)
            lastAddDirLocation = it.parentDir
        }
    }

    private fun addFiles() {
        chooseFiles("Add files to library", lastAddFilesLocation, root.scene.window, audioExtensionFilter()).ifOk {
            APP.actionPane.show(it)
            lastAddFilesLocation = getCommonRoot(it)
        }
    }

    private fun removeInvalid() {
        val task = removeMissingSongsFromLibTask()
        runNew(task)
        AppProgress.start(task)
    }

    companion object {
        private val pcPlaying = pseudoclass("played")
        private val contextMenuInstance by lazy { ValueContextMenu<MetadataGroup>() }
    }

}