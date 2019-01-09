package sp.it.pl.core

import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.DataFormat
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistItemGroup
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
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.writeImage
import sp.it.pl.util.functional.asArray
import sp.it.pl.util.functional.runIf
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.item
import sp.it.pl.util.graphics.items
import sp.it.pl.util.graphics.menu
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
        add<Any> { contextMenu, o ->
            menuItems(
                    item("Show detail") { APP.actionPane.show(o) },
                    menu("Examine in") {
                        widgetItems<Opener> { it.open(o) }
                    },
                    runIf(APP.developerMode) {
                        menu("Public methods") {
                            items(
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
                    }
            )
        }
        add<File> { contextMenu, file ->
            menuItems(
                    item("Browse location") { file.browse() },
                    item("Open (in associated program)") { file.open() },
                    item("Edit (in associated editor)") { file.edit() },
                    item("Delete from disc") { file.recycle() },
                    item("Copy as ...") {
                        saveFile("Copy as...", APP.DIR_APP, file.name, contextMenu.ownerWindow, ImageFileFormat.filter())
                                .ifOk { nf ->
                                    // TODO: use customization popup
                                    val success = file.copyRecursively(nf, false) { f, e ->
                                        logger.error(e) { "File copy failed" }
                                        OnErrorAction.SKIP
                                    }
                                    if (!success) APP.messagePane.show("File $file copy failed")
                                }
                    }
            )
        }
        addMany<File> { contextMenu, files ->
            menuItems(
                    item("Copy") { copyToSysClipboard(DataFormat.FILES, files) },
                    item("Explore in browser") { APP.actions.browseMultipleFiles(files.asSequence()) }
            )
        }
        add<MetadataGroup> { contextMenu, mg ->
            menuItems(
                    item("Play items") { PlaylistManager.use { it.setNplay(mg.grouped.stream().sorted(APP.db.libraryComparator.get())) } },
                    item("Enqueue items") { PlaylistManager.use { it.addItems(mg.grouped) } },
                    item("Update items from file") { APP.db.refreshItemsFromFile(mg.grouped) },
                    item("Remove items from library") { APP.db.removeItems(mg.grouped) },
                    menu("Show in") {
                        widgetItems<SongReader> { it.read(mg.grouped) }
                    },
                    menu("Edit tags in") {
                        widgetItems<SongWriter> { it.read(mg.grouped) }
                    },
                    item("Explore items's location") { APP.actions.browseMultipleFiles(mg.grouped.asSequence().mapNotNull { it.getFile() }) },
                    menu("Explore items' location in") {
                        widgetItems<FileExplorerFeature> { it.exploreCommonFileOf(mg.grouped.mapNotNull { it.getFile() }) }
                    },
                    runIf(mg.field==Metadata.Field.ALBUM) {
                        menu("Search cover in") {
                            items(
                                    APP.instances.getInstances<SearchUriBuilder>(),
                                    { "in ${it.name}" },
                                    { it(mg.getValueS("<none>")).browse() }
                            )
                        }
                    }
            )
        }
        add<PlaylistItemGroup> { contextMenu, pig ->
            menuItems(
                    item("Play items") { PlaylistManager.use { it.playItem(pig.items[0]) } },
                    item("Remove items") { PlaylistManager.use { it.removeAll(pig.items) } },
                    menu("Show in") {
                        widgetItems<SongReader> { it.read(pig.items) }
                    },
                    menu("Edit tags in") {
                        widgetItems<SongWriter> { it.read(pig.items) }
                    },
                    item("Crop items") { PlaylistManager.use { it.retainAll(pig.items) } },
                    item("Duplicate items as group") { PlaylistManager.use { it.duplicateItemsAsGroup(pig.items) } },
                    item("Duplicate items individually") { PlaylistManager.use { it.duplicateItemsByOne(pig.items) } },
                    item("Explore items's directory") { APP.actions.browseMultipleFiles(pig.items.asSequence().mapNotNull { it.getFile() }) },
                    item("Add items to library") { APP.db.addItems(pig.items.map { it.toMeta() }) },
                    menu("Search album cover") {
                        items(
                                APP.instances.getInstances<SearchUriBuilder>(),
                                { "in ${it.name}" },
                                { APP.db.itemToMeta(pig.items[0]) { i -> it(i.getAlbumOrEmpty()).browse() } }
                        )
                    }
            )
        }
        add<Thumbnail.ContextMenuData> { contextMenu, cmd ->
            menuItems(
                    runIf(cmd.image!=null) {
                        menu("Image") {
                            item("Save image as ...") {
                                saveFile("Save image as...", APP.DIR_APP, cmd.iFile?.name
                                        ?: "new_image", contextMenu.ownerWindow, ImageFileFormat.filter())
                                        .ifOk { writeImage(cmd.image, it) }
                            }
                            item("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, cmd.image) }
                        }
                    },
                    runIf(!cmd.fsDisabled) {
                        menu("Image file") {
                            item("Browse location") { cmd.fsImageFile.browse() }
                            item("Open (in associated program)") { cmd.fsImageFile.open() }
                            item("Edit (in associated editor)") { cmd.fsImageFile.edit() }
                            item("Delete from disc") { cmd.fsImageFile.recycle() }
                            item("Fullscreen") {
                                val f = cmd.fsImageFile
                                if (ImageFileFormat.isSupported(f)) {
                                    APP.actions.openImageFullscreen(f)
                                }
                            }
                        }
                    },
                    runIf(!cmd.fsDisabled) {
                        menu("Image file") {
                            item("Browse location") { cmd.fsImageFile.browse() }
                            item("Open (in associated program)") { cmd.fsImageFile.open() }
                            item("Edit (in associated editor)") { cmd.fsImageFile.edit() }
                            item("Delete from disc") { cmd.fsImageFile.recycle() }
                            item("Fullscreen") {
                                val f = cmd.fsImageFile
                                if (ImageFileFormat.isSupported(f)) {
                                    APP.actions.openImageFullscreen(f)
                                }
                            }
                        }
                    },
                    runIf(cmd.representant!=null) {
                        menuOfItemsFor(contextMenu, cmd.representant)
                    }
            )
        }

    }

    companion object {

        /** DSL for creating menu. */
        private fun <T: MenuItem> menuItems(vararg elements: T?) = seqOf(*elements).filterNotNull().filterNot { it is Menu && it.items.isEmpty() }

        /** DSL for creating menu. */
        private inline fun <reified W> Menu.widgetItems(crossinline action: (W) -> Unit) = items(
                APP.widgetManager.factories.getFactoriesWith<W>(),
                { it.nameGui() },
                { it.use(WidgetSource.NO_LAYOUT) { action(it) } }
        )

        private fun menuOfItemsFor(contextMenu: ImprovedContextMenu<*>, value: Any?): Menu {
            val menuName = APP.className.get(value?.javaClass ?: Void::class.java)
            return menuOfItemsFor(contextMenu, menuName, value)
        }

        private fun menuOfItemsFor(contextMenu: ImprovedContextMenu<*>, menuName: String, value: Any?) = menu(menuName, contextMenuItemBuilders[contextMenu, value])

        private fun menu(text: String, items: Sequence<MenuItem> = seqOf()) = Menu(text, null, *items.asArray())

    }

}