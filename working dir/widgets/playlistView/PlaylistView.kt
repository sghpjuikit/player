package playlistView

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FILTER
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FILTER_OUTLINE
import javafx.event.EventHandler
import javafx.geometry.NodeOrientation.INHERIT
import javafx.scene.control.Menu
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.stage.FileChooser
import sp.it.pl.audio.Player
import sp.it.pl.audio.PlayerConfiguration
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.playlist.PlaylistItem
import sp.it.pl.audio.playlist.PlaylistItem.Field
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.playlist.writePlaylist
import sp.it.pl.gui.nodeinfo.TableInfo.Companion.DEFAULT_TEXT_FACTORY
import sp.it.pl.gui.objects.table.PlaylistTable
import sp.it.pl.gui.objects.table.TableColumnInfo
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group
import sp.it.pl.layout.widget.WidgetSource.NO_LAYOUT
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets.PLAYLIST
import sp.it.pl.main.rowHeight
import sp.it.pl.util.access.V
import sp.it.pl.util.access.Vo
import sp.it.pl.util.access.initSync
import sp.it.pl.util.async.executor.ExecuteN
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.conf.Config
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cn
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.file.parentDirOrRoot
import sp.it.pl.util.functional.net
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.graphics.Util.menuItem
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.syncTo
import sp.it.pl.util.system.saveFile
import sp.it.pl.util.type.Util
import sp.it.pl.util.units.Dur
import sp.it.pl.util.validation.Constraint
import java.util.UUID
import java.util.function.Consumer
import java.util.function.UnaryOperator
import kotlin.streams.asSequence

@Widget.Info(
        author = "Martin Polakovic",
        name = PLAYLIST,
        description = "Provides list of items to play. Highlights playing and unplayable "+"items.",
        howto = ""
                +"Available actions:\n"
                +"    Item left click : Selects item\n"
                +"    Item right click : Opens context menu\n"
                +"    Item double click : Plays item\n"
                +"    Item drag : Activates Drag&Drop\n"
                +"    Item drag + CTRL : Moves item within playlist\n"
                +"    Type : search & filter\n"
                +"    Press ENTER : Plays item\n"
                +"    Press ESC : Clear selection & filter\n"
                +"    Scroll : Scroll table vertically\n"
                +"    Scroll + SHIFT : Scroll table horizontally\n"
                +"    Column drag : swap columns\n"
                +"    Column right click: show column menu\n"
                +"    Click column : Sort - ascending | descending | none\n"
                +"    Click column + SHIFT : Sorts by multiple columns\n"
                +"    Menu bar : Opens additional actions\n",
        notes = "Plans: multiple playlists through tabs",
        version = "1",
        year = "2015",
        group = Group.PLAYLIST
)
class PlaylistView(widget: Widget<*>): SimpleController(widget), PlaylistFeature {

    private val playlist = computeInitialPlaylist(widget.id)
    private val table = PlaylistTable(playlist)
    private val columnInitializer = ExecuteN(1)
    private var outSelected: Output<PlaylistItem?> = outputs.create(widget.id, "Selected", PlaylistItem::class.java, null)
    private var outPlaying: Output<PlaylistItem?> = outputs.create(widget.id, "Playing", PlaylistItem::class.java, null)

    @IsConfig(name = "Table orientation", info = "Orientation of the table.")
    val orient by cv(INHERIT) { Vo(APP.ui.tableOrient) }
    @IsConfig(name = "Zeropad numbers", info = "Adds 0s for number length consistency.")
    val zeropad by cv(true) { Vo(APP.ui.tableZeropad) }
    @IsConfig(name = "Search show original index", info = "Show unfiltered table item index when filter applied.")
    val orig_index by cv(true) { Vo(APP.ui.tableOrigIndex) }
    @IsConfig(name = "Show table header", info = "Show table header with columns.")
    val show_header by cv(true) { Vo(APP.ui.tableShowHeader) }
    @IsConfig(name = "Show table footer", info = "Show table controls at the bottom of the table. Displays menubar and table items information.")
    val show_footer by cv(true) { Vo(APP.ui.tableShowFooter) }
    @IsConfig(name = "Scroll to playing", info = "Scroll table to playing item when it changes.")
    val scrollToPlaying by cv(true)
    @IsConfig(name = "Play displayed only", info = "Only displayed items will be played when filter is active.")
    val filter_for_playback by cv(false) {
        V(it).initSync { v ->
            table.filterPane.button.icon(if (v) FILTER else FILTER_OUTLINE)
            table.filterPane.button.onClick(Runnable { filterToggle() })
            table.filterPane.button.tooltip(
                    if (v) "Disable filter for playback. Causes the playback "+"to ignore the filter."
                    else "Enable filter for playback. Causes the playback "+"to play only displayed items."
            )
            table.filterPane.button.isDisable = false // needed
            playlist.setTransformation(
                    if (v) UnaryOperator<List<PlaylistItem>> { table.items.materialize() }
                    else UnaryOperator { it.asSequence().sortedWith(table.itemsComparator.value).toList() }
            )
        }
    }
    @IsConfig(name = "Default browse location", info = "Opens this location for file dialogs.")
    var lastSavePlaylistLocation by cn(APP.DIR_USERDATA).only(Constraint.FileActor.DIRECTORY)

