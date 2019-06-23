@file:Suppress("ConstantConditionIf")

package gameView

import de.jensd.fx.glyphs.GlyphIcons
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.Node
import javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.StackPane
import javafx.scene.text.TextAlignment.JUSTIFY
import javafx.util.Callback
import mu.KLogging
import sp.it.pl.gui.objects.grid.GridFileThumbCell
import sp.it.pl.gui.objects.grid.GridFileThumbCell.Loader
import sp.it.pl.gui.objects.grid.GridView
import sp.it.pl.gui.objects.grid.GridView.CellSize
import sp.it.pl.gui.objects.hierarchy.Item
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.image.cover.FileCover
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.gui.objects.tree.buildTreeCell
import sp.it.pl.gui.objects.tree.initTreeView
import sp.it.pl.gui.objects.tree.tree
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.FastFile
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.configure
import sp.it.pl.main.isImage
import sp.it.pl.main.scaleEM
import sp.it.pl.main.withAppProgress
import sp.it.pl.web.WebSearchUriBuilder
import sp.it.pl.web.WikipediaQBuilder
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.minus
import sp.it.util.access.toggleNext
import sp.it.util.access.togglePrevious
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.animPar
import sp.it.util.async.FX
import sp.it.util.async.NEW
import sp.it.util.async.burstTPExecutor
import sp.it.util.async.runNew
import sp.it.util.async.runOn
import sp.it.util.async.threadFactory
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.only
import sp.it.util.dev.failIf
import sp.it.util.file.FileType
import sp.it.util.file.Properties
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.readTextTry
import sp.it.util.functional.getOr
import sp.it.util.functional.net
import sp.it.util.math.max
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.reactive.syncFrom
import sp.it.util.system.browse
import sp.it.util.system.chooseFile
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.system.runAsProgram
import sp.it.util.ui.Resolution
import sp.it.util.ui.anchorPane
import sp.it.util.ui.image.FitFrom.OUTSIDE
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.layFullArea
import sp.it.util.ui.minPrefMaxHeight
import sp.it.util.ui.minPrefMaxWidth
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.stackPane
import sp.it.util.ui.text
import sp.it.util.ui.typeText
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.minutes
import sp.it.util.units.times
import sp.it.util.validation.Constraint.FileActor.DIRECTORY
import sp.it.util.validation.Constraint.FileActor.FILE
import java.io.File
import java.net.URI
import java.util.HashMap
import kotlin.math.round

