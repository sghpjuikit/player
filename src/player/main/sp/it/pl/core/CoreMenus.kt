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
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.configure
import sp.it.pl.main.imageWriteExtensionFilter
import sp.it.pl.main.isAudio
import sp.it.pl.main.isImage
import sp.it.pl.main.writeImage
import sp.it.pl.web.SearchUriBuilder
import sp.it.util.async.runNew
import sp.it.util.conf.Configurable
import sp.it.util.conf.Configurable.configsFromFxPropertiesOf
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.IsConfig
import sp.it.util.conf.cv
import sp.it.util.conf.only
import sp.it.util.file.div
import sp.it.util.functional.ifFalse
import sp.it.util.system.browse
import sp.it.util.system.copyToSysClipboard
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.system.saveFile
import sp.it.util.ui.item
import sp.it.util.ui.items
import sp.it.util.ui.separator
import sp.it.util.validation.Constraint.FileActor.DIRECTORY
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import javafx.stage.Window as WindowFX

object CoreMenus: Core {

    /** Menu item builders registered per class. */
    val menuItemBuilders = contextMenuGenerator

    override fun init() {
        menuItemBuilders.apply {
            addNull {
                menu("Inspect in") {
                    item("Object viewer") { APP.actionPane.show(selected) }
                    separator()
                    widgetItems<Opener> { it.open(selected) }
                }
            }
            add<Any> {
                menu("Inspect in") {
                    item("Object viewer") { APP.actionPane.show(selected) }
                    separator()
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
                if (selected.isAudio()) {
                    item("Play") { PlaylistManager.use { it.playUri(selected.toURI()) } }
                    item("Enqueue") { PlaylistManager.use { it.addFile(selected) } }
                }
                if (selected.isImage()) {
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
                        items(APP.instances.getInstances<SearchUriBuilder>().asSequence(),
                                { "in ${it.name}" },
                                { it(selected.getValueS("<none>")).browse() })
                    }
            }
            add<PlaylistSongGroup> {
                item("Play songs") { selected.playlist.playItem(selected.songs[0]) }
                item("Remove songs") { selected.playlist.removeAll(selected.songs) }
                menu("Show in") {
                    widgetItems<SongReader> { it.read(selected.songs) }
                }
                menu("Edit tags in") {
                    widgetItems<SongWriter> { it.read(selected.songs) }
                }
                item("Crop") { selected.playlist.retainAll(selected.songs) }
                menu("Duplicate") {
                    item("as group") { selected.playlist.duplicateItemsAsGroup(selected.songs) }
                    item("individually") { selected.playlist.duplicateItemsByOne(selected.songs) }
                }
                item("Explore directory") { APP.actions.browseMultipleFiles(selected.songs.asSequence().mapNotNull { it.getFile() }) }
                menu("Search album cover") {
                    items(APP.instances.getInstances<SearchUriBuilder>().asSequence(),
                            { "in ${it.name}" },
                            { APP.db.songToMeta(selected.songs[0]) { i -> it(i.getAlbumOrEmpty()).browse() } })
                }
            }
            add<Thumbnail.ContextMenuData> {
                if (selected.image!=null)
                    menu("Cover") {
                        item("Save image as ...") {
                            saveFile("Save image as...", APP.DIR_APP, selected.iFile?.name ?: "new_image", contextMenu.ownerWindow, imageWriteExtensionFilter()).ifOk {
                                writeImage(selected.image, it).ifError { e ->
                                    APP.messagePane.show("Saving image $it failed\n\nReason: ${e.message}")
                                }
                            }
                        }
                        item("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, selected.image) }
                    }
                if (!selected.fsDisabled && selected.iFile!=selected.representant)
                    menuFor(contextMenu, "Cover file", selected.fsImageFile)
                if (selected.representant!=null)
                    menuFor(contextMenu, selected.representant)
            }
            add<Input<*>> {
                menuFor(contextMenu, "Value", selected.value)
                menu("Link") {
                    item("All identical") { selected.bindAllIdentical() }
                }
                menu("Unlink") {
                    item("All inbound") { selected.unbindAll() }
                }
            }
            add<Output<*>> {
                menuFor(contextMenu, "Value", selected.value)
                menu("Unlink") {
                    item("All outbound") { selected.unbindAll() }
                }
            }
            add<InOutput<*>> {
                menuFor(contextMenu, "Value", selected.o.value)
                menu("Link") {
                    item("All identical") { selected.i.bindAllIdentical() }
                }
                menu("Unlink") {
                    item("All") { selected.i.unbindAll(); selected.o.unbindAll() }
                    item("All inbound") { selected.i.unbindAll() }
                    item("All outbound") { selected.o.unbindAll() }
                }
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
            items = menuItemBuilders[contextMenu, value]
    )

}