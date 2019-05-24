package sp.it.pl.main

import com.drew.imaging.ImageMetadataReader
import com.drew.imaging.ImageProcessingException
import com.sun.tools.attach.VirtualMachine
import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.paint.Color.BLACK
import javafx.stage.Screen
import javafx.util.Callback
import mu.KLogging
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.readAudioFile
import sp.it.pl.gui.objects.grid.GridCell
import sp.it.pl.gui.objects.grid.GridView
import sp.it.pl.gui.objects.grid.GridView.SelectionOn
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.icon.IconInfo
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.popover.ScreenPos
import sp.it.pl.gui.pane.OverlayPane
import sp.it.pl.gui.pane.OverlayPane.Display.SCREEN_OF_MOUSE
import sp.it.pl.layout.container.ComponentUi
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetLoader.WINDOW_FULLSCREEN
import sp.it.pl.layout.widget.WidgetUse.NEW
import sp.it.pl.layout.widget.WidgetUse.NO_LAYOUT
import sp.it.pl.layout.widget.controller.Controller
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.layout.widget.feature.TextDisplayFeature
import sp.it.pl.web.DuckDuckGoQBuilder
import sp.it.pl.web.WebBarInterpreter
import sp.it.util.Util.urlEncodeUtf8
import sp.it.util.access.fieldvalue.StringGetter
import sp.it.util.action.ActionRegistrar
import sp.it.util.action.IsAction
import sp.it.util.async.runFX
import sp.it.util.collections.setTo
import sp.it.util.conf.IsConfigurable
import sp.it.util.dev.Blocks
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.stackTraceAsString
import sp.it.util.functional.asIf
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1If
import sp.it.util.system.browse
import sp.it.util.system.open
import sp.it.util.system.runCommand
import sp.it.util.type.Util.getEnumConstants
import sp.it.util.ui.anchorPane
import sp.it.util.ui.bgr
import sp.it.util.ui.getScreenForMouse
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.listView
import sp.it.util.ui.listViewCellFactory
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.stackPane
import sp.it.util.units.millis
import sp.it.util.units.times
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

@IsConfigurable("Shortcuts")
class AppActions {

    @IsAction(name = "Open on Github", desc = "Opens Github page for this application. For developers.")
    fun openAppGithubPage() {
        APP.uriGithub.browse()
    }

    @IsAction(name = "Open app directory", desc = "Opens directory from which this application is running from.")
    fun openAppLocation() {
        APP.DIR_APP.open()
    }

    @IsAction(name = "Open css guide", desc = "Opens css reference guide. For developers.")
    fun openCssGuide() {
        URI.create("http://docs.oracle.com/javase/8/javafx/api/javafx/scene/doc-files/cssref.html").browse()
    }

    @IsAction(name = "Open icon viewer", desc = "Opens application icon browser. For developers.")
    fun openIconViewer() {
        val iconSize = 120.0
        val iconsView = GridView<GlyphIcons, GlyphIcons>(GlyphIcons::class.java, { it }, iconSize, iconSize+30, 5.0, 5.0).apply {
            search.field = StringGetter.of { value, _ -> value.name() }
            selectOn setTo listOf(SelectionOn.MOUSE_HOVER, SelectionOn.MOUSE_CLICK, SelectionOn.KEY_PRESS)
            cellFactory = Callback {
                object: GridCell<GlyphIcons, GlyphIcons>() {

                    init {
                        styleClass += "icon-grid-cell"
                        isPickOnBounds = true
                    }

                    public override fun updateItem(icon: GlyphIcons, empty: Boolean) {
                        super.updateItem(icon, empty)

                        if (empty || item==null) {
                            graphic = null
                        } else {
                            val iconInfo = graphic as? IconInfo ?: IconInfo(null, iconSize).apply { isMouseTransparent = true }
                            iconInfo.setGlyph(if (empty) null else icon)
                            graphic = iconInfo
                        }
                    }

                    override fun updateSelected(selected: Boolean) {
                        super.updateSelected(selected)

                        graphic.asIf<IconInfo>()?.select(selected)
                    }

                }
            }
        }
        val groupsView = listView<Class<GlyphIcons>> {
            minPrefMaxWidth = 200.0
            cellFactory = listViewCellFactory { group, empty ->
                text = if (empty) null else group.simpleName
            }
            selectionModel.selectionMode = SINGLE
            selectionModel.selectedItemProperty() sync {
                iconsView.itemsRaw setTo it?.net { getEnumConstants<GlyphIcons>(it).toList() }.orEmpty()
            }
            items setTo Icon.GLYPH_TYPES
        }
        val layout = hBox(20, CENTER) {
            setPrefSize(900.0, 700.0)
            lay += groupsView
            lay(ALWAYS) += stackPane(iconsView)
        }

        PopOver(layout).show(ScreenPos.APP_CENTER)
        if (!groupsView.items.isEmpty()) groupsView.selectionModel.select(0)
    }

