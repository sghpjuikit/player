package sp.it.pl.core

import javafx.stage.Window as WindowFX
import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.lang.reflect.Modifier
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Menu
import javafx.scene.input.KeyCode.A
import javafx.scene.input.KeyCode.F
import javafx.scene.input.KeyCode.F11
import javafx.scene.input.KeyCode.F12
import javafx.scene.input.KeyCode.F2
import javafx.scene.input.KeyCode.G
import javafx.scene.input.KeyCode.Q
import javafx.scene.input.KeyCode.SHORTCUT
import javafx.scene.input.KeyCode.WINDOWS
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.layout.Component
import sp.it.pl.layout.ComponentFactory
import sp.it.pl.layout.ComponentUiBase
import sp.it.pl.layout.Container
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetSource
import sp.it.pl.layout.WidgetUi
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.layout.WidgetUse.NEW
import sp.it.pl.layout.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.controller.io.GeneratingOutputRef
import sp.it.pl.layout.controller.io.IOLayer
import sp.it.pl.layout.controller.io.InOutput
import sp.it.pl.layout.controller.io.Input
import sp.it.pl.layout.controller.io.InputRef
import sp.it.pl.layout.controller.io.Output
import sp.it.pl.layout.controller.io.OutputRef
import sp.it.pl.layout.controller.io.boundOutputs
import sp.it.pl.layout.controller.io.boundInputs
import sp.it.pl.layout.feature.ConfiguringFeature
import sp.it.pl.layout.feature.FileExplorerFeature
import sp.it.pl.layout.feature.Opener
import sp.it.pl.layout.feature.PlaylistFeature
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.layout.feature.SongWriter
import sp.it.pl.main.APP
import sp.it.pl.main.ActionsPaneGenericActions
import sp.it.pl.main.App
import sp.it.pl.main.AppError
import sp.it.pl.main.AppTexts.textNoVal
import sp.it.pl.main.Df.FILES
import sp.it.pl.main.Df.IMAGE
import sp.it.pl.main.Df.PLAIN_TEXT
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.IconMD
import sp.it.pl.main.Ui.ICON_CLOSE
import sp.it.pl.main.Ui.ICON_CONF
import sp.it.pl.main.WidgetDefaultMenu
import sp.it.pl.main.configure
import sp.it.pl.main.copyAs
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.imageWriteExtensionFilter
import sp.it.pl.main.isAudio
import sp.it.pl.main.isImage
import sp.it.pl.main.showFloating
import sp.it.pl.main.sysClipboard
import sp.it.pl.main.toMetadata
import sp.it.pl.main.toUi
import sp.it.pl.main.writeImage
import sp.it.pl.plugin.PluginBox
import sp.it.pl.ui.itemnode.WidgetsCE.WidgetInfoPane
import sp.it.pl.ui.objects.contextmenu.SelectionMenuItem
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.WindowBase.Maximized
import sp.it.pl.ui.objects.window.stage.asAppWindow
import sp.it.pl.ui.objects.window.stage.clone
import sp.it.pl.ui.objects.window.stage.openWindowSettings
import sp.it.pl.ui.pane.ActContext
import sp.it.pl.web.SearchUriBuilder
import sp.it.util.access.toggle
import sp.it.util.access.vn
import sp.it.util.action.ActionManager
import sp.it.util.action.ActionRegistrar
import sp.it.util.conf.Config
import sp.it.util.conf.Configurable
import sp.it.util.conf.nonNull
import sp.it.util.conf.toConfigurableFx
import sp.it.util.conf.uiConverter
import sp.it.util.conf.values
import sp.it.util.dev.Dsl
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.isParentOf
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.runTry
import sp.it.util.reactive.sync1IfNonNull
import sp.it.util.reactive.syncNonNullIntoWhile
import sp.it.util.system.browse
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.system.saveFile
import sp.it.util.text.keys
import sp.it.util.text.keysUi
import sp.it.util.text.nameUi
import sp.it.util.text.resolved
import sp.it.util.type.type
import sp.it.util.ui.ContextMenuGenerator
import sp.it.util.ui.MenuBuilder
import sp.it.util.ui.drag.set
import sp.it.util.ui.menuItem

object CoreMenus: Core {

   /** Menu item builders registered per class. */
   val menuItemBuilders = ContextMenuGenerator()

