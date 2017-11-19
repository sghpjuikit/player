@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package gui.objects.contextmenu

import audio.playlist.PlaylistManager
import audio.tagging.Metadata.Field
import audio.tagging.MetadataGroup
import audio.tagging.PlaylistItemGroup
import gui.objects.image.Thumbnail
import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseEvent
import layout.widget.WidgetManager.WidgetSource.NO_LAYOUT
import layout.widget.feature.FileExplorerFeature
import layout.widget.feature.SongReader
import layout.widget.feature.SongWriter
import main.App
import main.App.APP
import mu.KotlinLogging
import util.access.AccessibleValue
import util.collections.map.ClassListMap
import util.file.ImageFileFormat
import util.file.Util.recycleFile
import util.file.Util.writeImage
import util.functional.seqOf
import util.graphics.Util.menuItem
import util.graphics.getScreen
import util.system.Environment
import util.system.copyToSysClipboard
import util.system.saveFile
import web.SearchUriBuilder
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.test.fail

/** Context menu wrapping a value - usually an object set before showing, for menu items' action. */
open class ImprovedContextMenu<E: Any>: ContextMenu(), AccessibleValue<E> {

    private lateinit var v: E

    init {
        consumeAutoHidingEvents = false
    }

    override fun getValue(): E = v

    override fun setValue(value: E) {
        v = value
    }

    fun setValueAndItems(value: E) {
        v = value
        setItemsForValue()
    }

    override fun show(n: Node, screenX: Double, screenY: Double) = super.show(n.scene.window, screenX, screenY)

    /**
     * Shows the context menu for node at proper coordinates of the event.
     *
     * Prefer this method to show context menu (especially in MouseClick handler), because when showing ContextMenu,
     * there is a difference between show(Window,x,y) and (Node,x,y). The former will not hide the menu when next click
     * happens within the node itself! This method avoids that.
     */
    fun show(n: Node, e: MouseEvent) = super.show(n.scene.window, e.screenX, e.screenY)

    fun show(n: Node, e: ContextMenuEvent) = super.show(n.scene.window, e.screenX, e.screenY)

    @JvmOverloads
    fun addItemsForValue(value: Any? = v) {
        items += CONTEXT_MENUS[this, value]
    }

    @JvmOverloads
    fun setItemsForValue(value: Any? = v) {
        items.clear()
        addItemsForValue(value)
    }

}

private typealias ItemsSupply = (ImprovedContextMenu<*>, Any?) -> Sequence<MenuItem>
val logger = KotlinLogging.logger { }

class ContextMenuItemSuppliers {
    private val m = ClassListMap<ItemsSupply> { fail() }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> add(type: Class<T>, items: (ImprovedContextMenu<*>, T) -> Sequence<MenuItem>) {
        m.accumulate(type, items as ItemsSupply)
    }

    inline fun <reified T: Any> add(noinline items: (ImprovedContextMenu<*>, T) -> Sequence<MenuItem>) {
        add(T::class.java, items)
    }

    operator fun get(contextMenu: ImprovedContextMenu<*>, value: Any?): Sequence<MenuItem> {
        return m.getElementsOfSuperV(value?.javaClass ?: Void::class.java).asSequence()
                .map { it(contextMenu, value) }
                .flatMap { it }
    }
}

