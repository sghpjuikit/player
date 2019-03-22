package sp.it.pl.core

import javafx.scene.Node
import javafx.scene.control.ContextMenu
import javafx.scene.control.Menu
import javafx.scene.input.DataFormat
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.gui.objects.contextmenu.ContextMenuGenerator
import sp.it.pl.gui.objects.contextmenu.contextMenuGenerator
import sp.it.pl.gui.objects.contextmenu.item
import sp.it.pl.gui.objects.contextmenu.menu
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.configure
import sp.it.pl.util.async.runNew
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.Configurable.configsFromFxPropertiesOf
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
import javafx.stage.Window as WindowFX

object CoreMenus: Core {

    /** Menu item builders registered per class. */
    val menuItemBuilders = contextMenuGenerator

    override fun init() {
        menuItemBuilders.apply {
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
                if (ImageFileFormat.isSupported(selected)) {
                    item("Fullscreen") {
                        APP.actions.openImageFullscreen(selected)
                    }
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
            add<Node> {
                menu("Inspect ui properties in") {
                    widgetItems<ConfiguringFeature> { w ->
                        runNew {
                            configsFromFxPropertiesOf(selected)
                        } ui {
                            w.configure(it)
                        }
                    }
                }
            }
            add<WindowFX> {
                menu("Inspect ui properties in") {
                    widgetItems<ConfiguringFeature> { w ->
                        runNew {
                            configsFromFxPropertiesOf(selected)
                        } ui {
                            w.configure(it)
                        }
                    }
                }
            }
            add<Configurable<*>> {
                menu("Inspect properties in") {
                    widgetItems<ConfiguringFeature> { it.configure(selected) }
                }
            }
            addMany<File> {
                item("Copy (to clipboard)") { copyToSysClipboard(DataFormat.FILES, selected) }
                item("Browse location") { APP.actions.browseMultipleFiles(selected.asSequence()) }
            }
            add<MetadataGroup> {
                item("Play songs") { PlaylistManager.use { it.setNplay(selected.grouped.stream().sorted(APP.db.libraryComparator.get())) } }
                item("Enqueue songs") { PlaylistManager.use { it.addItems(selected.grouped) } }
                item("Update songs from file") { APP.db.refreshSongsFromFile(selected.grouped) }
                item("Remove songs from library") { APP.db.removeSongs(selected.grouped) }
                menu("Show in") {
                    widgetItems<SongReader> { it.read(selected.grouped) }
                }
                menu("Edit tags in") {
                    widgetItems<SongWriter> { it.read(selected.grouped) }
                }
                item("Explore songs' location") { APP.actions.browseMultipleFiles(selected.grouped.asSequence().mapNotNull { it.getFile() }) }
                menu("Explore songs' location in") {
                    widgetItems<FileExplorerFeature> { it.exploreCommonFileOf(selected.grouped.mapNotNull { it.getFile() }) }
                }
                if (selected.field==Metadata.Field.ALBUM)
                    menu("Search cover in") {
                        items(APP.instances.getInstances<SearchUriBuilder>(),
                                { "in ${it.name}" },
                                { it(selected.getValueS("<none>")).browse() })
                    }
            }
            add<PlaylistSongGroup> {
                item("Play songs") { PlaylistManager.use { it.playItem(selected.songs[0]) } }
                item("Remove songs") { PlaylistManager.use { it.removeAll(selected.songs) } }
                menu("Show in") {
                    widgetItems<SongReader> { it.read(selected.songs) }
                }
                menu("Edit tags in") {
                    widgetItems<SongWriter> { it.read(selected.songs) }
                }
                item("Crop") { PlaylistManager.use { it.retainAll(selected.songs) } }
                menu("Duplicate") {
                    item("as group") { PlaylistManager.use { it.duplicateItemsAsGroup(selected.songs) } }
                    item("individually") { PlaylistManager.use { it.duplicateItemsByOne(selected.songs) } }
                }
                item("Explore directory") { APP.actions.browseMultipleFiles(selected.songs.asSequence().mapNotNull { it.getFile() }) }
                menu("Search album cover") {
                    items(APP.instances.getInstances<SearchUriBuilder>(),
                            { "in ${it.name}" },
                            { APP.db.songToMeta(selected.songs[0]) { i -> it(i.getAlbumOrEmpty()).browse() } })
                }
            }
            add<Thumbnail.ContextMenuData> {
                if (selected.image!=null)
                    menu("Cover") {
                        item("Save image as ...") {
                            saveFile("Save image as...", APP.DIR_APP, selected.iFile?.name ?: "new_image",
                                    contextMenu.ownerWindow, ImageFileFormat.filter())
                                    .ifOk { writeImage(selected.image, it) }
                        }
                        item("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, selected.image) }
                    }
                if (!selected.fsDisabled && selected.iFile!=selected.representant)
                    menuFor(contextMenu, "Cover file", selected.fsImageFile)
                if (selected.representant!=null)
                    menuFor(contextMenu, selected.representant)
            }

        }
    }

    override fun dispose() {}

    private inline fun <reified W> Menu.widgetItems(noinline action: (W) -> Unit) = items(
            source = APP.widgetManager.factories.getFactoriesWith<W>(),
            text = { it.nameGui() },
            action = { it.use(NO_LAYOUT) { action(it) } }
    )

    private fun ContextMenuGenerator.Builder<*>.menuFor(contextMenu: ContextMenu, value: Any?) = menuFor(
            contextMenu = contextMenu,
            menuName = APP.className.get(value?.javaClass ?: Void::class.java),
            value = value
    )

    private fun ContextMenuGenerator.Builder<*>.menuFor(contextMenu: ContextMenu, menuName: String, value: Any?) = menu(
            text = menuName,
            items = contextMenuGenerator[contextMenu, value]
    )

}