    @IsAction(name = "Open launcher", desc = "Opens program launcher widget.", keys = "CTRL+P")
    fun openLauncher() {
        val f = File(APP.DIR_LAYOUTS, "AppMainLauncher.fxwl")
        val c = null
                ?: APP.windowManager.instantiateComponent(f)
                ?: APP.widgetManager.factories.getFactoryByGuiName(Widgets.APP_LAUNCHER).orNull()?.create()

        if (c!=null) {
            val op = object: OverlayPane<Void>() {
                override fun show(data: Void?) {
                    val componentRoot = c.load() as Pane
                    // getChildren().add(componentRoot);   // alternatively for borderless/fullscreen experience // TODO investigate & use | remove
                    content = anchorPane {
                        lay(20) += componentRoot
                    }
                    if (c is Widget) {
                        val parent = this
                        c.controller.getFieldOrThrow("closeOnLaunch").value = true
                        c.controller.getFieldOrThrow("closeOnRightClick").value = true
                        c.uiTemp = object: ComponentUi {
                            override val root = parent
                            override fun show() {}
                            override fun hide() {}
                            override fun dispose() = root.hide()    // TODO: make sure there is no leak, + turn this into WidgetLoader
                        }
                    }
                    super.show()
                }

                override fun hide() {
                    super.hide()
                    c.exportFxwl(f)
                    c.close()
                }
            }
            op.display.value = SCREEN_OF_MOUSE
            op.displayBgr.value = APP.ui.viewDisplayBgr.value
            op.show(null)
            op.makeResizableByUser()
            c.load().apply {
                prefWidth(900.0)
                prefHeight(700.0)
            }
            c.focus()
        }
    }

    @IsAction(name = "Open settings", desc = "Opens application settings.")
    fun openSettings() {
        APP.widgetManager.widgets.use<ConfiguringFeature>(NO_LAYOUT) { it.configure(APP.configuration) }
    }

    @IsAction(name = "Open app actions", desc = "Actions specific to whole application.")
    fun openActions() {
        APP.ui.actionPane.orBuild.show(APP)
    }

    @IsAction(name = "Open...", desc = "Display all possible open actions.", keys = "CTRL+SHIFT+O", global = true)
    fun openOpen() {
        APP.ui.actionPane.orBuild.show(AppOpen)
    }

    @IsAction(name = "Show shortcuts", desc = "Display all available shortcuts.", keys = "COMMA")
    fun showShortcuts() {
        APP.ui.shortcutPane.orBuild.show(ActionRegistrar.getActions())
    }

    @IsAction(name = "Show system info", desc = "Display system information.")
    fun showSysInfo() {
        APP.ui.infoPane.orBuild.show(null)
    }

    @IsAction(name = "Show overlay", desc = "Display screen overlay.")
    fun showOverlay() {
        val overlays = ArrayList<OverlayPane<Unit>>()
        fun <T> List<T>.forEachDelayed(block: (T) -> Unit) = forEachIndexed { i, it -> runFX(200.millis*i) { block(it) } }
        var canHide = false
        val showAll = {
            overlays.forEachDelayed { it.show(Unit) }
        }
        val hideAll = {
            canHide = true
            overlays.forEachDelayed { it.hide() }
        }
        overlays += Screen.getScreens().asSequence().sortedBy { it.bounds.minX }.map {
            object: OverlayPane<Unit>() {

                init {
                    content = stackPane()
                    display.value = object: ScreenGetter {
                        override fun computeScreen() = it
                    }
                }

                override fun show(data: Unit?) {
                    super.show()
                }

                override fun hide() {
                    if (canHide) super.hide()
                    else hideAll()
                }

            }
        }
        showAll()
    }

    @IsAction(name = "Run garbage collector", desc = "Runs java's garbage collector using 'System.gc()'.")
    fun runGarbageCollector() {
        System.gc()
    }

    @IsAction(name = "Search", desc = "Display application search.", keys = "CTRL+SHIFT+I", global = true)
    fun showSearchPosScreen() {
        showSearch(ScreenPos.SCREEN_CENTER)
    }

    fun showSearch(pos: ScreenPos) {
        val p = PopOver<Node>()
        p.contentNode.value = APP.search.buildUi { p.hide() }
        p.title.set("Search for an action or option")
        p.isAutoHide = true
        p.show(pos)
    }

