package dirViewer

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS
import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.HBox
import javafx.util.Callback
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.ImagesDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.FileFilters
import sp.it.pl.main.FileFlatter
import sp.it.pl.main.IconFA
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.emScaled
import sp.it.pl.main.installDrag
import sp.it.pl.main.withAppProgress
import sp.it.pl.ui.objects.grid.GridCell
import sp.it.pl.ui.objects.grid.GridFileIconCell
import sp.it.pl.ui.objects.grid.GridFileThumbCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellSize.NORMAL
import sp.it.pl.ui.objects.grid.GridViewSkin
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.hierarchy.Item.CoverStrategy
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.placeholder.Placeholder
import sp.it.pl.ui.objects.placeholder.show
import sp.it.util.Sort.ASCENDING
import sp.it.util.Util.enumToHuman
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.access.toggleNext
import sp.it.util.access.togglePrevious
import sp.it.util.access.v
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.onlyIfMatches
import sp.it.util.async.runIO
import sp.it.util.collections.insertEvery
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
import sp.it.util.conf.EditMode
import sp.it.util.conf.cList
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.conf.readOnlyUnless
import sp.it.util.conf.uiConverter
import sp.it.util.conf.uiNoOrder
import sp.it.util.conf.values
import sp.it.util.dev.failIf
import sp.it.util.file.FileSort.DIR_FIRST
import sp.it.util.file.FileType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.isAnyParentOrSelfOf
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.let_
import sp.it.util.functional.nullsLast
import sp.it.util.functional.recurseBF
import sp.it.util.functional.traverse
import sp.it.util.inSort
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Suppressor
import sp.it.util.reactive.attach
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onChangeAndNow
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.suppressed
import sp.it.util.reactive.suppressing
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.reactive.syncTo
import sp.it.util.system.chooseFile
import sp.it.util.system.edit
import sp.it.util.system.open
import sp.it.util.text.nameUi
import sp.it.util.text.pluralUnit
import sp.it.util.ui.Resolution
import sp.it.util.ui.Util.layHeaderTop
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.install
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.onHoverOrDrag
import sp.it.util.ui.prefSize
import sp.it.util.ui.setScaleXYByTo
import sp.it.util.ui.x
import sp.it.util.units.millis
import java.io.File
import java.util.Stack
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.round

@Widget.Info(
   author = "Martin Polakovic",
   name = "Dir Viewer",
   description = "Displays directory hierarchy and files as thumbnails in a vertically scrollable grid. " + "Intended as simple library",
   version = "0.7.0",
   year = "2015",
   group = OTHER
)
class DirViewer(widget: Widget): SimpleController(widget), ImagesDisplayFeature {

   private val outputSelectedSuppressor = Suppressor(false)
   private val outputSelected = io.o.create<File>("Selected", null)
   private val inputFile = io.i.create<List<File>>("Root directory", listOf()) {
      runIO {
         it.orEmpty().mapNotNull { if (it.isDirectory) it else it.parentFile }.toSet().toList()
      } ui {
         files setTo it
      }
   }

   private val files by cList<File>().only(DIRECTORY)
      .def(name = "Location", info = "Root directories of the content.")
   private val fileFlatter by cv(FileFlatter.TOP_LVL)
      .def(name = "Location joiner", info = "Merges location files into a virtual view.")
   private var filesMaterialized = files.materialize()
   private val filesEmpty = v(true).apply { files.onChangeAndNow { value = files.isEmpty() } }

