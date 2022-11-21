package gameView

import de.jensd.fx.glyphs.GlyphIcons
import java.io.File
import java.net.URI
import java.util.UUID
import javafx.geometry.Insets
import javafx.geometry.Pos.TOP_CENTER
import javafx.geometry.Pos.CENTER
import javafx.geometry.Side
import javafx.scene.Node
import javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.TreeView
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.ESCAPE
import javafx.scene.input.KeyCode.F5
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.KeyEvent.KEY_RELEASED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.StackPane
import javafx.scene.layout.VBox
import javafx.util.Callback
import kotlin.math.round
import mu.KLogging
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.GAME
import sp.it.pl.main.WidgetTags.LIBRARY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppAnimator
import sp.it.pl.main.AppError
import sp.it.pl.main.AppEventLog
import sp.it.pl.main.Events.FileEvent
import sp.it.pl.main.HelpEntries
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconTx
import sp.it.pl.main.IconUN
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.configure
import sp.it.pl.main.emScaled
import sp.it.pl.main.isImage
import sp.it.pl.main.onErrorNotify
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.objects.MdNode
import sp.it.pl.ui.objects.grid.GridFileThumbCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellGap
import sp.it.pl.ui.objects.grid.GridView.CellSize
import sp.it.pl.ui.objects.grid.GridViewSkin
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.FileCover
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.pl.ui.objects.placeholder.show
import sp.it.pl.ui.objects.tree.FileTreeItem
import sp.it.pl.ui.objects.tree.buildTreeCell
import sp.it.pl.ui.objects.tree.initTreeView
import sp.it.pl.ui.objects.tree.tree
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.pl.web.WebSearchUriBuilder
import sp.it.pl.web.WikipediaQBuilder
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.OrV.OrValue.Initial.Override
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.toggle
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.animation.Anim.Companion.animPar
import sp.it.util.async.FX
import sp.it.util.async.runVT
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.ConfigurableBase
import sp.it.util.conf.butElement
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cOr
import sp.it.util.conf.cr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.defInherit
import sp.it.util.conf.noUi
import sp.it.util.conf.only
import sp.it.util.conf.uiNoOrder
import sp.it.util.dev.failIf
import sp.it.util.dev.stacktraceAsString
import sp.it.util.file.FileType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.parentDirOrRoot
import sp.it.util.file.properties.PropVal
import sp.it.util.file.properties.readProperties
import sp.it.util.file.readTextTry
import sp.it.util.file.toFast
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.getOr
import sp.it.util.functional.orNull
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
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
import sp.it.util.text.keys
import sp.it.util.text.nameUi
import sp.it.util.ui.Resolution
import sp.it.util.ui.anchorPane
import sp.it.util.ui.dsl
import sp.it.util.ui.hBox
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
import sp.it.util.ui.typeText
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.millis
import sp.it.util.units.times
import sp.it.util.units.version
import sp.it.util.units.year

class GameView(widget: Widget): SimpleController(widget) {

   val grid = GridView<Item, File>({ it.value }, 50.x2, 10.x2)
   private val cellTextHeight = APP.ui.font.map { 30.0.emScaled }.apply { attach { applyCellSize() } on onClose }