    init {
        onClose += Player.playlistSelected.i.bind(outSelected)
        onClose += playlist.playingI sync { outPlaying.value = playlist.playing }
        onClose += Player.onItemRefresh { ms ->
            outPlaying.value?.let { ms.ifHasK(it.uri, Consumer { outPlaying.value = it.toPlaylist() }) }
            outSelected.value?.let { ms.ifHasK(it.uri, Consumer { outSelected.value = it.toPlaylist() }) }
        }

        table.search.setColumn(Field.NAME)
        table.selectionModel.selectionMode = MULTIPLE
        table.items_info.textFactory = { all, list -> DEFAULT_TEXT_FACTORY(all, list)+" - "+Dur(list.sumByDouble { it.timeMs }) }
        onClose += APP.ui.font sync { table.fixedCellSize = it.rowHeight() }
        onClose += orient syncTo table.nodeOrientationProperty()
        onClose += zeropad syncTo table.zeropadIndex
        onClose += orig_index syncTo table.showOriginalIndex
        onClose += show_header syncTo table.headerVisible
        onClose += show_footer syncTo table.footerVisible
        onClose += scrollToPlaying syncTo table.scrollToPlaying

        this layFullArea table.root

        table.menuAdd.items.addAll(
                menuItem("Add files") { PlaylistManager.chooseFilesToAdd() },
                menuItem("Add directory") { PlaylistManager.chooseFolderToAdd() },
                menuItem("Add URL") { PlaylistManager.chooseUrlToAdd() },
                menuItem("Play files") { PlaylistManager.chooseFilesToPlay() },
                menuItem("Play directory") { PlaylistManager.chooseFolderToPlay() },
                menuItem("Play URL") { PlaylistManager.chooseUrlToPlay() },
                menuItem("Duplicate selected (+)") { playlist.duplicateItemsByOne(table.selectedItems) },
                menuItem("Duplicate selected (*)") { playlist.duplicateItemsAsGroup(table.selectedItems) }
        )
        table.menuRemove.items.addAll(
                menuItem("Remove selected") { playlist -= table.selectedItems },
                menuItem("Remove not selected") { playlist.retainAll(table.selectedItems) },
                menuItem("Remove unsupported") { playlist.removeUnplayable() },
                menuItem("Remove duplicates") { playlist.removeDuplicates() },
                menuItem("Remove all") { playlist.clear() }
        )
        table.menuOrder.items.addAll(
                menuItem("Order reverse") { playlist.reverse() },
                menuItem("Order randomly") { playlist.randomize() },
                menuItem("Edit selected") { APP.widgetManager.widgets.use<SongReader>(NO_LAYOUT) { it.read(table.selectedItems) } },
                menuItem("Save playlist") {
                    saveFile(
                            "Choose playlist location",
                            lastSavePlaylistLocation ?: PlayerConfiguration.lastSavePlaylistLocation,
                            "Playlist",
                            widget.windowOrActive.orNull()?.stage ?: APP.windowManager.createStageOwner(),
                            FileChooser.ExtensionFilter("m3u8", "m3u8")
                    ).ifOk { file ->
                        lastSavePlaylistLocation = file.parentDirOrRoot
                        PlayerConfiguration.lastSavePlaylistLocation = file.parentDirOrRoot
                        writePlaylist(table.selectedOrAllItemsCopy, file.name, file.parentDirOrRoot)
                    }
                }
        )
        table.menuOrder.items addToStart Menu("Order by").apply {
            items += Field.FIELDS.map { f -> menuItem(f.toStringEnum()) { table.sortBy(f) } }
        }
        onClose += table.selectionModel.selectedItemProperty() attach {
            if (!table.movingItems)
                outSelected.value = it
        }
        onClose += table::dispose

        onScroll = EventHandler { it.consume() }

    }

    override fun getFields(): Collection<Config<Any>> {
        widget.properties["columns"] = table.columnState.toString()
        return super.getFields()
    }

    override fun refresh() {
        columnInitializer.execute {
            table.columnState = widget.properties.getS("columns")?.net { TableColumnInfo.fromString(it) } ?: table.defaultColumnInfo
        }
        filter_for_playback.applyValue()
    }

    override fun getPlaylist() = playlist

    private fun filterToggle(): Unit = filter_for_playback.setCycledNapplyValue()

    private fun computeInitialPlaylist(id: UUID) = null
            ?: PlaylistManager.playlists[id]
            ?: getUnusedPlaylist(id).apply {
                PlaylistManager.playlists.put(this)
            }

    private fun getUnusedPlaylist(id: UUID): Playlist {
        val danglingPlaylists = ArrayList(PlaylistManager.playlists)
        APP.widgetManager.widgets.findAll(OPEN).asSequence()
                .filter { it.info.hasFeature(PlaylistFeature::class.java) }
                .mapNotNull { (it.controller as PlaylistFeature?)?.playlist }
                .forEach { danglingPlaylists.removeIf { playlist -> playlist.id==it.id } }

        val danglingPlaylist: Playlist? = null
                ?: danglingPlaylists.find { it.id==PlaylistManager.active }
                ?: danglingPlaylists.firstOrNull()

        if (danglingPlaylist!=null) {
            PlaylistManager.playlists.remove(danglingPlaylist)

            if (danglingPlaylist.id==PlaylistManager.active)
                PlaylistManager.active = id
            Util.setField(Playlist::class.java, danglingPlaylist, Playlist::id.name, id) // TODO: fix
        }

        return danglingPlaylist ?: Playlist(id)
    }

    companion object {
        infix fun <T> MutableList<T>.addToStart(item: T) = add(0, item)
    }

}