val CONTEXT_MENUS = ContextMenuItemSuppliers().apply {
    add<File> { contextMenu, file -> notNullSeqOf(
            menuItem("Browse location") { Environment.browse(file) },
            menuItem("Open (in associated program)") { Environment.open(file) },
            menuItem("Edit (in associated editor)") { Environment.edit(file) },
            menuItem("Delete from disc") { recycleFile(file) },
            menuItem("Copy as ...") {
                saveFile("Copy as...", App.APP.DIR_APP, file.name, contextMenu.ownerWindow, ImageFileFormat.filter())
                        .ifOk { nf ->
                            // TODO: move low lvl impl. to utils
                            try {
                                Files.copy(file.toPath(), nf.toPath(), StandardCopyOption.REPLACE_EXISTING)
                            } catch (e: IOException) {
                                logger.error(e) { "File copy failed" }
                            }
                        }
            }
    )}
    add<MetadataGroup> { contextMenu, mg -> notNullSeqOf(
            menuItem("Play items") { PlaylistManager.use { it.setNplay(mg.grouped.stream().sorted(APP.db.libraryComparator.get())) } },
            menuItem("Enqueue items") { PlaylistManager.use { it.addItems(mg.grouped) } },
            menuItem("Update items from file") { APP.actions.refreshItemsFromFileJob(mg.grouped) },
            menuItem("Remove items from library") { APP.db.removeItems(mg.grouped) },
            menuWithItems(
                    "Show in",
                    App.APP.widgetManager.getFactories().filter { it.hasFeature(SongReader::class) },
                    { it.nameGui() },
                    { App.APP.widgetManager.use(it.nameGui(), NO_LAYOUT) { c -> (c.controller as SongReader).read(mg.grouped) } }
            ),
            menuWithItems(
                    "Edit tags in",
                    App.APP.widgetManager.getFactories().filter { it.hasFeature(SongWriter::class) },
                    { it.nameGui() },
                    { App.APP.widgetManager.use(it.nameGui(), NO_LAYOUT) { c -> (c.controller as SongWriter).read(mg.grouped) } }
            ),
            menuItem("Explore items's directory") { Environment.browse(mg.grouped.stream().filter { it.isFileBased() }.map { it.getFile() }) },
            menuWithItems(
                    "Explore items' directory in",
                    App.APP.widgetManager.getFactories().filter { it.hasFeature(FileExplorerFeature::class) },
                    { it.nameGui() },
                    { App.APP.widgetManager.use(it.nameGui(), NO_LAYOUT) { c -> (c.controller as FileExplorerFeature).exploreFile(mg.grouped[0].getFile()) } }
            ),
            if (mg.field!=Field.ALBUM) null
            else menuWithItems(
                    "Search cover in",
                    App.APP.instances.getInstances<SearchUriBuilder>().stream(),
                    { "in ${it.name}" },
                    { Environment.browse(it.apply(mg.getValueS("<none>"))) }
            )
    )}
    add<PlaylistItemGroup> { contextMenu, pig -> notNullSeqOf(
            menuItem("Play items") { PlaylistManager.use { it.playItem(pig.items[0]) } },
            menuItem("Remove items") { PlaylistManager.use { it.removeAll(pig.items) } },
            menuWithItems(
                    "Show in",
                    App.APP.widgetManager.getFactories().filter { it.hasFeature(SongReader::class) },
                    { it.nameGui() },
                    { App.APP.widgetManager.use(it.nameGui(), NO_LAYOUT) { c -> (c.controller as SongReader).read(pig.items) } }
            ),
            menuWithItems(
                    "Edit tags in",
                    App.APP.widgetManager.getFactories().filter { it.hasFeature(SongWriter::class) },
                    { it.nameGui() },
                    { App.APP.widgetManager.use(it.nameGui(), NO_LAYOUT) { c -> (c.controller as SongWriter).read(pig.items) } }
            ),
            menuItem("Crop items") { PlaylistManager.use { it.retainAll(pig.items) } },
            menuItem("Duplicate items as group") { PlaylistManager.use { it.duplicateItemsAsGroup(pig.items) } },
            menuItem("Duplicate items individually") { PlaylistManager.use { it.duplicateItemsByOne(pig.items) } },
            menuItem("Explore items's directory") { Environment.browse(pig.items.stream().filter { it.isFileBased() }.map { it.getFile() }) },
            menuItem("Add items to library") { APP.db.addItems(pig.items.map { it.toMeta() }) },
            menuWithItems(
                    "Search album cover",
                    App.APP.instances.getInstances<SearchUriBuilder>().stream(),
                    { "in ${it.name}" },
                    { APP.actions.itemToMeta(pig.items[0]) { i -> Environment.browse(it.apply(i.getAlbumOrEmpty())) } }
            )
    )}
    add<Thumbnail.ContextMenuData> { contextMenu, cmd -> notNullSeqOf(
            if (cmd.image==null) null
            else Menu("Image", null,
                        menuItem("Save image as ...") {
                            saveFile("Save image as...", App.APP.DIR_APP, cmd.iFile?.name ?: "new_image", contextMenu.ownerWindow, ImageFileFormat.filter())
                                    .ifOk { writeImage(cmd.image, it) }
                        },
                        menuItem("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, cmd.image) }
                ),
            if (cmd.fsDisabled) null
            else Menu("Image file", null,
                    menuItem("Browse location") { Environment.browse(cmd.fsImageFile) },
                    menuItem("Open (in associated program)") { Environment.open(cmd.fsImageFile) },
                    menuItem("Edit (in associated editor)") { Environment.edit(cmd.fsImageFile) },
                    menuItem("Delete from disc") { recycleFile(cmd.fsImageFile) },
                    menuItem("Fullscreen") {
                        val f = cmd.fsImageFile
                        if (ImageFileFormat.isSupported(f)) {
                            val screen = getScreen(contextMenu.x, contextMenu.y)
                            APP.actions.openImageFullscreen(f, screen)
                        }
                    }
            ),
            if (cmd.representant==null) null else menuOfItemsFor(contextMenu, cmd.representant)
    )}

}

private fun <T: Any> notNullSeqOf(vararg elements: T?) = seqOf(*elements).filterNotNull()

private fun menu(text: String, items: Sequence<MenuItem> = seqOf()) = Menu(text, null, *items.toList().toTypedArray())

private fun <A> menuWithItems(text: String, from: Stream<A>, toStr: (A) -> String, action: (A) -> Unit) = menu(text, items(from, toStr, action).asSequence())

private fun <A> items(from: Stream<A>, toStr: (A) -> String, action: (A) -> Unit) = from.map { menuItem(toStr(it)) { e -> action(it) } }

private fun menuOfItemsFor(contextMenu: ImprovedContextMenu<*>, menuName: String, value: Any?) = menu(menuName, CONTEXT_MENUS[contextMenu, value])

private fun menuOfItemsFor(contextMenu: ImprovedContextMenu<*>, value: Any?): Menu {
    val menuName = App.APP.className.get(value?.javaClass ?: Void::class.java)
    return menuOfItemsFor(contextMenu, menuName, value)
}