    @IsAction(name = "Run system command", desc = "Runs command just like in a system's shell's command line.", global = true)
    fun runCommand() {
        configureString("Run system command", "Command") {
            runCommand(it)
        }
    }

    @IsAction(name = "Run as app argument", desc = "Equivalent of launching this application with the command as a parameter.")
    fun runAppCommand() {
        configureString("Run app command", "Command") {
            APP.parameterProcessor.process(listOf(it))
        }
    }

    @IsAction(name = "Open web search", desc = "Opens website or search engine result for given phrase", keys = "CTRL + SHIFT + W", global = true)
    fun openWebBar() {
        // TODO: use URI validator
        configureString("Open on web...", "Website or phrase") {
            val uriString = WebBarInterpreter.toUrlString(it, DuckDuckGoQBuilder)
            try {
                val uri = URI(uriString)
                uri.browse()
            } catch (e: URISyntaxException) {
                logger.warn(e) { "$uriString is not a valid URI" }
            }
        }
    }

    @IsAction(name = "Open web dictionary", desc = "Opens website dictionary for given word", keys = "CTRL + SHIFT + E", global = true)
    fun openDictionary() {
        configureString("Look up in dictionary...", "Word") {
            URI.create("http://www.thefreedictionary.com/${urlEncodeUtf8(it)}").browse()
        }
    }

    @JvmOverloads
    fun openImageFullscreen(image: File, screen: Screen = getScreenForMouse()) {
        APP.widgetManager.widgets.use<ImageDisplayFeature>(NEW(WINDOW_FULLSCREEN(screen))) { f ->
            val w = f.asIf<Controller>()!!.widget
            val window = w.graphics.scene.window
            val root = window.scene.root

            window.scene.fill = BLACK
            root.asIf<Pane>()?.background = bgr(BLACK)

            root.onEventUp(KEY_PRESSED, ENTER) { window.hide() }
            root.onEventUp(KEY_PRESSED, ESCAPE) { window.hide() }

            window.showingProperty().sync1If({ it }) {
                f.showImage(image)
            }
        }
    }

    /**
     * The check whether file exists, is accessible or of correct type/format is left on the caller and behavior in
     * such cases is undefined.
     */
    @Blocks
    fun printAllImageFileMetadata(file: File) {
        failIfFxThread()

        val title = "Metadata of "+file.path
        val text = try {
            val sb = StringBuilder()
            ImageMetadataReader.readMetadata(file)
                    .directories
                    .forEach {
                        sb.append("\nName: ").append(it.name)
                        it.tags.forEach { tag -> sb.append("\n\t").append(tag.toString()) }
                    }
            title+sb.toString()
        } catch (e: IOException) {
            "$title\n$"+e.stackTraceAsString()
        } catch (e: ImageProcessingException) {
            "$title\n$"+e.stackTraceAsString()
        }
        runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
    }

    @Blocks
    fun printAllAudioItemMetadata(song: Song) {
        failIfFxThread()

        if (song.isFileBased()) {
            printAllAudioFileMetadata(song.getFile()!!)
        } else {
            val text = "Metadata of ${song.uri}\n<only supported for files>"
            runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
        }
    }

    /**
     * The check whether file exists, is accessible or of correct type/format is left on the caller and behavior in
     * such cases is undefined.
     */
    @Blocks
    fun printAllAudioFileMetadata(file: File) {
        failIfFxThread()

        val title = "Metadata of ${file.path}"
        val content = file.readAudioFile()
                .map { af ->
                    "\nHeader:\n"+
                    af.audioHeader.toString().split("\n").joinToString("\n\t")+
                    "\nTag:"+
                    if (af.tag==null) " <none>" else af.tag.fields.asSequence().joinToString("") { "\n\t${it.id}:$it" }
                }
                .getOrSupply { "\n${it.stackTraceAsString()}" }
        val text = title+content
        runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
    }

    @IsAction(name = "Print running java processes")
    fun printJavaProcesses() {
        val text = VirtualMachine.list().joinToString("") {
            "\nVM:\n\tid: ${it.id()}\n\tdisplayName: ${it.displayName()}\n\tprovider: ${it.provider()}"
        }
        runFX { APP.widgetManager.widgets.use<TextDisplayFeature>(NEW) { it.showText(text) } }
    }

    fun browseMultipleFiles(files: Sequence<File>) {
        val fs = files.asSequence().toSet()
        when {
            fs.isEmpty() -> {}
            fs.size==1 -> fs.firstOrNull()?.browse()
            else -> APP.ui.actionPane.orBuild.show(MultipleFiles(fs))
        }
    }

    companion object: KLogging()
}