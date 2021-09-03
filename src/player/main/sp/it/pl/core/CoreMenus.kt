package sp.it.pl.core

import javafx.stage.Window as WindowFX
import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.lang.reflect.Modifier
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode.A
import javafx.scene.input.KeyCode.F
import javafx.scene.input.KeyCode.F11
import javafx.scene.input.KeyCode.F12
import javafx.scene.input.KeyCode.G
import javafx.scene.input.KeyCode.Q
import javafx.scene.input.KeyCode.SHORTCUT
import javafx.scene.input.KeyCode.WINDOWS
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.layout.Component
import sp.it.pl.layout.WidgetSource
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.controller.io.GeneratingOutputRef
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.layout.controller.io.InOutput
import sp.it.pl.layout.controller.io.Input
import sp.it.pl.layout.controller.io.Output
import sp.it.pl.layout.feature.ConfiguringFeature
import sp.it.pl.layout.feature.FileExplorerFeature
import sp.it.pl.layout.feature.Opener
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.layout.feature.SongWriter
import sp.it.pl.layout.openInConfigured
import sp.it.pl.main.APP
import sp.it.pl.main.ActionsPaneGenericActions
import sp.it.pl.main.App
import sp.it.pl.main.AppDev
import sp.it.pl.main.AppError
import sp.it.pl.main.AppOpen
import sp.it.pl.main.Df.FILES
import sp.it.pl.main.Df.IMAGE
import sp.it.pl.main.Df.PLAIN_TEXT
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Ui.ICON_CLOSE
import sp.it.pl.main.Ui.ICON_CONF
import sp.it.pl.main.configure
import sp.it.pl.main.copyAs
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.imageWriteExtensionFilter
import sp.it.pl.main.isAudio
import sp.it.pl.main.isImage
import sp.it.pl.main.sysClipboard
import sp.it.pl.main.toMetadata
import sp.it.pl.main.toUi
import sp.it.pl.main.writeImage
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized
import sp.it.pl.ui.objects.window.stage.asAppWindow
import sp.it.pl.ui.objects.window.stage.clone
import sp.it.pl.ui.objects.window.stage.openWindowSettings
import sp.it.pl.web.SearchUriBuilder
import sp.it.util.access.toggle
import sp.it.util.access.vn
import sp.it.util.async.runIO
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.nonNull
import sp.it.util.conf.toConfigurableFx
import sp.it.util.conf.uiConverter
import sp.it.util.conf.valuesIn
import sp.it.util.dev.Dsl
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.isParentOf
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry
import sp.it.util.system.browse
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.system.saveFile
import sp.it.util.text.keys
import sp.it.util.text.resolved
import sp.it.util.type.type
import sp.it.util.ui.ContextMenuGenerator
import sp.it.util.ui.MenuBuilder
import sp.it.util.ui.drag.set
import sp.it.util.ui.menuItem

object CoreMenus: Core {

   /** Menu item builders registered per class. */
   val menuItemBuilders = ContextMenuGenerator()

