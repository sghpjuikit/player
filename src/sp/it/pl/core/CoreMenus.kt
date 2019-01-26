package sp.it.pl.core

import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.input.DataFormat
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistItemGroup
import sp.it.pl.gui.objects.contextmenu.ContextMenuGenerator
import sp.it.pl.gui.objects.contextmenu.contextMenuGenerator
import sp.it.pl.gui.objects.contextmenu.item
import sp.it.pl.gui.objects.contextmenu.menu
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.WidgetSource
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.configure
import sp.it.pl.util.conf.ConfigurableBase
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.file.ImageFileFormat
import sp.it.pl.util.file.Util.writeImage
import sp.it.pl.util.file.div
import sp.it.pl.util.file.isPlayable
import sp.it.pl.util.functional.ifFalse
import sp.it.pl.util.graphics.item
import sp.it.pl.util.graphics.items
import sp.it.pl.util.system.browse
import sp.it.pl.util.system.copyToSysClipboard
import sp.it.pl.util.system.edit
import sp.it.pl.util.system.open
import sp.it.pl.util.system.recycle
import sp.it.pl.util.system.saveFile
import sp.it.pl.util.validation.Constraint.FileActor.DIRECTORY
import sp.it.pl.web.SearchUriBuilder
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

class CoreMenus: Core {

    /** Menu item builders registered per class. */
    val menuItemBuilders = contextMenuGenerator

    override fun init() {
        menuItemBuilders.init()
    }

    override fun dispose() {}

    private fun ContextMenuGenerator.init() = apply {
        add<Any> {
            item("Show available actions") { APP.actionPane.show(selected) }
            menu("Show in") {
                widgetItems<Opener> { it.open(selected) }
            }
            if (APP.developerMode)
                menu("Public methods") {
                    items(selected::class.java.methods.asSequence()
                            .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
                            .sortedBy { it.name }
                            .filter { it.parameterCount==0 && (it.returnType==Void::class.javaObjectType || it.returnType==Void::class.javaPrimitiveType || it.returnType==Unit::class.java) },
                            { it.name },
                            {
                                try {
                                    it(selected)
                                } catch (e: IllegalAccessException) {
                                    logger.error(e) { "Could not invoke method $it on object $selected" }
                                } catch (e: InvocationTargetException) {
                                    logger.error(e) { "Could not invoke method $it on object $selected" }
                                }
                            })
                }
        }
        add<File> {
            if (selected.isPlayable) {
                item("Play") { PlaylistManager.use { it.playUri(selected.toURI()) } }
                item("Enqueue") { PlaylistManager.use { it.addFile(selected) } }
            }
            item("Open (in associated program)") { selected.open() }
            item("Edit (in associated editor)") { selected.edit() }
            item("Delete from disc") { selected.recycle() }
            item("Copy as ...") {
                object: ConfigurableBase<Any?>() {
                    @IsConfig(name = "File") val file by cv(APP.DIR_APP).only(DIRECTORY)
                    @IsConfig(name = "Overwrite") val overwrite by cv(false)
                    @IsConfig(name = "On error") val onError by cv(OnErrorAction.SKIP)
                }.configure("Copy as...") {
                    selected.copyRecursively(it.file.value/selected.name, it.overwrite.value) { _, e ->
                        logger.warn(e) { "File copy failed" }
                        it.onError.value
                    }.ifFalse {
                        APP.messagePane.show("File $selected copy failed")
                    }
                }
            }
        }
        addMany<File> {
            item("Copy (to clipboard)") { copyToSysClipboard(DataFormat.FILES, selected) }
            item("Browse location") { APP.actions.browseMultipleFiles(selected.asSequence()) }
        }
        add<MetadataGroup> {
            item("Play items") { PlaylistManager.use { it.setNplay(selected.grouped.stream().sorted(APP.db.libraryComparator.get())) } }
            item("Enqueue items") { PlaylistManager.use { it.addItems(selected.grouped) } }
            item("Update items from file") { APP.db.refreshItemsFromFile(selected.grouped) }
            item("Remove items from library") { APP.db.removeItems(selected.grouped) }
            menu("Show in") {
                widgetItems<SongReader> { it.read(selected.grouped) }
            }
            menu("Edit tags in") {
                widgetItems<SongWriter> { it.read(selected.grouped) }
            }
            item("Explore items's location") { APP.actions.browseMultipleFiles(selected.grouped.asSequence().mapNotNull { it.getFile() }) }
            menu("Explore items' location in") {
                widgetItems<FileExplorerFeature> { it.exploreCommonFileOf(selected.grouped.mapNotNull { it.getFile() }) }
            }
            if (selected.field==Metadata.Field.ALBUM)
                menu("Search cover in") {
                    items(APP.instances.getInstances<SearchUriBuilder>(),
                            { "in ${it.name}" },
                            { it(selected.getValueS("<none>")).browse() })
                }
        }
        add<PlaylistItemGroup> {
            item("Play items") { PlaylistManager.use { it.playItem(selected.items[0]) } }
            item("Remove items") { PlaylistManager.use { it.removeAll(selected.items) } }
            menu("Show in") {
                widgetItems<SongReader> { it.read(selected.items) }
            }
            menu("Edit tags in") {
                widgetItems<SongWriter> { it.read(selected.items) }
            }
            item("Crop items") { PlaylistManager.use { it.retainAll(selected.items) } }
            item("Duplicate items as group") { PlaylistManager.use { it.duplicateItemsAsGroup(selected.items) } }
            item("Duplicate items individually") { PlaylistManager.use { it.duplicateItemsByOne(selected.items) } }
            item("Explore items's directory") { APP.actions.browseMultipleFiles(selected.items.asSequence().mapNotNull { it.getFile() }) }
            item("Add items to library") { APP.db.addItems(selected.items.map { it.toMeta() }) }
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
                menuFor(contextMenu, selected.representant)
        }

    }

    companion object {

        private inline fun <reified W> Menu.widgetItems(crossinline action: (W) -> Unit) =
                items(APP.widgetManager.factories.getFactoriesWith<W>(),
                        { it.nameGui() },
                        { it.use(WidgetSource.NO_LAYOUT) { action(it) } })

        private fun ContextMenuGenerator.Builder<*>.menuFor(contextMenu: ContextMenu, value: Any?) {
            val menuName = APP.className.get(value?.javaClass ?: Void::class.java)
            menuFor(contextMenu, menuName, value)
        }

        private fun ContextMenuGenerator.Builder<*>.menuFor(contextMenu: ContextMenu, menuName: String, value: Any?) =
                menu(menuName, contextMenuGenerator[contextMenu, value])

    }

}