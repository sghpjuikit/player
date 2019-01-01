package sp.it.pl.core

import javafx.scene.Node
import javafx.scene.control.Menu
import javafx.scene.input.DataFormat
import sp.it.pl.audio.SimpleItem
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistItemGroup
import sp.it.pl.gui.objects.contextmenu.ContextMenuBuilder
import sp.it.pl.gui.objects.contextmenu.ContextMenuItemSuppliers
import sp.it.pl.gui.objects.contextmenu.ImprovedContextMenu
import sp.it.pl.gui.objects.contextmenu.contextMenuItemBuilders
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.WidgetSource
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.browseMultipleFiles
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.writeImage
import sp.it.pl.util.file.isPlayable
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

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class CoreMenus: Core {

    /** Menu item builders registered per class. */
    @JvmField val menuItemBuilders = contextMenuItemBuilders

    override fun init() {
        menuItemBuilders.init()
    }

    override fun dispose() {}

    private fun ContextMenuItemSuppliers.init() = apply {
        add<Any> {
            menuItem("Show detail") { APP.actionPane.show(selected) }
            widgetMenu<Opener>("Examine in") { it.open(selected) }
            if (APP.developerMode)
                menu("Public methods") {
                    items(getAllMethods(selected::class.java).asSequence()
                            .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
                            .filter { it.parameterCount==0 && it.returnType==Void.TYPE },
                            { it.name },
                            {
                                try {
                                    it(selected)
                                } catch (e: IllegalAccessException) {
                                    logger.error(e) { "Could not invoke method $it on object $selected" }
                                } catch (e: InvocationTargetException) {
                                    logger.error(e) { "Could not invoke method $it on object $selected" }
                                }
                            }
                    )
                }
        }
        add<File> {
            if (selected.isPlayable) {
                menuItem("Play") { PlaylistManager.use { it.playUri(selected.toURI()) } }
                menuItem("Enqueue") { PlaylistManager.use { it.addFile(selected) } }
            }
            menuItem("Browse location") { selected.browse() }
            menuItem("Open (in associated program)") { selected.open() }
            menuItem("Edit (in associated editor)") { selected.edit() }
            menuItem("Delete from disc") { selected.recycle() }
            menuItem("Copy as ...") {
                saveFile("Copy as...", APP.DIR_APP, selected.name, contextMenu.ownerWindow, ImageFileFormat.filter())
                        .ifOk { nf ->
                            // TODO: use customization popup
                            val success = selected.copyRecursively(nf, false) { f, e ->
                                logger.error(e) { "File copy failed" }
                                OnErrorAction.SKIP
                            }
                            if (!success) APP.messagePane.show("File $selected copy failed")
                        }
            }
        }
        addMany<File> {
            if (selected.all { it.isPlayable }) {
                menuItem("Play") { PlaylistManager.use { it.setNplay(selected.map { SimpleItem(it) }) } }
                menuItem("Enqueue") { PlaylistManager.use { it.addFiles(selected) } }
            }
            menuItem("Copy") { copyToSysClipboard(DataFormat.FILES, selected) }
            menuItem("Explore in browser") { browseMultipleFiles(selected.asSequence()) }
        }
        add<MetadataGroup> {
            menuItem("Play items") { PlaylistManager.use { it.setNplay(selected.grouped.stream().sorted(APP.db.libraryComparator.get())) } }
            menuItem("Enqueue items") { PlaylistManager.use { it.addItems(selected.grouped) } }
            menuItem("Update items from file") { APP.db.refreshItemsFromFile(selected.grouped) }
            menuItem("Remove items from library") { APP.db.removeItems(selected.grouped) }
            widgetMenu<SongReader>("Show in") { it.read(selected.grouped) }
            widgetMenu<SongWriter>("Edit tags in") { it.read(selected.grouped) }
            menuItem("Explore items's location") { browseMultipleFiles(selected.grouped.asSequence().mapNotNull { it.getFile() }) }
            widgetMenu<FileExplorerFeature>("Explore items' location in") { it.exploreCommonFileOf(selected.grouped.mapNotNull { it.getFile() }) }
            if (selected.field==Metadata.Field.ALBUM)
                menu("Search cover in") {
                    items(APP.instances.getInstances<SearchUriBuilder>(),
                            { "in ${it.name}" },
                            { it(selected.getValueS("<none>")).browse() })
                }
        }
        add<PlaylistItemGroup> {
            menuItem("Play items") { PlaylistManager.use { it.playItem(selected.items[0]) } }
            menuItem("Remove items") { PlaylistManager.use { it.removeAll(selected.items) } }
            widgetMenu<SongReader>("Show in") { it.read(selected.items) }
            widgetMenu<SongWriter>("Edit tags in") { it.read(selected.items) }
            menuItem("Crop items") { PlaylistManager.use { it.retainAll(selected.items) } }
            menuItem("Duplicate items as group") { PlaylistManager.use { it.duplicateItemsAsGroup(selected.items) } }
            menuItem("Duplicate items individually") { PlaylistManager.use { it.duplicateItemsByOne(selected.items) } }
            menuItem("Explore items's directory") { browseMultipleFiles(selected.items.asSequence().mapNotNull { it.getFile() }) }
            menuItem("Add items to library") { APP.db.addItems(selected.items.map { it.toMeta() }) }
            menu("Search album cover") {
                items(APP.instances.getInstances<SearchUriBuilder>(),
                        { "in ${it.name}" },
                        { APP.db.itemToMeta(selected.items[0]) { i -> it(i.getAlbumOrEmpty()).browse() } })
            }
        }
        add<Thumbnail.ContextMenuData> {
            if (selected.image!=null)
                menu("Image") {
                    item("Save image as ...") {
                        saveFile("Save image as...", APP.DIR_APP, selected.iFile?.name ?: "new_image",
                                contextMenu.ownerWindow, ImageFileFormat.filter())
                                .ifOk { writeImage(selected.image, it) }
                    }
                    item("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, selected.image) }
                }
            if (!selected.fsDisabled)
                menu("Image file") {
                    item("Browse location") { selected.fsImageFile.browse() }
                    item("Open (in associated program)") { selected.fsImageFile.open() }
                    item("Edit (in associated editor)") { selected.fsImageFile.edit() }
                    item("Delete from disc") { selected.fsImageFile.recycle() }
                    item("Fullscreen") {
                        val f = selected.fsImageFile
                        if (ImageFileFormat.isSupported(f)) {
                            APP.actions.openImageFullscreen(f)
                        }
                    }
                }
            if (selected.representant!=null)
                addMenuOfItemsFor(contextMenu, selected.representant)
        }

    }

    private inline fun Menu.menu(text: String, graphics: Node? = null, then: (Menu).() -> Unit) {
        items += Menu(text, graphics).apply { then() }
    }

    private inline fun Menu.item(text: String, crossinline action: () -> Unit) {
        items += menuItem(text) { action() }
    }

    private fun <A> Menu.items(from: Sequence<A>, toStr: (A) -> String, action: (A) -> Unit) {
        items += from.map { menuItem(toStr(it)) { e -> action(it) } }.sortedBy { it.text }
    }

    private inline fun <reified W> Menu.widgetItems(crossinline action: (W) -> Unit) =
            items(APP.widgetManager.factories.getFactoriesWith<W>(),
                    { it.nameGui() },
                    { it.use(WidgetSource.NO_LAYOUT) { action(it) } })

    private inline fun <reified W> ContextMenuBuilder<*>.widgetMenu(text: String, crossinline action: (W) -> Unit) =
            menu(text).widgetItems(action)

    private fun ContextMenuBuilder<*>.addMenuOfItemsFor(contextMenu: ImprovedContextMenu<*>, value: Any?) {
        val menuName = APP.className.get(value?.javaClass ?: Void::class.java)
        addMenuOfItemsFor(contextMenu, menuName, value)
    }

    private fun ContextMenuBuilder<*>.addMenuOfItemsFor(contextMenu: ImprovedContextMenu<*>, menuName: String, value: Any?) =
            menu(menuName, contextMenuItemBuilders[contextMenu, value])

}