   override fun init() {
      menuItemBuilders {
         mSingleCustom = { kClass ->
            { value ->
               ActionsPaneGenericActions.actionsAll[kClass].orEmpty().asSequence().map { action ->
                  menuItem(action.name) {
                     action.invoke(value)
                  }
               }
            }
         }
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
         add<App> {
            menuFor("Developer tools", AppDev)
            menuFor("Directory", value.location)
            item("Open...") { APP.ui.actionPane.orBuild.show(AppOpen) }
            menu("Windows") {
               item("New window") { APP.windowManager.createWindow() }
               menu("All") {
                  APP.windowManager.windows.forEach { w ->
                     menuFor("${w.stage.title} (${w.width} x ${w.height})", w)
                  }
               }
            }
            menu("Audio") {
               item("Play/pause") { APP.audio.pauseResume() }
            }
            item("Restart") { APP.restart() }
            item("Exit") { APP.close() }
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

            menu("Copy") {

               if (APP.location isParentOf value) {
                  item("Path (absolute)") { sysClipboard[PLAIN_TEXT] = it.absolutePath }
                  item("Path (app relative)") { sysClipboard[PLAIN_TEXT] = it.path }
               } else {
                  item("Path") { sysClipboard[PLAIN_TEXT] = it.path }
               }

               item("Filename") { sysClipboard[PLAIN_TEXT] = it.nameOrRoot }
               item("File (${keys("${SHORTCUT.resolved} + C")})") { sysClipboard[FILES] = listOf(it) }
               item("File To ...") { it.copyAs() }
            }
         }
         addMany<File> {
            if (value.size>1) {
               menu("Copy (${keys("${SHORTCUT.resolved} + C")})") {
                  item("Files") { sysClipboard[FILES] = it.toList() }
                  item("Files To ...") { it.copyAs() }
               }
            }
            item("Browse location") { APP.actions.browseMultipleFiles(it.asSequence()) }
         }
         add<Node> {
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w ->
                  runIO { value.toConfigurableFx() } ui { w.configure(it) }
               }
            }
         }
         add<Scene> {
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w ->
                  runIO { value.toConfigurableFx() } ui { w.configure(it) }
               }
            }
         }
         add<Window> {
            item("Clone") { it.clone() }
               .icon(IconFA.CLONE)
            item("Close (" + keys(WINDOWS, Q) +")") { it.close() }
               .icon(ICON_CLOSE)
            item("Fullscreen (" + keys(WINDOWS, F11) + "/" + keys(WINDOWS, F12) + ")") { it.clone() }
               .icon(if (value.fullscreen.value) IconMD.FULLSCREEN_EXIT else IconMD.FULLSCREEN)
            menu("Maximize (" + keys(WINDOWS, F) + ")") {
                  item(Maximized.ALL.toUi()) { it.maximized.value = Maximized.ALL }.icon(IconMA.BORDER_OUTER)
                  item(Maximized.LEFT.toUi()) { it.maximized.value = Maximized.LEFT }.icon(IconMA.BORDER_LEFT)
                  item(Maximized.RIGHT.toUi()) { it.maximized.value = Maximized.RIGHT }.icon(IconMA.BORDER_RIGHT)
                  item(Maximized.LEFT_TOP.toUi()) { it.maximized.value = Maximized.LEFT_TOP }.icon(IconMA.BORDER_STYLE) { rotate = 0.0 }
                  item(Maximized.RIGHT_TOP.toUi()) { it.maximized.value = Maximized.RIGHT_TOP }.icon(IconMA.BORDER_STYLE) { rotate = 90.0 }
                  item(Maximized.RIGHT_BOTTOM.toUi()) { it.maximized.value = Maximized.RIGHT_BOTTOM }.icon(IconMA.BORDER_STYLE) { rotate = 180.0 }
                  item(Maximized.LEFT_BOTTOM.toUi()) { it.maximized.value = Maximized.LEFT_BOTTOM }.icon(IconMA.BORDER_STYLE) { rotate = 270.0 }
                  item(Maximized.NONE.toUi()) { it.maximized.value = Maximized.NONE }.icon(IconMA.BORDER_CLEAR)
               }
               .icon(IconMD.WINDOW_MAXIMIZE)
            item("Minimize (" + keys(WINDOWS, G) + ")") { it.minimize() }
               .icon(IconMD.WINDOW_MINIMIZE)
            item("On top (" + keys(WINDOWS, A) + ")") { it.alwaysOnTop.toggle() }
               .icon(if (value.alwaysOnTop.value) IconFA.SQUARE else IconFA.SQUARE_ALT)
            item("Settings") { openWindowSettings(it, null) }
               .icon(ICON_CONF)
         }
         add<WindowFX> {
            value.asAppWindow().ifNotNull { w -> item("Clone") { w.clone() } }
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w ->
                  runIO { value.toConfigurableFx() } ui { w.configure(it) }
               }
            }
         }
         add<Component> {
            item("Clone") { it.openInConfigured() }
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
                     p.playlist.setNplay(it.grouped)
                  }
               }
               item("Play songs (new playlist)") {
                  APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                     p.playlist.setNplay(it.grouped)
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
               widgetItems<SongReader> {
                  it.read(value.grouped) }
            }
            menu("Edit tags in") {
               widgetItems<SongWriter> { it.read(value.grouped) }
            }
            if (value.grouped.size==1) {
               value.grouped.firstOrNull()?.getFile().ifNotNull {
                  menuFor("File", it)
               }
            } else {
               menu("Location") {
                  item("Explore songs' location") { APP.actions.browseMultipleFiles(it.grouped.asSequence().mapNotNull { it.getFile() }) }
                  menu("Explore songs' location in") {
                     widgetItems<FileExplorerFeature> {
                        it.exploreCommonFileOf(value.grouped.mapNotNull { it.getFile() })
                     }
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
            if (value.songs.size==1) {
               value.songs.firstOrNull()?.getFile().ifNotNull {
                  menuFor("File", it)
               }
            }
            item("Play songs (${keys("ENTER")})") { it.playlist.playItem(it.songs.firstOrNull()) }
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
            menu("Remove") {
               item("Remove selected (${keys("DELETE")})") { it.playlist.removeAll(it.songs) }
               item("Retain selected") { it.playlist.retainAll(it.songs) }
               item("Remove all") { it.playlist.clear() }
            }
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
                  item("Copy to clipboard") { sysClipboard[IMAGE] = it.image }
               }
            if (!value.fsDisabled && value.iFile!=value.representant)
               menuFor("Cover file", value.fsImageFile)
            if (value.representant!=null)
               menuFor(value.representant)
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
               item("From all identical") { it.bindAllIdentical() }
               item("To generated...") { input ->
                  Config.forProperty(type<GeneratingOutputRef<*>>(), "Generator", vn<GeneratingOutputRef<*>>(null)).constrain {
                     nonNull()
                     uiConverter { it.name }
                     valuesIn { IOLayer.generatingOutputRefs.asSequence().filter { input.isAssignable(it.type) && !input.isBound(it.id) } }
                  }.configure("Link ${input.name} to...") { c ->
                     c.value.ifNotNull(input::bind)
                  }
               }
            }
            item("Set to...") { input ->
               Config.forProperty(input.type, "Value", vn(input.value).asIs()).configure("Set ${input.name} value") { c ->
                  fun Input<Any?>.setValue() { this.value = c.value }
                  input.asIs<Input<Any?>>().setValue()
               }
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
               item("From all identical") { it.i.bindAllIdentical() }
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
   private fun MenuBuilder<*, *>.menuFor(menuName: String, value: Any?, block: MenuBuilder<Menu, out Any?>.() -> Unit = {}) = menu(menuName, null) {
      menuItemBuilders[value].forEach { this add it }
      block()
   }

   private inline fun MenuItem.icon(icon: GlyphIcons, block: (Icon).() -> Unit = {}) = apply { graphic = Icon(icon).apply(block) }

}