   @Suppress("RemoveExplicitTypeArguments")
   override fun init() {
      menuItemBuilders {
         addCustom { kClass ->
            { value ->
               ActionsPaneGenericActions.actionsAll[kClass].orEmpty().asSequence().filter { it.invokeIsDoable(value) }.map { action ->
                  menuItem(action.name, action.icon.toCmUi { tooltip(action.description) } ) {
                     action.invokeFutAndProcess(ActContext(it), value)
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
            if (value !is CoreMenuNoInspect)
               menu("Inspect in") {
                  item("Object viewer") {
                     APP.ui.actionPane.orBuild.show(
                        when {
                           it is MetadataGroup -> it.grouped
                           it is PlaylistSongGroup -> it.songs
                           else -> it
                        }
                     )
                  }
                  separator()
                  widgetItems<Opener> { it.open(value) }
               }
            if (value !is CoreMenuNoInspect && APP.developerMode.value)
               menu("Invoke") {
                  val v = value
                  items(
                     v::class.java.methods.asSequence()
                        .filter { Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) }
                        .filter { it.name!="notify" || it.name!="notifyAll" || it.name!="wait" }
                        .sortedBy { it.name }
                        .filter { it.parameterCount==0 && (it.returnType==Void::class.javaObjectType || it.returnType==Void::class.javaPrimitiveType || it.returnType==Unit::class.java) },
                     { it.name },
                     { null },
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
            menu("Windows") {
               item("New window") { APP.windowManager.createWindow() }
               menu("All") {
                  APP.windowManager.windows.forEach { w ->
                     menuFor("${w.stage.title} (${w.width} x ${w.height})", w)
                  }
               }
            }
            menu("Audio") {
               item("Play/pause", keys = ActionRegistrar["Pause/resume"].keysUi()) { APP.audio.pauseResume() }
            }
            item("Restart") { APP.restart() }
            item("Exit") { APP.close() }
         }
         add<File> {
            if (value.isAudio()) {
               menu("Playback") {
                  item("Play") {
                     APP.widgetManager.widgets.use<PlaylistFeature>(ANY) { p ->
                        p.playlist.addAndPlay(it.toURI())
                     }
                  }
                  item("Play (new playlist)") {
                     APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                        p.playlist.addAndPlay(it.toURI())
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
            menu("Open in") {
               item("associated program") { it.open() }
            }
            menu("Edit in") {
               item("associated editor") { it.edit() }
            }
            item("Delete from disc") { it.recycle() }

            menu("Copy") {

               if (APP.location isParentOf value) {
                  item("Path (absolute)") { sysClipboard[PLAIN_TEXT] = it.absolutePath }
                  item("Path (app relative)") { sysClipboard[PLAIN_TEXT] = it.path }
               } else {
                  item("Path") { sysClipboard[PLAIN_TEXT] = it.path }
               }

               item("Filename") { sysClipboard[PLAIN_TEXT] = it.nameOrRoot }
               item("File", keys = keys("${SHORTCUT.resolved} + C")) { sysClipboard[FILES] = listOf(it) }
               item("File To ...") { it.copyAs() }
            }
         }
         addMany<File> {
            if (value.size>1) {
               menu("Copy") {
                  item("Files", keys = keys("${SHORTCUT.resolved} + C")) { sysClipboard[FILES] = it.toList() }
                  item("Files To ...") { it.copyAs() }
               }
            }
            item("Browse location") { APP.actions.browseMultipleFiles(it.asSequence()) }
         }
         add<Node> {
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w -> w.configureAsync { value.toConfigurableFx() } }
            }
         }
         add<Scene> {
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w -> w.configureAsync { value.toConfigurableFx() } }
            }
         }
         add<Window> {
            item("Clone", IconFA.CLONE.toCmUi()) { it.clone() }
            item("Close", ICON_CLOSE.toCmUi(), keys(WINDOWS, Q)) { it.close() }
            item("Fullscreen", if (value.fullscreen.value) IconMD.FULLSCREEN_EXIT.toCmUi() else IconMD.FULLSCREEN.toCmUi(), "${keys(WINDOWS)}? + ${keys(F11)}|${keys(F12)}") { it.fullscreen.toggle() }
            menu("Maximize", IconMD.WINDOW_MAXIMIZE.toCmUi()) {
               item(Maximized.ALL.toUi(), IconMA.BORDER_OUTER.toCmUi(), keys(WINDOWS, F)) { it.maximized.value = Maximized.ALL }
               item(Maximized.LEFT.toUi(), IconMA.BORDER_LEFT.toCmUi(), keys(WINDOWS, F)) { it.maximized.value = Maximized.LEFT }
               item(Maximized.RIGHT.toUi(), IconMA.BORDER_RIGHT.toCmUi(), keys(WINDOWS, F)) { it.maximized.value = Maximized.RIGHT }
               item(Maximized.LEFT_TOP.toUi(), IconMA.BORDER_STYLE.toCmUi { rotate = 0.0 }, keys(WINDOWS, F)) { it.maximized.value = Maximized.LEFT_TOP }
               item(Maximized.RIGHT_TOP.toUi(), IconMA.BORDER_STYLE.toCmUi { rotate = 90.0 }, keys(WINDOWS, F)) { it.maximized.value = Maximized.RIGHT_TOP }
               item(Maximized.RIGHT_BOTTOM.toUi(), IconMA.BORDER_STYLE.toCmUi { rotate = 180.0 }, keys(WINDOWS, F)) { it.maximized.value = Maximized.RIGHT_BOTTOM }
               item(Maximized.LEFT_BOTTOM.toUi(), IconMA.BORDER_STYLE.toCmUi { rotate = 270.0 }, keys(WINDOWS, F)) { it.maximized.value = Maximized.LEFT_BOTTOM }
               item(Maximized.NONE.toUi(), IconMA.BORDER_CLEAR.toCmUi(), keys(WINDOWS, F)) { it.maximized.value = Maximized.NONE }
            }
            item("Minimize", IconMD.WINDOW_MINIMIZE.toCmUi(), keys(WINDOWS, G)) { it.minimize() }
            item("On top", if (value.alwaysOnTop.value) IconFA.SQUARE.toCmUi() else IconFA.SQUARE_ALT.toCmUi(), keys(WINDOWS, A)) { it.alwaysOnTop.toggle() }
            item("Settings", ICON_CONF.toCmUi()) { openWindowSettings(it, null) }
         }
         add<WindowFX> {
            value.asAppWindow().ifNotNull { w -> itemsFor(w) }
            menu("Inspect ui properties in") {
               widgetItems<ConfiguringFeature> { w -> w.configureAsync { value.toConfigurableFx() } }
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
                     p.playlist.setAndPlay(it.grouped)
                  }
               }
               item("Play songs (new playlist)") {
                  APP.widgetManager.widgets.use<PlaylistFeature>(NEW) { p ->
                     p.playlist.setAndPlay(it.grouped)
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
               item("Update songs from file") { APP.db.refreshSongsFromFile(it.grouped.toList()) }
               item("Remove songs from library") { APP.db.removeSongs(it.grouped) }
            }
            menu("Open in") {
               widgetItems<SongReader> {
                  it.read(value.grouped.toList()) }
            }
            menu("Edit in") {
               widgetItems<SongWriter> { it.read(value.grouped.toList()) }
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
                     APP.instances.getInstancesAsSeq<SearchUriBuilder>(),
                     { "in ${it.name}" },
                     { it.icon?.toCmUi() },
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
            item("Play songs", keys = keys("ENTER")) { it.playlist.playTransformedItem(it.songs.firstOrNull()) }
            menu("Show in") {
               widgetItems<SongReader> {
                  it.read(value.songs)
               }
            }
            menu("Edit in") {
               widgetItems<SongWriter> {
                  it.read(value.songs)
               }
            }
            menu("Remove") {
               item("Remove selected", keys = keys("DELETE")) { it.playlist.removeAll(it.songs) }
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
                  APP.instances.getInstancesAsSeq<SearchUriBuilder>(),
                  { "in ${it.name}" },
                  { null },
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
         add<ComponentFactory<*>> {
            item("New") { APP.windowManager.showWindow(it) }
         }
         add<Component> {
            menu("Load type") {
               items {
                  SelectionMenuItem.buildSingleSelectionMenu(Widget.LoadType.values().toList(), value.loadType.value, { it.toUi() }) { value.loadType.value = it }.asSequence()
               }
            }
            item("Close") { it.close() }
            item("Detach") {
               when(it) {
                  is Container<*> -> it.ui?.asIf<ComponentUiBase<*>>()?.detach()
                  is Widget -> it.ui?.asIf<ComponentUiBase<*>>()?.detach()
               }
            }
         }
         add<Widget> {
            item("Show info") { w -> showFloating(w.factory.name + " info") { WidgetInfoPane(w.factory) } }
            item("Show help", keys = "${F2.nameUi}|${ActionManager.keyShortcutsComponent.nameUi}") { APP.actions.showShortcutsFor(it) }
            item("Show actions", IconFA.GAVEL.toCmUi(), ActionManager.keyActionsComponent.nameUi) { APP.actions.showShortcutsFor(it) }
            value.ui.asIf<WidgetUi>().ifNotNull { w -> item("Settings", Icon(IconFA.COGS)) { w.controls.showSettings() } }
            menuFor("Settings defaults", WidgetDefaultMenu(value))
         }
         add<PluginBox<*>> {
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
               item("From value...") { input ->
                  Config.forProperty(input.type, "Value", vn(input.value).asIs()).configure("Set ${input.name} value") { c ->
                     fun Input<Any?>.setValue() { this.value = c.value }
                     input.asIs<Input<Any?>>().setValue()
                  }
               }
               item("From output...") { input ->
                  Config.forProperty(type<OutputRef?>(), "Output", vn(null)).constrain {
                     nonNull()
                     uiConverter { it?.name ?: textNoVal }
                     values { listOf(null) + IOLayer.allOutputRefs().filter { input.isAssignable(it) && !input.isBound(it) } }
                  }.configure("Link '${input.name}' input to...") { c ->
                     c.value.ifNotNull { input.bindAny(it.output) }
                  }
               }
               item("From generator...") { input ->
                  Config.forProperty(type<GeneratingOutputRef<*>?>(), "Generator", vn(null)).constrain {
                     nonNull()
                     uiConverter { it?.name ?: textNoVal }
                     values { listOf(null) + IOLayer.generatingOutputRefs.filter { input.isAssignable(it.type) && !input.isBound(it.id) } }
                  }.configure("Link '${input.name}' input from...") { c ->
                     c.value.ifNotNull(input::bind)
                  }
               }
               item("From all identical") { it.bindAllIdentical() }
            }
            menu("Unlink") {
               menu("One") {
                  items(value.boundOutputs(), { it.name }) { value.unbind(it) }
               }
               item("All") { it.unbindAll() }
            }
         }
         add<Output<*>> {
            menuFor("Value", value.value)
            menu("Link") {
               item("To...") { output ->
                  Config.forProperty(type<InputRef?>(), "Input", vn(null)).constrain {
                     nonNull()
                     uiConverter { it?.name ?: textNoVal }
                     values { listOf(null) + IOLayer.allInputRefs().filter { it.input.isAssignable(output) && !it.input.isBound(output) } }
                  }.configure("Link '${output.id.name}.${output.name}' input to...") { c ->
                     c.value.ifNotNull { it.input.bindAny(output) }
                  }
               }
            }
            menu("Unlink") {
               menu("One") {
                  items(value.boundInputs(), { it.name }) { value.unbind(it) }
               }
               item("All") { it.unbindAll() }
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
      graphics = { it.toFactory()?.icon?.toCmUi() },
      action = { it.use(NO_LAYOUT) { action(it) } }
   )

   @Dsl
   private fun MenuBuilder<*, *>.menuFor(value: Any?) = menuFor(APP.className.getOf(value), value)

   @Dsl
   private fun MenuBuilder<*, *>.menuFor(menuName: String, value: Any?, block: MenuBuilder<Menu, out Any?>.() -> Unit = {}) = menu(menuName, null) {
      menuItemBuilders[value].forEach { this add it }
      block()
   }

   @Dsl
   private fun MenuBuilder<*, *>.itemsFor(value: Any?) = items { menuItemBuilders[value] }

   private inline fun GlyphIcons.toCmUi(block: (Icon).() -> Unit = {}) = Icon(this).apply(block).apply {
      parentProperty().syncNonNullIntoWhile(Parent::sceneProperty) {
         it.windowProperty().sync1IfNonNull {
            focusOwner.value = parent.parent
         }
      }
   }

}

/** Extending this causes [CoreMenus] menu builder to not generate generic menu items */
interface CoreMenuNoInspect