   private val cellSize by cv(NORMAL).uiNoOrder().attach { applyCellSize() }
      .def(name = "Thumbnail size", info = "Size of the thumbnail.")
   private val cellSizeRatio by cv(Resolution.R_1x1).attach { applyCellSize() }
      .def(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
   private val coverOn by cv(true)
      .def(name = "Thumbnail", info = "Display thumbnail instead of simple icon.")
   private val coverFitFrom by cv(FitFrom.OUTSIDE).readOnlyUnless(coverOn)
      .def(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
   private val coverLoadingUseComposedDirCover by cv(CoverStrategy.DEFAULT.useComposedDirCover).readOnlyUnless(coverOn)
      .def(name = "Use composed cover for dir", info = "Display directory cover that shows its content.")
   private val coverUseParentCoverIfNone by cv(CoverStrategy.DEFAULT.useParentCoverIfNone).readOnlyUnless(coverOn)
      .def(name = "Use parent cover", info = "Display simple parent directory cover if file has none.")
   private val cellTextHeight = APP.ui.font.map(onClose) { 30.0.emScaled }.apply {
      attach { applyCellSize() }
   }

   private val grid = GridView<Item, File>(File::class.java, { it.value }, 50.0, 50.0, 5.0, 5.0)
   private val itemVisitId = AtomicLong(0)
   private var item: Item? = null
   private val placeholder = lazy {
      Placeholder(FOLDER_PLUS, "Click to explore directory") {
         prodUserToSetupLocation()
      }
   }
   private val filter by cv(FileFilters.filterPrimary.name) { FileFilters.toEnumerableValue(it) }
      .def(name = "File filter", info = "Shows only directories and files passing the filter.")
   private val sort by cv(ASCENDING).attach { applySort() }
      .def(name = "Sort", info = "Sorting effect.")
   private val sortFile by cv(DIR_FIRST).attach { applySort() }
      .def(name = "Sort first", info = "Group directories and files - files first, last or no separation.")
   private val sortBy by cv(FileField.NAME.name()).values(FileField.all.map { it.name() } + "PATH_LIBRARY").attach { applySort() }
      .def(name = "Sort seconds", info = "Sorting criteria.").uiConverter { enumToHuman(it) }
   private var lastVisited by cn<File>(null).only(DIRECTORY)
      .def(name = "Last visited", info = "Last visited item.", editable = EditMode.APP)

   private val navigationVisible by cv(true).def(name = "Show navigation", info = "Whether breadcrumb navigation bar is visible.")
   private val navigation = Navigation()

   init {
      root.prefSize = 1000.emScaled x 700.emScaled

      grid.search.field = FileField.PATH
      grid.primaryFilterField = FileField.NAME_FULL
      grid.cellFactory syncFrom coverOn.map { Callback<GridView<Item, File>, GridCell<Item, File>> { _ -> if (it) Cell() else IconCell() } }
      grid.selectedItem sync {
         outputSelectedSuppressor.suppressed {
            failIf(it!=null && it.parent!=item) { "item-parent mismatch" }
            outputSelected.value = it?.value
            item?.lastSelectedChild = grid.implGetSkin()?.selectedCI ?: GridViewSkin.NO_SELECT
         }
      }
      root.lay += layHeaderTop(0.0, CENTER_LEFT, navigation, grid)

      grid.onEventDown(KEY_PRESSED, ENTER) {
         val si = grid.selectedItem.value
         if (si!=null) doubleClickItem(si, it.isShiftDown)
      }
      grid.onEventDown(KEY_PRESSED, BACK_SPACE) { visitUp() }
      grid.onEventDown(MOUSE_CLICKED, SECONDARY) { visitUp() }
      grid.onEventDown(MOUSE_CLICKED, BACK) { visitUp() }
      grid.onEventUp(SCROLL) { e ->
         if (e.isShortcutDown) {
            e.consume()
            val isInc = e.deltaY<0 || e.deltaX>0
            val useFreeStyle = e.isShiftDown
            if (useFreeStyle) {
               val preserveAspectRatio = true
               val scaleUnit = 1.2
               val w = grid.cellWidth.value
               val h = grid.cellHeight.value
               val nw = 50.0 max round(if (isInc) w*scaleUnit else w/scaleUnit)
               var nh = 50.0 max round(if (isInc) h*scaleUnit else h/scaleUnit)
               if (preserveAspectRatio) nh = nw/cellSizeRatio.value.ratio
               applyCellSize(nw, nh)
            } else {
               if (isInc) cellSize.togglePrevious()
               else cellSize.toggleNext()
            }
         }
      }

      installDrag(
         root, FOLDER_PLUS, "Explore directory",
         { it.dragboard.hasFiles() },
         { inputFile.value = it.dragboard.files }
      )

      coverOn.attach { revisitCurrent() }
      coverLoadingUseComposedDirCover.attach { revisitCurrent() }
      coverUseParentCoverIfNone.attach { revisitCurrent() }
      fileFlatter attach { revisitCurrent() }
      filter attach { revisitCurrent() }

      files.onChange { filesMaterialized = files.materialize() }
      files.onChange { revisitTop() }
      filesEmpty sync { placeholder.show(root, it) }
      filesEmpty sync { grid.parent.isVisible = !it }
      onClose += { disposeItems() }

      root.sync1IfInScene {
         applyCellSize()
         revisitCurrent()
      }
   }

   private fun visitUp() {
      item?.parent?.let {
         item?.disposeChildrenContent()
         visit(it)
      }
   }

   private fun visit(dir: Item) {
      item?.lastScrollPosition = grid.implGetSkin().position
      if (item===dir) return
      item?.takeIf { it.isHChildOf(dir) }?.disposeChildrenContent()
      itemVisitId.incrementAndGet()

      item = dir
      navigation.breadcrumbs.values setTo dir.traverse { it.parent }.toList().asReversed()
      lastVisited = dir.value
      val locationsMaterialized = filesMaterialized
      runIO {
         dir.children() let_ { it.sortedWith(buildSortComparator(locationsMaterialized, it)) }
      }.withAppProgress(
         widget.custom_name.value + ": Fetching view"
      ).ui {
         outputSelectedSuppressor.suppressing {
            grid.itemsRaw setTo it
            grid.implGetSkin().position = dir.lastScrollPosition max 0.0
            grid.implGetSkin().select(dir.lastSelectedChild)
         }
      }
   }

   override fun focus() = grid.requestFocus()

   override fun showImages(imgFiles: Collection<File>) {
      inputFile.value = imgFiles.toList()
   }

   /** Visits top/root item. Rebuilds entire hierarchy. */
   private fun revisitTop() {
      disposeItems()
      visit(TopItem())
   }

   /** Visits last visited item. Rebuilds entire hierarchy. */
   private fun revisitCurrent() {
      disposeItems()
      val topItem = TopItem()
      if (lastVisited==null) {
         visit(topItem)
      } else {

         // Build stack of files representing the visited branch
         val path = Stack<File>() // nested items we need to rebuild to get to last visited
         var f = lastVisited
         while (f!=null && topItem.children().none { it.value==f }) {
            path.push(f)
            f = f.parentFile
         }
         val tmpF = f
         val success = topItem.children().any { it.value!=null && it.value==tmpF }
         if (success) {
            path.push(f)
         }

         // Visit the branch
         if (success) {
            runIO {
               var item: Item? = topItem
               while (!path.isEmpty()) {
                  val tmp = path.pop()
                  item = item?.children()?.find { tmp==it.value }
               }
               item ?: topItem
            } ui {
               visit(it)
            }
         } else {
            visit(topItem)
         }
      }
   }

   private fun disposeItems() = item?.hRoot?.dispose()

   private fun doubleClickItem(i: Item, edit: Boolean) {
      if (i.valType==DIRECTORY)
         visit(i)
      else {
         if (edit) i.value.edit()
         else i.value.open()
      }
   }

   private fun applyCellSize(width: Double = cellSize.value.width, height: Double = cellSize.value.width/cellSizeRatio.value.ratio) {
      item?.hRoot?.recurseBF { it.childrenRO().orEmpty() }.orEmpty().forEach { it.disposeCover() }
      grid.itemsRaw.forEach { it.disposeCover() }
      grid.cellWidth.value = width.emScaled
      grid.cellHeight.value = height.emScaled + cellTextHeight.value
      grid.horizontalCellSpacing.value = 10.emScaled
      grid.verticalCellSpacing.value = 10.emScaled
   }

   private fun applySort() {
      val itemsMaterialized = grid.itemsRaw.materialize()
      val locationsMaterialized = filesMaterialized
      runIO {
         itemsMaterialized.sortedWith(buildSortComparator(locationsMaterialized, itemsMaterialized))
      } ui {
         grid.itemsRaw setTo it
      }
   }

   @Suppress("MapGetWithNotNullAssertionOperator")
   private fun buildSortComparator(locationsMaterialized: List<File>, items: List<Item>): Comparator<Item> {
      return when (sortBy.value) {
         "PATH_LIBRARY" -> {
            val childByParent = items.associateWith { c -> locationsMaterialized.find { p -> p.isAnyParentOrSelfOf(c.value) }!! }
            val pathByChild = items.associateWith { c -> c.value.path.substringAfter(childByParent[c]!!.path) }
            compareBy<Item> { 0 }
               .thenBy { it.valType }.inSort(sortFile.value.sort)
               .thenBy { pathByChild[it]!! }
         }
         else -> {
            val sortByValue = FileField.valueOf(sortBy.value)!!
            compareBy<Item> { 0 }
               .thenBy { it.valType }.inSort(sortFile.value.sort)
               .thenBy(sortByValue.comparator<File> { it.inSort(sort.value).nullsLast() }) { it.value }
               .thenBy { it.value.path }
         }
      }
   }

   private fun prodUserToSetupLocation() = chooseFile("Choose directory", DIRECTORY, APP.locationHome, root.scene.window).ifOk { files setToOne it }

   private inner class IconCell: GridFileIconCell() {

      override fun onAction(i: Item, edit: Boolean) = doubleClickItem(i, edit)

      override fun computeCellTextHeight(): Double = cellTextHeight.value

      override fun computeGraphics() {
         super.computeGraphics()
         root install appTooltipForData { item?.value }
      }

   }

   private inner class Cell: GridFileThumbCell() {
      private val disposer = Disposer()

      override fun computeCellTextHeight(): Double = cellTextHeight.value

      override fun computeGraphics() {
         super.computeGraphics()
         thumb!!.fitFrom syncFrom coverFitFrom on disposer
         root install appTooltipForData { thumb!!.representant }
      }

      override fun computeTask(r: () -> Unit) = onlyIfMatches(itemVisitId, r)

      override fun onAction(i: Item, edit: Boolean) = doubleClickItem(i, edit)

      override fun dispose() {
         disposer()
         super.dispose()
      }
   }

   private open inner class FItem(parent: Item?, value: File?, type: FileType?): Item(parent, value, type) {

      override fun createItem(parent: Item, value: File, type: FileType) = FItem(parent, value, type)

      override fun filterChildFile(f: File) = !f.isHidden && f.canRead() && filter.getValueAsFilter().apply(f, arrayOf())

   }

   private inner class TopItem: FItem(null, null, null) {

      init {
         coverStrategy = CoverStrategy(coverLoadingUseComposedDirCover.value, coverUseParentCoverIfNone.value, false, true)
      }

      override fun childrenFiles() = filesMaterialized.filter { it.isDirectory && it.exists() }.let(fileFlatter.value.flatten).map { CachingFile(it) }

      override fun getCoverFile() = children().firstOrNull()?.value?.parentFile?.let { getImageT(it, "cover") }

   }

   private inner class Navigation: HBox() {
      val upIcon = Icon(IconFA.CHEVRON_UP).onClickDo { visitUp() }.tooltip("Go up (${PRIMARY.nameUi} or ${BACK_SPACE.nameUi})")
      val breadcrumbs = Breadcrumbs<Item>(
         {
            when (it) {
               is TopItem -> when {
                  files.isEmpty() -> "No location"
                  else -> "Location".pluralUnit(files.size)
               }
               else -> it.value.nameOrRoot
            }
         },
         {
            if (it is TopItem && files.isEmpty()) prodUserToSetupLocation()
            else visit(it)
         }
      )

      init {
         spacing = 0.0
         alignment = CENTER_LEFT
         syncTo(filesEmpty, navigationVisible) { a, b ->
            if (!a && b) children setTo listOf(upIcon, breadcrumbs)
            else children.clear()
         }
      }
   }

   private class Breadcrumbs<T>(converter: (T) -> String, onClick: (T) -> Unit): HBox() {
      val values = observableArrayList<T>()!!

      init {
         padding = Insets(10.0)
         spacing = 10.0

         values.onChange {
            children setTo values.map { value ->
               label(converter(value)) {
                  val a = anim(150.millis) { setScaleXYByTo(it, 0.0, 5.0) }.intpl { it*it }
                  onHoverOrDrag { a.playFromDir(it) }
                  onEventDown(MOUSE_CLICKED) { onClick(value) }
               }
            }.asSequence().insertEvery(1) { label(">") }
         }
      }

   }

}