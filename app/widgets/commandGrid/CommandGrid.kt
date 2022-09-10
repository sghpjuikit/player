package commandGrid

import java.io.File
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.ScrollEvent.SCROLL
import mu.KLogging
import sp.it.pl.conf.Command
import sp.it.pl.layout.Widget
import sp.it.pl.main.WidgetTags.UTILITY
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconUN
import sp.it.pl.main.FileExtensions.command
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.grid.GridFileThumbCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellGap
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.pane.ShortcutPane
import sp.it.util.access.OrV.OrValue.Initial.Override
import sp.it.util.access.toggle
import sp.it.util.async.future.Fut
import sp.it.util.async.runIO
import sp.it.util.conf.cOr
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.defInherit
import sp.it.util.conf.uiNoOrder
import sp.it.util.dev.fail
import sp.it.util.file.FileType
import sp.it.util.file.FileType.FILE
import sp.it.util.file.children
import sp.it.util.file.div
import sp.it.util.file.hasExtension
import sp.it.util.file.readTextTry
import sp.it.util.functional.andAlso
import sp.it.util.functional.net
import sp.it.util.functional.orNull
import sp.it.util.functional.toUnit
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.Resolution
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.install
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.version
import sp.it.util.units.year

class CommandGrid(widget: Widget): SimpleController(widget) {

   val grid = GridView<Item, File>({ it.value }, 50.emScaled.x2, 5.emScaled.x2)

   val gridShowFooter by cOr(APP.ui::gridShowFooter, grid.footerVisible, Override(false), onClose)
      .defInherit(APP.ui::gridShowFooter)
   val gridCellAlignment by cOr<CellGap>(APP.ui::gridCellAlignment, grid.cellAlign, Override(CellGap.LEFT), onClose)
      .defInherit(APP.ui::gridCellAlignment)
   val cellSize by cv(GridView.CellSize.NORMAL).uiNoOrder().attach { applyCellSize() }
      .def(name = "Thumbnail size", info = "Size of the thumbnail.")
   val cellMaxColumns by cvn(2)
      .def(name = "Maximum width", info = "Maximum number of cells in a horizontal row. Null indicates no limit.")
   val cellSizeRatio by cv(Resolution.R_1x1).attach { applyCellSize() }
      .def(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
   val coverFitFrom by cv(FitFrom.OUTSIDE)
      .def(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
   val cellTextHeight = APP.ui.font.map { 30.0.emScaled }.apply { attach { applyCellSize() } on onClose }

   init {
      root.prefSize = 900.emScaled x 500.emScaled
      root.consumeScrolling()

      grid.cellFactory.value = { Cell() }
      grid.footerVisible.value = false
      grid.cellMaxColumns syncFrom cellMaxColumns
      grid.onEventUp(SCROLL) { e ->
         if (e.isShortcutDown) {
            e.consume()
            val isDec = e.deltaY<0 || e.deltaX>0
            val useFreeStyle = e.isShiftDown
            if (useFreeStyle) {
               val preserveAspectRatio = true
               val scaleUnit = 1.2
               val w = grid.cellWidth.value
               val h = grid.cellHeight.value
               val nw = 50.0 max kotlin.math.round(if (isDec) w*scaleUnit else w/scaleUnit)
               var nh = 50.0 max kotlin.math.round(if (isDec) h*scaleUnit else h/scaleUnit)
               if (preserveAspectRatio) nh = nw/cellSizeRatio.value.ratio
               applyCellSize(nw, nh)
            } else {
               cellSize.toggle(isDec)
            }
         }
      }
      grid.onEventDown(KEY_PRESSED, ENTER) {
         if (!it.isConsumed) {
            grid.selectedItem.value?.doubleClickItem()
            it.consume()
         }
      }

      root.lay += grid

      root.sync1IfInScene {
         applyCellSize()
         runIO {
            userLocation.children().filter { it hasExtension command }.toList()
         } ui {
            grid.itemsRaw += it.map {
               object: Item(null, userLocation/it.name, FILE) {
                  override fun createItem(parent: Item?, value: File?, type: FileType?) = fail { "" }
               }
            }
         }
      }
   }

   override fun focus() = grid.requestFocus()

   private fun applyCellSize(width: Double = cellSize.value.width, height: Double = cellSize.value.width/cellSizeRatio.value.ratio) {
      grid.itemsRaw.forEach { it.disposeCover() }
      grid.cellWidth.value = width.emScaled
      grid.cellHeight.value = height.emScaled + cellTextHeight.value
   }

   private fun Item.cellAction(): Fut<Command> =  value.net { runIO { it.readTextTry().andAlso(Command::ofS).orNull() ?: Command.DoNothing } }

   private fun Item.doubleClickItem() = cellAction().ui { it() }.toUnit()

   private inner class Cell: GridFileThumbCell() {

      private val disposer = Disposer()

      override fun computeCellTextHeight(): Double = cellTextHeight.value

      override fun computeGraphics() {
         super.computeGraphics()
         thumb!!.fitFrom syncFrom coverFitFrom on disposer
         root install appTooltipForData { thumb!!.representant }
      }

      override fun computeTask(r: () -> Unit) = r

      override fun onAction(i: Item, edit: Boolean) = i.doubleClickItem()

      override fun dispose() {
         disposer()
         super.dispose()
      }
   }

   companion object: WidgetCompanion, KLogging() {
      override val name = "Command Grid"
      override val description = "Grid of commands - two-dimensional toolbar"
      override val descriptionLong = "$description. Commands are specified as .spit-command files in widget's user directory."
      override val icon = IconUN(0x2e2a)
      override val version = version(1, 0, 1)
      override val isSupported = true
      override val year = year(2021)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(UTILITY)
      override val summaryActions = listOf<ShortcutPane.Entry>()
   }
}