package sp.it.pl.core

import javafx.scene.Node
import javafx.scene.input.DataFormat
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetSource
import sp.it.pl.layout.widget.WidgetUse.ANY
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.widget.controller.io.InOutput
import sp.it.pl.layout.widget.controller.io.Input
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.layout.widget.feature.PlaylistFeature
import sp.it.pl.layout.widget.feature.SongReader
import sp.it.pl.layout.widget.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.AppError
import sp.it.pl.main.AppErrors
import sp.it.pl.main.configure
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.imageWriteExtensionFilter
import sp.it.pl.main.isAudio
import sp.it.pl.main.isImage
import sp.it.pl.main.toMetadata
import sp.it.pl.main.writeImage
import sp.it.pl.web.SearchUriBuilder
import sp.it.util.async.runIO
import sp.it.util.conf.Configurable
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.conf.toConfigurableFx
import sp.it.util.dev.Dsl
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.div
import sp.it.util.file.isParentOf
import sp.it.util.functional.ifFalse
import sp.it.util.functional.runTry
import sp.it.util.system.browse
import sp.it.util.system.copyToSysClipboard
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.system.saveFile
import sp.it.util.ui.ContextMenuGenerator
import sp.it.util.ui.MenuBuilder
import java.io.File
import java.lang.reflect.Modifier
import javafx.stage.Window as WindowFX

object CoreMenus: Core {

   /** Menu item builders registered per class. */
   val menuItemBuilders = ContextMenuGenerator()

