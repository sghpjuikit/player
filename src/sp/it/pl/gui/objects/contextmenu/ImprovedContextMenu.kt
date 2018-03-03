@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package sp.it.pl.gui.objects.contextmenu

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.ContextMenuEvent
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseEvent
import mu.KotlinLogging
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata.Field
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistItemGroup
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.WidgetSource.NO_LAYOUT
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.feature.SongWriter
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.browseMultipleFiles
import sp.it.pl.util.access.AccessibleValue
import sp.it.pl.util.collections.map.ClassListMap
import sp.it.pl.util.dev.fail
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.writeImage
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.getElementType
import sp.it.pl.util.functional.runIf
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.Util.menuItem
import sp.it.pl.util.system.browse
import sp.it.pl.util.system.copyToSysClipboard
import sp.it.pl.util.system.edit
import sp.it.pl.util.system.open
import sp.it.pl.util.system.recycle
import sp.it.pl.util.system.saveFile
import sp.it.pl.util.type.Util.getAllMethods
import sp.it.pl.web.SearchUriBuilder
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

private typealias ItemsSupply = (ImprovedContextMenu<*>, Any?) -> Sequence<MenuItem>

val logger = KotlinLogging.logger { }

/**
 * Context menu wrapping a value - usually an object set before showing, for menu items' action. It can then generate
 * the items based on the value from supported actions.
 *
 * Usually [ValueContextMenu] is better choice.
 */
open class ImprovedContextMenu<E: Any?>: ContextMenu(), AccessibleValue<E> {

    protected var v: E? = null

    init {
        consumeAutoHidingEvents = false
    }

    @Suppress("UNCHECKED_CAST")
    override fun getValue(): E = v as E

    override fun setValue(value: E) {
        v = value
    }

    /** Convenience for [setValue] & [setItemsForValue]. */
    open fun setValueAndItems(value: E) {
        setValue(value)
        setItemsForValue()
    }

    override fun show(n: Node, screenX: Double, screenY: Double) = show(n.scene.window, screenX, screenY)

    /**
     * Shows the context menu for node at proper coordinates of the event.
     *
     * Prefer this method to show context menu (especially in MouseClick handler), because when showing ContextMenu,
     * there is a difference between show(Window,x,y) and (Node,x,y). The former will not hide the menu when next click
     * happens within the node itself! This method avoids that.
     */
    fun show(n: Node, e: MouseEvent) = show(n.scene.window, e.screenX, e.screenY)

    fun show(n: Node, e: ContextMenuEvent) = show(n.scene.window, e.screenX, e.screenY)

    /**
     * Add menu items for specified value or current value if none is specified. Previous items are not removed.
     *
     * Usually [setItemsForValue] is better choice.
     */
    fun addItemsForValue(value: E? = v) {
        items += CONTEXT_MENUS[this, value]
    }

    /**
     * Clear and add menu items for specified value or current value if none is specified. Previous items are removed.
     */
    fun setItemsForValue(value: E? = v) {
        items.clear()
        addItemsForValue(value)
    }

}

/**
 * Generic [ImprovedContextMenu], which supports collection unwrapping in [setValue] (empty collection will be handled
 * as null and collection with one element handled as that one element). This is convenient for multi-select controls.
 */
class ValueContextMenu: ImprovedContextMenu<Any?>() {

    override fun setValue(value: Any?) {
        v = when (value) {
            is Collection<*> -> {
                when (value.size) {
                    0 -> null
                    1 -> value.firstOrNull()
                    else -> value
                }
            }
            else -> value
        }
    }

}

