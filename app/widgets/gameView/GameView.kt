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
import sp.it.pl.gui.objects.grid.GridView
import sp.it.pl.gui.objects.grid.GridView.CellSize
import sp.it.pl.gui.objects.hierarchy.Item
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.image.FileCover
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.gui.objects.placeholder.show
import sp.it.pl.gui.objects.tree.buildTreeCell
import sp.it.pl.gui.objects.tree.initTreeView
import sp.it.pl.gui.objects.tree.tree
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.AppError
import sp.it.pl.main.AppErrors
import sp.it.pl.main.FastFile
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.ifErrorNotify
import sp.it.pl.main.isImage
import sp.it.pl.main.onErrorNotify
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
import sp.it.util.async.runIO
import sp.it.util.async.runOn
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.Constraint.FileActor.DIRECTORY
import sp.it.util.conf.Constraint.FileActor.FILE
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.conf.uiNoOrder
import sp.it.util.dev.failIf
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.FileType
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.readProperties
import sp.it.util.file.readTextTry
import sp.it.util.functional.getOr
import sp.it.util.functional.orNull
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.reactive.sync1IfInScene
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
import sp.it.util.units.times
import java.io.File
import java.net.URI
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

   private val cellTextHeight = APP.ui.font.map { 20.0.emScaled }.apply {
      onClose += { unsubscribe() }
      attach { applyCellSize() }
   }

   val cellSize by cv(CellSize.NORMAL).def(name = "Thumbnail size", info = "Size of the thumbnail.").uiNoOrder() attach { applyCellSize() }
   val cellSizeRatio by cv(Resolution.R_1x1).def(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.") attach { applyCellSize() }
   val fitFrom by cv(OUTSIDE).def(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
   val files by cList<File>().def(name = "Location", info = "Location of the library.").only(DIRECTORY)

   val grid = GridView<Item, File>(File::class.java, { it.value }, 50.0, 50.0, 10.0, 10.0)
   val placeholder = lazy {
      Placeholder(IconMD.FOLDER_PLUS, "Click to add directory to library") {
         chooseFile("Choose directory", FileType.DIRECTORY, APP.locationHome, root.scene.window)
            .ifOk { files += it }
      }
   }

   init {
      root.prefSize = 1200.emScaled x 900.emScaled
      root.consumeScrolling()

      root.lay += grid.apply {
         search.field = FileField.PATH
         primaryFilterField = FileField.NAME_FULL
         cellFactory.value = Callback { Cell() }
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
                  val w = grid.cellWidth.value
                  val h = grid.cellHeight.value
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


      files.onChange { viewGames() } on onClose
      files.onChange { grid.isVisible = !files.isEmpty() } on onClose
      files.onChange { placeholder.show(root, files.isEmpty()) } on onClose
      placeholder.show(root, files.isEmpty())

      root.sync1IfInScene {
         applyCellSize()
         viewGames()
      }
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
      grid.cellWidth.value = width.emScaled
      grid.cellHeight.value = height.emScaled + cellTextHeight.value
      grid.horizontalCellSpacing.value = 10.emScaled
      grid.verticalCellSpacing.value = 10.emScaled
      grid.itemsRaw setTo grid.itemsRaw.map { FItem(null, it.value, it.valType) }
   }

   private inner class Cell: GridFileThumbCell() {

      override fun computeCellTextHeight() = cellTextHeight.value

      override fun computeGraphics() {
         super.computeGraphics()

         thumb!!.fitFrom syncFrom fitFrom
         root install appTooltipForData { thumb!!.representant }
      }

      override fun onAction(i: Item, edit: Boolean) = viewGame(i.value)

   }

   private class FItem(parent: Item?, value: File, type: FileType): Item(parent, value, type) {
      override fun createItem(parent: Item, value: File, type: FileType) = FItem(parent, value, type)
   }

   private class Game(dir: File) {
      /** Directory representing the location where game metadata reside. Game's identity. */
      val location: File = dir.absoluteFile
      /** Name of the game. */
      val name: String = location.name
      /** Whether game does not require installation. */
      val isPortable: Boolean = false
      /** Location of the installation. */
      val installLocation: File? = null
      /** Readme file. */
      val infoFile = location/"play-howto.md"
      /** Cover. */
      val cover by lazy {
         FileCover(location.findImage("cover_big") ?: location.findImage("cover"))
      }
      /** Properties. */
      val settings: Map<String, PropVal> by lazy { (location/"game.properties").readProperties().orNull().orEmpty() }

      fun exeFile(block: (File?) -> Unit) = runIO {
         null
            ?: (location/"play.lnk").takeIf { it.exists() }
            ?: (location/"play.bat").takeIf { it.exists() }
            ?: settings["pathAbs"]?.val1?.let { File(it) }?.takeIf { it.exists() }
            ?: settings["path"]?.val1?.let { location/it }?.takeIf { it.exists() }
      }.onDone(FX) {
         block(it.toTry().orNull())
      }

      fun play() {
         exeFile { exe ->
            if (exe==null) {
               AppErrors.push("No launcher is set up.")
            } else {
               val arguments = settings["arguments"]?.valN.orEmpty().filter { it.isNotBlank() }.toTypedArray()
               exe.runAsProgram(*arguments).onErrorNotify {
                  AppError("Unable to launch program $exe", "Reason: ${it.stacktraceAsString}")
               }
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
                              wrappingWidthProperty() syncFrom widthProperty() - 200
                           }
                           lay += fileTree

                        }
                        lay(TOP_RIGHT, Insets(100.0, 0.0, 0.0, 0.0)) += vBox(20.0, TOP_CENTER) {
                           minPrefMaxWidth = 200.0

                           lay += IconFA.EDIT onClick {
                              val file = game.infoFile
                              runIO {
                                 file.createNewFile()
                                 file.edit()
                              }
                           }
                           lay += IconFA.GAMEPAD onClick {
                              game.exeFile { exe ->
                                 if (exe==null)
                                    object: ConfigurableBase<Any?>() {
                                       var file by c(game.location/"exe").only(FILE).def(name = "File", info = "Executable game launcher")
                                    }.configure("Set up launcher") {
                                       val targetDir = it.file.parentDirOrRoot.absolutePath.substringAfter(game.location.absolutePath + File.separator)
                                       val targetName = it.file.name
                                       val link = game.location/"play.bat"
                                       runIO {
                                          failIf(!it.file.exists()) { "Target file does not exist." }
                                          link.writeText("""@echo off${'\n'}start "" /d "$targetDir" "$targetName"""")
                                       }.onDone(FX) {
                                          it.toTry().ifErrorNotify { AppError("Can not set up launcher $link", "Reason:\n${it.stacktraceAsString}") }
                                       }
                                    }
                                 else
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

         cover.loadFile(null)
         titleL.text = ""
         infoT.text = ""
         fileTree.root = null

         runIO {
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
         override fun uri(q: String): URI = URI.create("https://store.steampowered.com/search/?term=$q")
      }

   }
}