   override fun init() {
      menuItemBuilders {
         addNull {
            menu("Inspect in") {
               item("Object viewer") { APP.ui.actionPane.orBuild.show(it) }
               separator()
               widgetItems<Opener> { it.open(value) }
            }
         }
         add<Any> {
            menu("Inspect in") {
               item("Object viewer") { APP.ui.actionPane.orBuild.show(it) }
               separator()
               widgetItems<Opener> { it.open(value) }
            }
            if (APP.developerMode.value)
               menu("Public methods") {
                  val v = value
                  items(
                     v::class.java.methods.asSequence()
                        .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
                        .filter { it.name!="notify" || it.name!="notifyAll" || it.name!="wait" }
                        .sortedBy { it.name }
                        .filter { it.parameterCount==0 && (it.returnType==Void::class.javaObjectType || it.returnType==Void::class.javaPrimitiveType || it.returnType==Unit::class.java) },
                     { it.name },
                     {
                        runTry {
                           it(v)
                        }.ifError { e ->
                           logger.error(e) { "Could not invoke method=$it on object=$v" }
                        }
                     }
                  )
               }
         }
         add<File> {
            if (value.isAudio()) {
               menu("Playback") {
                  item("Play") {
                     APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { p ->
                        p.playlist.addNplay(it.toURI())
                     }
                  }
                  item("Play (new playlist)") {
                     APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                        p.playlist.addNplay(it.toURI())
                     }
                  }
                  item("Enqueue") {
                     APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { p ->
                        p.playlist.addFile(it)
                     }
                  }
                  item("Enqueue (new playlist)") {
                     APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                        p.playlist.addFile(it)
                     }
                  }
               }
            }
            if (value.isImage()) {
               item("Fullscreen") { APP.actions.openImageFullscreen(it) }
            }
            item("Open (in associated program)") { it.open() }
            item("Edit (in associated editor)") { it.edit() }
            item("Delete from disc") { it.recycle() }

            if (APP.location isParentOf value) {
               item("Copy path (absolute)") { copyToSysClipboard(DataFormat.PLAIN_TEXT, it.absolutePath) }
               item("Copy path (app relative)") { copyToSysClipboard(DataFormat.PLAIN_TEXT, it.path) }
            } else {
               item("Copy path") { copyToSysClipboard(DataFormat.PLAIN_TEXT, it.path) }
            }

            item("Copy as ...") { f ->
               object: ConfigurableBase<Any?>() {
                  val file by cv(APP.location).only(DIRECTORY).def(name = "File")
                  val overwrite by cv(false).def(name = "Overwrite")
                  val onError by cv(OnErrorAction.SKIP).def(name = "On error")
               }.configure("Copy as...") {
                  f.copyRecursively(it.file.value/f.name, it.overwrite.value) { _, e ->
                     logger.warn(e) { "File copy failed" }
                     it.onError.value
                  }.ifFalse {
                     AppErrors.push("File $f copy failed")
                  }
               }
            }
         }
         addMany<File> {
            item("Copy (to clipboard)") { copyToSysClipboard(DataFormat.FILES, it) }
            item("Browse location") { APP.actions.browseMultipleFiles(it.asSequence()) }
         }
         add<Node> {
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w ->
                  runIO { value.toConfigurableFx() } ui { w.configure(it) }
               }
            }
         }
         add<WindowFX> {
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w ->
                  runIO { value.toConfigurableFx() } ui { w.configure(it) }
               }
            }
         }
         add<Configurable<*>> {
            menu("Inspect properties in") {
               widgetItems<ConfiguringFeature> { it.configure(value) }
            }
         }
         add<MetadataGroup> {
            menu("Playback") {
               item("Play songs") {
                  APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { p ->
                     p.playlist.addItems(it.grouped)
                     p.playlist.playFirstItem()
                  }
               }
               item("Play songs (new playlist)") {
                  APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                     p.playlist.addItems(it.grouped)
                     p.playlist.playFirstItem()
                  }
               }
               item("Enqueue songs") {
                  APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { p ->
                     p.playlist.addItems(it.grouped)
                  }
               }
               item("Enqueue songs (new playlist)") {
                  APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                     p.playlist.addItems(it.grouped)
                  }
               }
            }
            menu("Library") {
               item("Update songs from file") { APP.db.refreshSongsFromFile(it.grouped) }
               item("Remove songs from library") { APP.db.removeSongs(it.grouped) }
            }
            menu("Show in") {
               widgetItems<SongReader> { it.read(value.grouped) }
            }
            menu("Edit tags in") {
               widgetItems<SongWriter> { it.read(value.grouped) }
            }
            menu("Location") {
               item("Explore songs' location") { APP.actions.browseMultipleFiles(it.grouped.asSequence().mapNotNull { it.getFile() }) }
               menu("Explore songs' location in") {
                  widgetItems<FileExplorerFeature> {
                     it.exploreCommonFileOf(value.grouped.mapNotNull { it.getFile() })
                  }
               }
            }
            if (value.field==Metadata.Field.ALBUM)
               menu("Search cover in") {
                  items(
                     APP.instances.getInstances<SearchUriBuilder>().asSequence(),
                     { "in ${it.name}" },
                     { it(value.getValueS("<none>")).browse() }
                  )
               }
         }
         add<PlaylistSongGroup> {
            item("Play songs") { it.playlist.playItem(it.songs.firstOrNull()) }
            item("Remove songs") { it.playlist.removeAll(it.songs) }
            menu("Show in") {
               widgetItems<SongReader> {
                  it.read(value.songs)
               }
            }
            menu("Edit tags in") {
               widgetItems<SongWriter> {
                  it.read(value.songs)
               }
            }
            item("Crop") { it.playlist.retainAll(it.songs) }
            menu("Duplicate") {
               item("as group") { it.playlist.duplicateItemsAsGroup(it.songs) }
               item("individually") { it.playlist.duplicateItemsByOne(it.songs) }
            }
            item("Explore directory") { APP.actions.browseMultipleFiles(it.songs.asSequence().mapNotNull { it.getFile() }) }
            menu("Search album cover") {
               items(
                  APP.instances.getInstances<SearchUriBuilder>().asSequence(),
                  { "in ${it.name}" },
                  { uriBuilder -> value.songs.firstOrNull()?.toMetadata { uriBuilder(it.getAlbumOrEmpty()).browse() } }
               )
            }
         }
         add<Thumbnail.ContextMenuData> {
            if (value.image!=null)
               menu("Cover") {
                  item("Save image as ...") {
                     saveFile(
                        "Save image as...",
                        APP.location,
                        it.iFile?.name ?: "new_image",
                        parentPopup.ownerWindow,
                        imageWriteExtensionFilter()
                     ).ifOk { f ->
                        writeImage(it.image, f).ifErrorNotify {
                           AppError("Saving image $f failed", "Reason: ${it.stacktraceAsString}")
                        }
                     }
                  }
                  item("Copy to clipboard") { copyToSysClipboard(DataFormat.IMAGE, it.image) }
               }
            if (!value.fsDisabled && value.iFile!=value.representant)
               menuFor("Cover file", value.fsImageFile)
            if (value.representant!=null)
               menuFor(value.representant)
         }
         add<Widget.Group> {
            item("Show open widget instances") { group ->
               APP.ui.actionPane.orBuild.show(
                  APP.widgetManager.widgets.findAll(WidgetSource.OPEN).filter { it.factory.group()==group }.sortedBy { it.name }.toList()
               )
            }
         }
         add<WidgetSource> {
            item("Show open widget instances") {
               APP.ui.actionPane.orBuild.show(
                  APP.widgetManager.widgets.findAll(it).sortedBy { it.name }.toList()
               )
            }
         }
         add<Input<*>> {
            menuFor("Value", value.value)
            menu("Link") {
               item("All identical") { it.bindAllIdentical() }
            }
            menu("Unlink") {
               item("All inbound") { it.unbindAll() }
            }
         }
         add<Output<*>> {
            menuFor("Value", value.value)
            menu("Unlink") {
               item("All outbound") { it.unbindAll() }
            }
         }
         add<InOutput<*>> {
            menuFor("Value", value.o.value)
            menu("Link") {
               item("All identical") { it.i.bindAllIdentical() }
            }
            menu("Unlink") {
               item("All") { it.i.unbindAll(); it.o.unbindAll() }
               item("All inbound") { it.i.unbindAll() }
               item("All outbound") { it.o.unbindAll() }
            }
         }
      }
   }

   @Dsl
   private inline fun <reified W: Any> MenuBuilder<*, *>.widgetItems(crossinline action: (W) -> Unit) = items(
      source = APP.widgetManager.factories.getFactoriesWith<W>(),
      text = { it.name },
      action = { it.use(NO_LAYOUT) { action(it) } }
   )

   @Dsl
   private fun MenuBuilder<*, *>.menuFor(value: Any?) = menuFor(APP.className.getOf(value), value)

   @Dsl
   private fun MenuBuilder<*, *>.menuFor(menuName: String, value: Any?) = menu(menuName, null) {
      menuItemBuilders[value].forEach { this add it }
   }

}