   val gridShowFooter by cOr(APP.ui::gridShowFooter, grid.footerVisible, Override(false), onClose)
      .defInherit(APP.ui::gridShowFooter)
   val gridCellAlignment by cOr<CellGap>(APP.ui::gridCellAlignment, grid.cellAlign, Inherit(), onClose)
      .defInherit(APP.ui::gridCellAlignment)
   val gridCellSize by cv(CellSize.NORMAL).uiNoOrder().attach { applyCellSize() }
      .def(name = "Thumbnail size", info = "Size of the thumbnail.")
   val gridCellSizeRatio by cv(Resolution.R_1x1).attach { applyCellSize() }
      .def(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
   val gridCellCoverFitFrom by cv(OUTSIDE)
      .def(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
   val coverCache by cv(false)
      .def(name = "Use thumbnail cache", info = "Cache thumbnails on disk for faster loading. Useful when items form mostly finite set. Cache is an internal directory split by thumbnail size.")
   val coverCacheId by cv(UUID.randomUUID()).noUi()
      .def(name = "Thumbnail cache id", info = "Isolates image cache of this widget instance by the id. Unique per widget instance.")
   val files by cList<File>()
      .def(name = "Location", info = "Location of the library.").butElement { only(DIRECTORY) }
   val filesRefresh by cr { viewGames() }
      .def(name = "Location (refresh)", info = "Reloads location and reloads the view.")

   val placeholder = lazy {
      Placeholder(IconMD.FOLDER_PLUS, "Click to add directory to library") {
         chooseFile("Choose directory", DIRECTORY, APP.locationHome, root.scene.window)
            .ifOk { files += it }
      }
   }

   init {
      root.prefSize = 1200.emScaled x 900.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.consumeScrolling()

      root.lay += grid.apply {
         search.field = FileField.PATH
         filterPrimaryField = FileField.NAME_FULL
         cellFactory.value = { Cell() }
         selectOn setTo GridView.SelectionOn.values()
         grid.skinProperty() attach {
            it?.asIs<GridViewSkin<*, *>>()?.menuOrder?.dsl {
               item("Refresh", keys = keys("F5")) {
                  viewGames()
               }
            }
         }

         onEventDown(KEY_PRESSED, ENTER) {
            if (!it.isConsumed) {
               val si = grid.selectedItem.value
               if (si!=null) {
                  if (it.isShiftDown) Game(si.value).setupAndPlay()
                  else viewGame(si.value)
               }
            }
         }
         onEventUp(SCROLL) {
            if (it.isShortcutDown) {
               it.consume()

               val isDec = it.deltaY<0 || it.deltaX>0
               val useFreeStyle = it.isShiftDown
               if (useFreeStyle) {
                  val preserveAspectRatio = true
                  val scaleUnit = 1.2
                  val w = grid.cellWidth.value
                  val h = grid.cellHeight.value
                  val nw = 50.0 max round(if (isDec) w*scaleUnit else w/scaleUnit)
                  var nh = 50.0 max round(if (isDec) h*scaleUnit else h/scaleUnit)
                  if (preserveAspectRatio) nh = nw/gridCellSizeRatio.get().ratio
                  applyCellSize(nw, nh)
               } else {
                  gridCellSize.toggle(isDec)
               }
            }
         }
         APP.actionStream.onEvent<FileEvent.Delete> { d ->
            root.children.asSequence().filterIsInstance<GamePane>().firstOrNull()?.handleFileDeleted(d.file)
         } on onClose
      }


      root.onEventDown(KEY_RELEASED, F5) { viewGames() }

      files.onChange { viewGames() } on onClose
      files.onChange { grid.isVisible = !files.isEmpty() } on onClose
      files.onChange { placeholder.show(root, files.isEmpty()) } on onClose
      placeholder.show(root, files.isEmpty())

      root.sync1IfInScene {
         applyCellSize()
         viewGames()
      }
   }

   override fun focus() = grid.requestFocus()

   private fun viewGames() {
      val itemRoot = GItemRoot(files.materialize())
      runVT {
         itemRoot.children()
      }.withAppProgress("Loading game list").ui {
         grid.itemsRaw setTo it
      }
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

   private fun applyCellSize(width: Double = gridCellSize.value.width, height: Double = gridCellSize.value.width/gridCellSizeRatio.value.ratio) {
      grid.cellWidth.value = width.emScaled
      grid.cellHeight.value = height.emScaled + cellTextHeight.value
      grid.horizontalCellSpacing.value = 15.emScaled
      grid.verticalCellSpacing.value = 15.emScaled
      grid.itemsRaw setTo grid.itemsRaw.map { GItem(null, it.value, it.valType) }
   }

   private inner class Cell: GridFileThumbCell() {
      val playPlaceholderPane by lazy {
         stackPane {
            styleClass += "game-cell-play-placeholder"

            lay(CENTER) += Icon(IconFA.GAMEPAD, 48.0).run {
               isFocusTraversable = false
               onClickDo {
                  Game(item.value).setupAndPlay()
               }
               withText(Side.BOTTOM, CENTER, "Play (${keys("SHIFT+ENTER")})").asIs<VBox>()
            }
         }
      }

      val playPlaceholder = Subscribed.delayedFx(350.millis) {
         val p = playPlaceholderPane
         onLayoutChildren = { x, y, w, h ->
            if (it) {
               val mh = h min p.maxHeight
               p.resizeRelocate(x + p.snappedLeftInset(), y + (h-mh-computeCellTextHeight()/2)/2, w-p.snappedLeftInset()-p.snappedRightInset(), mh)
            }
         }
         if (it) root.lay += p
         else root.lay -= p
      }

      override fun computeCellTextHeight() = cellTextHeight.value!!

      override fun updateSelected(selected: Boolean) {
         super.updateSelected(selected)
         playPlaceholder.subscribe(selected)
         gridView.value?.requestLayout() // TODO: remove fix to layout placeholder
      }

      override fun computeGraphics() {
         super.computeGraphics()

         thumb!!.fitFrom syncFrom gridCellCoverFitFrom
         root install appTooltipForData { thumb!!.representant }
      }

      override fun onAction(i: Item, edit: Boolean) = viewGame(i.value)

   }


   private inner class GItemRoot(val locations: List<File>): Item(null, File(""), DIRECTORY) {
      init {
         coverStrategy = CoverStrategy(true, true, false, true, coverCacheId.value.takeIf { coverCache.value })
      }
      override fun createItem(parent: Item, value: File, type: FileType) = GItem(parent, value, type)
      override fun childrenFiles(): Sequence<File> = locations.asSequence()
         .distinct().flatMap { it.children() }
         .map { CachingFile(it) }
         .filter { it.isDirectory && !it.isHidden }
         .sortedBy { it.name }
   }

   private inner class GItem(parent: Item?, value: File, type: FileType): Item(parent, value, type) {
      override fun createItem(parent: Item, value: File, type: FileType) = GItem(parent, value, type)
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
      val readmeFile = location/"README.md"

      /** Cover. */
      val cover by lazy {
         FileCover(location.findImage("cover_big") ?: location.findImage("cover"))
      }

      /** Properties. */
      val settings: Map<String, PropVal> by lazy { (location/"game.properties").readProperties().orNull().orEmpty() }

      fun exeFile(block: (File?) -> Unit) = runVT {
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
               AppEventLog.push("No launcher is set up.")
            } else {
               val arguments = settings["arguments"]?.valN.orEmpty().filter { it.isNotBlank() }.toTypedArray()
               exe.runAsProgram(*arguments).onErrorNotify {
                  AppError("Unable to launch program $exe", "Reason: ${it.stacktraceAsString}")
               }
            }
         }
      }

      fun setupAndPlay() {
         exeFile { exe ->
            if (exe==null) {
               object: ConfigurableBase<Any?>() {
                  var file by c(location/"exe").only(FILE).def(name = "File", info = "Executable game launcher")
               }.configure("Set up launcher") {
                  val targetDir = it.file.parentDirOrRoot.absolutePath.substringAfter(location.absolutePath + File.separator)
                  val targetName = it.file.name
                  val link = location/"play.bat"
                  runVT {
                     failIf(!it.file.exists()) { "Target file does not exist." }
                     link.writeText("""@echo off${'\n'}start "" /d "$targetDir" "$targetName"""")
                  }.onErrorNotify {
                     AppError("Can not set up launcher $link", "Reason:\n${it.stacktraceAsString}")
                  } ui {
                     play()
                  }
               }
            } else {
               play()
            }
         }
      }

   }

   inner class GamePane: StackPane() {
      private lateinit var game: Game
      private val cover = Thumbnail()
      private val titleL = label()
      private val infoT = MdNode()
      private val fileTree = TreeView<File>()
      private val animated = ArrayList<Node>()

      init {
         fileTree.isShowRoot = false
         fileTree.selectionModel.selectionMode = SINGLE
         fileTree.cellFactory = Callback { buildTreeCell(it) }
         fileTree.initTreeView()
         fileTree.propagateESCAPE()
         cover.pane.isMouseTransparent = true
         cover.borderVisible = false
         cover.fitFrom.value = OUTSIDE
         animated += sequenceOf(cover.pane, titleL, infoT, fileTree)

         lay += vBox(20, CENTER) {
            lay += anchorPane {
               minWidth = 0.0
               minPrefMaxHeight = 500.0

               layFullArea += cover.pane
            }
            lay += titleL.apply {
               styleClass += "h3"
            }
            lay(ALWAYS) += hBox(0.0, CENTER) {
               padding = Insets(0.0, 0.0, 0.0, 100.0)
               lay(ALWAYS) += scrollPane {
                     isFitToHeight = true
                     isFitToWidth = true
                     hbarPolicy = NEVER
                     vbarPolicy = NEVER

                     content = vBox(20, CENTER) {
                        lay += infoT
                        lay(ALWAYS) += fileTree.apply {
                           minHeight = 300.emScaled
                        }
                     }
                  }
               lay += vBox(20.0, TOP_CENTER) {
                  minPrefMaxWidth = 200.0

                  lay += IconFA.EDIT icon {
                     val file = game.readmeFile
                     runVT {
                        file.createNewFile()
                        file.edit()
                     }
                  }
                  lay += IconFA.GAMEPAD icon { game.setupAndPlay() }
                  lay += IconUN(0x1f4c1) icon { game.location.open() }
                  lay += IconMD.WIKIPEDIA icon { WikipediaQBuilder(game.name).browse() }
                  lay += IconMD.STEAM icon { SteamQBuilder(game.name).browse() }
                  lay += IconTx("GOG") icon { GogQBuilder(game.name).browse() }

                  animated += children
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
         infoT.text.value = ""
         fileTree.root = null
         fileTree.requestFocus()

         runVT {
            object {
               val title = g.name
               val location = g.location.toFast(DIRECTORY)
               val infoFile = g.readmeFile
               val info = g.readmeFile.readTextTry().getOr("")
               val coverImage = g.cover.getImage(cover.calculateImageLoadSize(), OUTSIDE)
               val triggerLoading = g.settings
            }
         } ui {
            if (g===game) {
               cover.loadImage(it.coverImage)
               infoT.readFile(g.readmeFile)
               fileTree.root = tree(it.location)
               titleL.text = ""

               val typeTitle = typeText(it.title, ' ')
               anim(30.millis*it.title.length) { titleL.text = typeTitle(it) }.delay(150.millis).play()
               animPar(animated) { i, node -> anim(300.millis) { node.opacity = it*it*it }.delay(200.millis*i) }.play()
            }
         }
      }

      fun handleFileDeleted(f: File) {
         fileTree.root?.asIf<FileTreeItem>()?.removeChild(f)
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Game library"
      override val description = "Game library"
      override val descriptionLong = "$description.\nSimple library of games using file system."
      override val icon = IconUN(0x2e2a)
      override val version = version(0, 10, 0)
      override val isSupported = true
      override val year = year(2016)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(LIBRARY, GAME)
      override val summaryActions = HelpEntries.Grid + listOf(
         Entry("Data", "Refresh", F5.nameUi),
         Entry("Grid cell", "Go to detail", ENTER.nameUi),
         Entry("Grid cell", "Play", keys("SHIFT+ENTER")),
         Entry("Game detail", "Back", BACK.nameUi),
         Entry("Game detail", "Back", SECONDARY.nameUi),
         Entry("Game detail", "Back", BACK_SPACE.nameUi),
         Entry("Game detail", "Back", ESCAPE.nameUi),
      )

      private fun File.findImage(imageName: String) = children().find {
         val filename = it.name
         val i = filename.lastIndexOf('.')
         if (i==-1) {
            false
         } else {
            filename.substring(0, i).equals(imageName, ignoreCase = true) && it.isImage()
         }
      }

      private infix fun GlyphIcons.icon(block: (Icon) -> Unit) = Icon(this).size(40.0).onClickDo(block)

      private object GogQBuilder: WebSearchUriBuilder {
         override val icon = IconTx("GOG")
         override val name = "GOG"
         override fun build(q: String): URI = URI.create("https://www.gog.com/games?sort=popularity&page=1&search=$q")
      }
      private object SteamQBuilder: WebSearchUriBuilder {
         override val icon = IconMD.STEAM
         override val name = "Steam"
         override fun build(q: String): URI = URI.create("https://store.steampowered.com/search/?term=$q")
      }

   }

}