class ContextMenuItemSuppliers {
    private val mSingle = ClassListMap<ItemsSupply> { fail() }
    private val mMany = ClassListMap<ItemsSupply> { fail() }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> add(type: Class<T>, items: (ImprovedContextMenu<*>, T) -> Sequence<MenuItem>) {
        mSingle.accumulate(type, items as ItemsSupply)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T: Any> addMany(type: Class<T>, items: (ImprovedContextMenu<*>, Collection<T>) -> Sequence<MenuItem>) {
        mMany.accumulate(type, items as ItemsSupply)
    }

    inline fun <reified T: Any> add(noinline items: (ImprovedContextMenu<*>, T) -> Sequence<MenuItem>) {
        add(T::class.java, items)
    }

    inline fun <reified T: Any> addMany(noinline items: (ImprovedContextMenu<*>, Collection<T>) -> Sequence<MenuItem>) {
        addMany(T::class.java, items)
    }

    operator fun get(contextMenu: ImprovedContextMenu<*>, value: Any?): Sequence<MenuItem> {
        val items1 = mSingle.getElementsOfSuperV(value?.javaClass ?: Void::class.java).asSequence()
                .map { it(contextMenu, value) }
                .flatMap { it }
        val itemsN = if (value is Collection<*>) {
            mMany.getElementsOfSuperV(value.getElementType()).asSequence()
                    .map { it(contextMenu, contextMenu.value) }
                    .flatMap { it }
        } else {
            sequenceOf()
        }
        return items1 + itemsN
    }

}

val CONTEXT_MENUS = ContextMenuItemSuppliers().apply {
    add<Any> { contextMenu, o -> menuItems(
            menuItem("Show detail") { APP.actionPane.show(o) },
            menuWithWidgetItems<Opener>("Examine in") { println("" + o::class + " " + o); it.open(o) },
            runIf(APP.developerMode) {
                menuWithItems(
                        "Public methods",
                        getAllMethods(o::class.java).asSequence()
                                .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
                                .filter { it.parameterCount==0 && it.returnType==Void.TYPE },
                        { it.name },
                        {
                            try {
                                it(o)
                            } catch (e: IllegalAccessException) {
                                logger.error(e) { "Could not invoke method $it on object $o" }
                            } catch (e: InvocationTargetException) {
                                logger.error(e) { "Could not invoke method $it on object $o" }
                            }
                        }
                )
            }
    )}
    add<File> { contextMenu, file -> menuItems(
            menuItem("Browse location") { file.browse() },
            menuItem("Open (in associated program)") { file.open() },
            menuItem("Edit (in associated editor)") { file.edit() },
            menuItem("Delete from disc") { file.recycle() },
            menuItem("Copy as ...") {
                saveFile("Copy as...", APP.DIR_APP, file.name, contextMenu.ownerWindow, ImageFileFormat.filter())
                        .ifOk { nf ->
                            // TODO: use customization popup
                            val success = file.copyRecursively(nf, false) { f,e ->
                                logger.error(e) { "File copy failed" }
                                OnErrorAction.SKIP
                            }
                            if (!success) APP.messagePane.show("File $file copy failed")
                        }
            }
    )}
    addMany<File> { contextMenu, files -> menuItems(
            menuItem("Copy") { copyToSysClipboard(DataFormat.FILES, files) },
            menuItem("Explore in browser") { browseMultipleFiles(files.asSequence()) }
    )}
    add<MetadataGroup> { contextMenu, mg -> menuItems(
            menuItem("Play items") { PlaylistManager.use { it.setNplay(mg.grouped.stream().sorted(APP.db.libraryComparator.get())) } },
            menuItem("Enqueue items") { PlaylistManager.use { it.addItems(mg.grouped) } },
            menuItem("Update items from file") { APP.actions.refreshItemsFromFileJob(mg.grouped) },
            menuItem("Remove items from library") { APP.db.removeItems(mg.grouped) },
            menuWithWidgetItems<SongReader>("Show in") { it.read(mg.grouped) },
            menuWithWidgetItems<SongWriter>("Edit tags in") { it.read(mg.grouped) },
            menuItem("Explore items's directory") { browseMultipleFiles(mg.grouped.asSequence().filter { it.isFileBased() }.map { it.getFile() }) },
            menuWithWidgetItems<FileExplorerFeature>("Explore items' directory in") { it.exploreFile(mg.grouped[0].getFile()) },
            runIf(mg.field==Field.ALBUM) {
                menuWithItems(
                        "Search cover in",
                        APP.instances.getInstances<SearchUriBuilder>(),
                        { "in ${it.name}" },
                        { it(mg.getValueS("<none>")).browse() }
                )
            }
    )}
    add<PlaylistItemGroup> { contextMenu, pig -> menuItems(
            menuItem("Play items") { PlaylistManager.use { it.playItem(pig.items[0]) } },
            menuItem("Remove items") { PlaylistManager.use { it.removeAll(pig.items) } },
            menuWithWidgetItems<SongReader>("Show in") { it.read(pig.items) },
            menuWithWidgetItems<SongWriter>("Edit tags in") { it.read(pig.items) },
            menuItem("Crop items") { PlaylistManager.use { it.retainAll(pig.items) } },
            menuItem("Duplicate items as group") { PlaylistManager.use { it.duplicateItemsAsGroup(pig.items) } },
            menuItem("Duplicate items individually") { PlaylistManager.use { it.duplicateItemsByOne(pig.items) } },
            menuItem("Explore items's directory") { browseMultipleFiles(pig.items.asSequence().filter { it.isFileBased() }.map { it.getFile() }) },
            menuItem("Add items to library") { APP.db.addItems(pig.items.map { it.toMeta() }) },
            menuWithItems(
                    "Search album cover",
                    APP.instances.getInstances<SearchUriBuilder>(),
                    { "in ${it.name}" },
                    { APP.actions.itemToMeta(pig.items[0]) { i -> it(i.getAlbumOrEmpty()).browse() } }
            )
    )}
    add<Thumbnail.ContextMenuData> { contextMenu, cmd -> menuItems(
            runIf(cmd.image!=null) {
                Menu("Image", null,
                        menuItem("Save image as ...") {
                            saveFile("Save image as...", APP.DIR_APP, cmd.iFile?.name ?: "new_image", contextMenu.ownerWindow, ImageFileFormat.filter())
                                    .ifOk { writeImage(cmd.image, it) }
                        },
                        menuItem("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, cmd.image) }
                )
            },
            runIf(!cmd.fsDisabled) {
                Menu("Image file", null,
                        menuItem("Browse location") { cmd.fsImageFile.browse() },
                        menuItem("Open (in associated program)") { cmd.fsImageFile.open() },
                        menuItem("Edit (in associated editor)") { cmd.fsImageFile.edit() },
                        menuItem("Delete from disc") { cmd.fsImageFile.recycle() },
                        menuItem("Fullscreen") {
                            val f = cmd.fsImageFile
                            if (ImageFileFormat.isSupported(f)) {
                                APP.actions.openImageFullscreen(f)
                            }
                        }
                )
            },
            runIf(cmd.representant!=null) {
                menuOfItemsFor(contextMenu, cmd.representant)
            }
    )}

}

private fun <T: MenuItem> menuItems(vararg elements: T?) = seqOf(*elements).filterNotNull().filterNot { it is Menu && it.items.isEmpty() }

private fun menu(text: String, items: Sequence<MenuItem> = seqOf()) = Menu(text, null, *items.asArray())

private fun <A> menuWithItems(text: String, from: Sequence<A>, toStr: (A) -> String, action: (A) -> Unit) = menu(text, items(from, toStr, action))

private fun <A> items(from: Sequence<A>, toStr: (A) -> String, action: (A) -> Unit) = from.map { menuItem(toStr(it)) { e -> action(it) } }

private fun menuOfItemsFor(contextMenu: ImprovedContextMenu<*>, menuName: String, value: Any?) = menu(menuName, CONTEXT_MENUS[contextMenu, value])

private fun menuOfItemsFor(contextMenu: ImprovedContextMenu<*>, value: Any?): Menu {
    val menuName = APP.className.get(value?.javaClass ?: Void::class.java)
    return menuOfItemsFor(contextMenu, menuName, value)
}

private inline fun <reified W> menuWithWidgetItems(name: String, crossinline action: (W) -> Unit) = menuWithItems(
        name,
        APP.widgetManager.factories.getFactoriesWith<W>(),
        { it.nameGui() },
        { it.use(NO_LAYOUT) { action(it) } }
)