@Widget.Info(
        name = "GameView",
        author = "Martin Polakovic",
        howto = "",
        description = "Game library.",
        notes = "",
        version = "0.9.0",
        year = "2016",
        group = Widget.Group.OTHER
)
class GameView(widget: Widget): SimpleController(widget) {

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    val cellSize by cv(CellSize.NORMAL) attach { applyCellSize() }
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    val cellSizeRatio by cv(Resolution.R_1x1) attach { applyCellSize() }
    @IsConfig(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
    val fitFrom by cv(OUTSIDE)
    @IsConfig(name = "Location", info = "Location of the library.")
    val files by cList<File>().only(DIRECTORY)

    val grid = GridView<Item, File>(File::class.java, { it.value }, cellSize.value.width, cellSize.value.width/cellSizeRatio.value.ratio+CELL_TEXT_HEIGHT, 10.0, 10.0)
    val imageLoader = Loader(burstTPExecutor(Runtime.getRuntime().availableProcessors()/2 max 1, 1.minutes, threadFactory("gameView-img-loader", true)))
    val placeholder = Placeholder(IconMD.FOLDER_PLUS, "Click to add directory to library") {
        chooseFile("Choose directory", FileType.DIRECTORY, APP.DIR_HOME, root.scene.window)
                .ifOk { files += it }
    }

    init {
        root.prefSize = 1000.scaleEM() x 700.scaleEM()
        root.consumeScrolling()

        files.onChange { viewGames() } on onClose
        files.onChange { placeholder.show(root, files.isEmpty()) } on onClose

        root.lay += grid.apply {
            search.field = FileField.PATH
            primaryFilterField = FileField.NAME_FULL
            cellFactory = Callback { Cell() }
            onEventDown(KEY_PRESSED, ENTER) {
                val si = grid.selectedItem.value
                if (si!=null) viewGame(si.value)
            }
            onEventUp(SCROLL) {
                if (it.isShortcutDown) {
                    it.consume()

                    val isInc = it.deltaY<0 || it.deltaX>0
                    val useFreeStyle = it.isShiftDown
                    if (useFreeStyle) {
                        val preserveAspectRatio = true
                        val scaleUnit = 1.2
                        val w = grid.cellWidth
                        val h = grid.cellHeight
                        val nw = 50.0 max round(if (isInc) w*scaleUnit else w/scaleUnit)
                        var nh = 50.0 max round(if (isInc) h*scaleUnit else h/scaleUnit)
                        if (preserveAspectRatio) nh = nw/cellSizeRatio.get().ratio
                        applyCellSize(nw, nh)
                    } else {
                        if (isInc) cellSize.togglePrevious()
                        else cellSize.toggleNext()
                    }
                }
            }
        }

        onClose += { imageLoader.shutdown() }

        placeholder.show(root, files.isEmpty())
        viewGames()
    }

    private fun viewGames() {
        runOn(NEW) {
            files.asSequence()
                    .distinct()
                    .flatMap { it.children() }
                    .map { CachingFile(it) }
                    .filter { it.isDirectory && !it.isHidden }
                    .sortedBy { it.name }
                    .map { FItem(null, it, FileType.DIRECTORY) }
                    .materialize()
        }.ui {
            grid.itemsRaw setTo it
        }.withAppProgress("Loading game list")
    }

    fun viewGame(game: File) {
        AppAnimator.closeAndDo(grid) {
            val gamePane = GamePane()
            root.lay += gamePane
            AppAnimator.openAndDo(gamePane) {
                gamePane.open(game)
            }
        }
    }

    private fun applyCellSize(width: Double = cellSize.value.width, height: Double = cellSize.value.width/cellSizeRatio.value.ratio) {
        grid.setCellSize(width, height+CELL_TEXT_HEIGHT)
        grid.itemsRaw setTo grid.itemsRaw.map { FItem(null, it.value, it.valType) }
    }

    private inner class Cell: GridFileThumbCell(imageLoader) {

        override fun computeCellTextHeight() = CELL_TEXT_HEIGHT

        override fun computeGraphics() {
            super.computeGraphics()

            thumb.fitFrom syncFrom fitFrom
            root install appTooltipForData { thumb.representant }
        }

        override fun onAction(i: Item, edit: Boolean) = viewGame(i.value)

    }

    private class FItem(parent: Item?, value: File, type: FileType): Item(parent, value, type) {
        override fun createItem(parent: Item, value: File, type: FileType) = FItem(parent, value, type)
    }

    private class Game(dir: File) {
        /** Directory representing the location where game metadata reside. Game's identity. */
        val location = dir.absoluteFile!!
        /** Name of the game.. */
        val name = location.name!!
        /** Whether game does not require installation. */
        val isPortable: Boolean = false
        /** Location of the installation. */
        val installLocation: File? = null
        /** Readme file. */
        val infoFile = location/"play-howto.md"
        /** Cover. */
        val cover by lazy { FileCover(location.findImage("cover_big") ?: location.findImage("cover"), "") }
        val settings: Map<String, String> by lazy {
            val f = location/"game.properties"
            if (f.exists()) Properties.load(f) else HashMap()
        }

        fun exeFile(): File? = null
                ?: (location/"play.lnk").takeIf { it.exists() }
                ?: (location/"play.bat").takeIf { it.exists() }
                ?: settings["pathAbs"]?.net { File(it) }
                ?: settings["path"]?.net { location/it }

        fun play() {
            val file = exeFile()
            if (file==null) {
                APP.ui.messagePane.orBuild.show("No launcher is set up.")
            } else {
                val arguments = settings["arguments"]
                        ?.let { it.replace(", ", ",").split(",").filter { it.isNotBlank() }.map { "-$it" } }
                        .orEmpty()
                file.runAsProgram(*arguments.toTypedArray()) ui {
                    it.ifError { APP.ui.messagePane.orBuild.show("Unable to launch program $file. Reason: ${it.message}") }
                }
            }
        }

    }

    inner class GamePane: StackPane() {
        private lateinit var game: Game
        private val cover = Thumbnail()
        private val titleL = label()
        private val infoT = text()
        private val fileTree = TreeView<File>()
        private val animated = ArrayList<Node>()

        init {
            titleL.style = "-fx-font-size: 20;"
            fileTree.isShowRoot = false
            fileTree.selectionModel.selectionMode = SINGLE
            fileTree.cellFactory = Callback { buildTreeCell(it) }
            fileTree.initTreeView()
            fileTree.propagateESCAPE()
            cover.pane.isMouseTransparent = true
            cover.borderVisible = false
            cover.fitFrom.value = OUTSIDE
            animated += sequenceOf(cover.pane, titleL, infoT, fileTree)

            lay += scrollPane {
                isFitToHeight = false
                isFitToWidth = true
                hbarPolicy = NEVER
                vbarPolicy = AS_NEEDED

                content = vBox(20, CENTER) {
                    lay += anchorPane {
                        minPrefMaxHeight = 500.0

                        layFullArea += cover.pane
                    }
                    lay += stackPane {
                        lay += vBox(0.0, CENTER) {
                            lay += titleL
                            lay += stackPane {
                                lay(CENTER, Insets(0.0, 200.0, 100.0, 100.0)) += vBox(20, CENTER) {

                                    lay += infoT.apply {
                                        textAlignment = JUSTIFY
                                        wrappingWidthProperty() syncFrom widthProperty()-200
                                    }
                                    lay += fileTree

                                }
                                lay(TOP_RIGHT, Insets(100.0, 0.0, 0.0, 0.0)) += vBox(20.0, TOP_CENTER) {
                                    minPrefMaxWidth = 200.0

                                    lay += IconFA.EDIT onClick {
                                        val file = game.infoFile
                                        runNew {
                                            file.createNewFile()
                                            file.edit()
                                        }
                                    }
                                    lay += IconFA.GAMEPAD onClick {
                                        val exeFile = game.exeFile()
                                        if (exeFile==null) {
                                            object: ConfigurableBase<Any?>() {
                                                @IsConfig(name = "File") var file by c(game.location/"exe").only(FILE)
                                            }.configure("Set up launcher") {
                                                val targetDir = it.file.parentDirOrRoot.absolutePath.substringAfter(game.location.absolutePath+File.separator)
                                                val targetName = it.file.name
                                                val link = game.location/"play.bat"
                                                runNew {
                                                    failIf(!it.file.exists()) { "Target file does not exist." }
                                                    link.writeText("""@echo off${'\n'}start "" /d "$targetDir" "$targetName"""")
                                                }.onDone(FX) {
                                                    it.toTry().ifError {
                                                        APP.ui.messagePane.orBuild.show("can ot set up launcher $link\\nReason:\n$it")
                                                    }
                                                }
                                            }
                                        } else {
                                            game.play()
                                        }
                                    }
                                    lay += IconFA.FOLDER onClick { game.location.open() }
                                    lay += IconMD.WIKIPEDIA onClick { WikipediaQBuilder(game.name).browse() }
                                    lay += IconMD.STEAM onClick { SteamQBuilder(game.name).browse() }

                                    animated += children
                                }
                            }
                        }
                    }
                }
            }

            onEventDown(MOUSE_CLICKED, SECONDARY) { close() }
            onEventDown(MOUSE_CLICKED, BACK) { close() }
            onEventDown(KEY_PRESSED, ESCAPE) { close() }
            onEventDown(KEY_PRESSED, BACK_SPACE) { close() }

            animated.forEach { it.opacity = 0.0 }
        }

        fun close() {
            AppAnimator.closeAndDo(this) {
                root.children -= this
                AppAnimator.openAndDo(grid) {}
            }
        }

        fun open(gFile: File) {
            val isSame = ::game.isInitialized && game.location==gFile
            if (isSame) return
            val g = Game(gFile)
            game = g

            cover.loadImage(null as File?)
            titleL.text = ""
            infoT.text = ""
            fileTree.root = null

            runNew {
                object {
                    val title = g.name
                    val location = FastFile(g.location.path, true, false)
                    val info = g.infoFile.readTextTry().getOr("")
                    val coverImage = g.cover.getImage(cover.calculateImageLoadSize())
                    val triggerLoading = g.settings
                }
            } ui {
                if (g===game) {
                    cover.loadImage(it.coverImage)
                    infoT.text = it.info
                    fileTree.root = tree(it.location)
                    titleL.text = ""

                    val typeTitle = typeText(it.title, ' ')
                    anim(30.millis*it.title.length) { titleL.text = typeTitle(it) }.delay(150.millis).play()
                    animPar(animated) { i, node -> anim(300.millis) { node.opacity = it*it*it }.delay(200.millis*i) }.play()
                }
            }
        }
    }

    companion object: KLogging() {

        private const val CELL_TEXT_HEIGHT = 20.0
        private const val ICON_SIZE = 40.0

        private fun File.findImage(imageName: String) = children().find {
            val filename = it.name
            val i = filename.lastIndexOf('.')
            if (i==-1) {
                false
            } else {
                filename.substring(0, i).equals(imageName, ignoreCase = true) && it.isImage()
            }
        }

        private infix fun GlyphIcons.onClick(block: (MouseEvent) -> Unit) = Icon(this).size(ICON_SIZE).onClickDo(block)

        object SteamQBuilder: WebSearchUriBuilder {
            override val name = "Steam"
            override fun uri(q: String) = URI.create("https://store.steampowered.com/search/?term=$q")
        }

    }
}