package dirViewer

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.FOLDER_PLUS
import javafx.collections.FXCollections.observableArrayList
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.input.KeyCode.BACK_SPACE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.BACK
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.HBox
import javafx.scene.layout.StackPane
import javafx.util.Callback
import sp.it.pl.gui.objects.grid.GridFileThumbCell
import sp.it.pl.gui.objects.grid.GridFileThumbCell.Loader
import sp.it.pl.gui.objects.grid.GridView
import sp.it.pl.gui.objects.grid.GridView.CellSize.NORMAL
import sp.it.pl.gui.objects.hierarchy.Item
import sp.it.pl.gui.objects.hierarchy.Item.CoverStrategy
import sp.it.pl.gui.objects.placeholder.Placeholder
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.FileFilters
import sp.it.pl.main.FileFlatter
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.installDrag
import sp.it.pl.main.scaleEM
import sp.it.pl.main.showAppProgress
import sp.it.pl.util.Sort.ASCENDING
import sp.it.pl.util.access.VarEnum
import sp.it.pl.util.access.fieldvalue.CachingFile
import sp.it.pl.util.access.fieldvalue.FileField
import sp.it.pl.util.animation.Anim.Companion.anim
import sp.it.pl.util.async.burstTPExecutor
import sp.it.pl.util.async.future.Fut.Companion.fut
import sp.it.pl.util.async.oneTPExecutor
import sp.it.pl.util.async.onlyIfMatches
import sp.it.pl.util.async.runFX
import sp.it.pl.util.async.threadFactory
import sp.it.pl.util.collections.materialize
import sp.it.pl.util.collections.setTo
import sp.it.pl.util.collections.setToOne
import sp.it.pl.util.conf.EditMode
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cList
import sp.it.pl.util.conf.cn
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.file.FileSort.DIR_FIRST
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.FileType.DIRECTORY
import sp.it.pl.util.file.Util.getCommonRoot
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.functional.Util.max
import sp.it.pl.util.functional.nullsLast
import sp.it.pl.util.functional.toUnit
import sp.it.pl.util.functional.traverse
import sp.it.pl.util.inSort
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.attach1IfNonNull
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onChange
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.onEventUp
import sp.it.pl.util.reactive.sync
import sp.it.pl.util.reactive.sync1IfInScene
import sp.it.pl.util.reactive.syncTo
import sp.it.pl.util.system.chooseFile
import sp.it.pl.util.system.edit
import sp.it.pl.util.system.open
import sp.it.pl.util.text.pluralUnit
import sp.it.pl.util.ui.Resolution
import sp.it.pl.util.ui.Util.layHeaderTop
import sp.it.pl.util.ui.image.FitFrom
import sp.it.pl.util.ui.install
import sp.it.pl.util.ui.label
import sp.it.pl.util.ui.lay
import sp.it.pl.util.ui.onHoverOrDragEnd
import sp.it.pl.util.ui.onHoverOrDragStart
import sp.it.pl.util.ui.prefSize
import sp.it.pl.util.ui.setScaleXY
import sp.it.pl.util.ui.x
import sp.it.pl.util.units.millis
import sp.it.pl.util.units.minutes
import sp.it.pl.util.validation.Constraint.FileActor
import java.io.File
import java.util.Stack
import java.util.concurrent.atomic.AtomicLong
import java.util.function.Consumer

@Widget.Info(
        author = "Martin Polakovic",
        name = "Dir Viewer",
        description = "Displays directory hierarchy and files as thumbnails in a vertically scrollable grid. "+"Intended as simple library",
        version = "0.7.0",
        year = "2015",
        group = OTHER
)
class DirViewer(widget: Widget): SimpleController(widget) {

    private val inputFile = inputs.create("Root directory", File::class.java, null, Consumer<File?> {
        if (it!=null && it.isDirectory && it.exists())
            files setToOne it
    })

    @IsConfig(name = "Location", info = "Root directories of the content.")
    private val files by cList<File>().only(FileActor.DIRECTORY)
    private var filesMaterialized = files.materialize()
    @IsConfig(name = "Location joiner", info = "Merges location files into a virtual view.")
    private val fileFlatter by cv(FileFlatter.TOP_LVL)

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    private val cellSize by cv(NORMAL)
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    private val cellSizeRatio by cv(Resolution.R_1x1)
    @IsConfig(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
    private val fitFrom by cv(FitFrom.OUTSIDE)

    @IsConfig(name = "Use composed cover for dir", info = "Display directory cover that shows its content.")
    private val coverLoadingUseComposedDirCover by cv(CoverStrategy.DEFAULT.useComposedDirCover)
    @IsConfig(name = "Use parent cover", info = "Display simple parent directory cover if file has none.")
    private val coverUseParentCoverIfNone by cv(CoverStrategy.DEFAULT.useParentCoverIfNone)

    private val grid = GridView<Item, File>(File::class.java, { it.value }, cellSize.value.width, cellSize.value.width/cellSizeRatio.value.ratio+CELL_TEXT_HEIGHT, 5.0, 5.0)
    private val executorIO = oneTPExecutor()
    private val executorThumbs = burstTPExecutor(8, 1.minutes, threadFactory("dirView-img-thumb", true))
    private val executorImage = burstTPExecutor(8, 1.minutes, threadFactory("dirView-img-full", true))
    private val imageLoader = Loader(executorThumbs, executorImage)
    private val visitId = AtomicLong(0)
    private val placeholder = Placeholder(FOLDER_PLUS, "Click to explore directory") {
        chooseFile("Choose directory", DIRECTORY, APP.DIR_HOME, root.scene.window).ifOk { files setToOne it }
    }
    @IsConfig(name = "File filter", info = "Shows only directories and files passing the filter.")
    private val filter by cv(FileFilters.filterPrimary.name) { FileFilters.toEnumerableValue(it) }
    @IsConfig(name = "Sort", info = "Sorting effect.")
    private val sort by cv(ASCENDING)
    @IsConfig(name = "Sort first", info = "Group directories and files - files first, last or no separation.")
    private val sortFile by cv(DIR_FIRST)
    @IsConfig(name = "Sort seconds", info = "Sorting criteria.")
    private val sortBy by cv(FileField.NAME as FileField<*>) { VarEnum(it) { FileField.FIELDS } }
    @IsConfig(name = "Last visited", info = "Last visited item.", editable = EditMode.APP)
    private var lastVisited by cn<File?>(null).only(FileActor.DIRECTORY)

    private var item: Item? = null   // item, children of which are displayed

    @IsConfig(name = "Show navigation", info = "Whether breadcrumb navigation bar is visible.")
    private val navigationVisible by cv(true)
    private val navigationPane = StackPane()
    private val navigation = Breadcrumbs<Item>(
            {
                when (it) {
                    is TopItem -> when (files.size) {
                        0 -> "No location"
                        1 -> files[0].absolutePath
                        else -> "Location".pluralUnit(files.size)
                    }
                    else -> it.value.name
                }
            },
            { visit(it) }
    )

    init {
        root.prefSize = 1000.scaleEM() x 700.scaleEM()

        grid.search.field = FileField.PATH
        grid.primaryFilterField = FileField.NAME_FULL
        grid.cellFactory = Callback { Cell() }
        root.lay += layHeaderTop(0.0, Pos.CENTER_LEFT, navigationPane, grid)

        placeholder.show(root, files.isEmpty())

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
                    val w = grid.cellWidth
                    val h = grid.cellHeight
                    val nw = max(50.0, Math.rint(if (isInc) w*scaleUnit else w/scaleUnit))
                    var nh = max(50.0, Math.rint(if (isInc) h*scaleUnit else h/scaleUnit))
                    if (preserveAspectRatio) nh = nw/cellSizeRatio.value.ratio
                    applyCellSize(nw, nh)
                } else {
                    if (isInc) cellSize.setPreviousValue()
                    else cellSize.setNextValue()
                }
            }
        }

        // drag & drop
        installDrag(
                root, FOLDER_PLUS, "Explore directory",
                { e -> e.dragboard.hasFiles() },
                { e ->
                    val fs = e.dragboard.files
                    files setTo if (fs.all { it.isDirectory }) fs else listOf(getCommonRoot(fs)!!)
                }
        )

        coverLoadingUseComposedDirCover.attach { revisitCurrent() }
        coverUseParentCoverIfNone.attach { revisitCurrent() }
        sort.attach { applySort() }
        sortFile.attach { applySort() }
        sortBy.attach { applySort() }
        fileFlatter attach { revisitCurrent() }
        cellSize attach { applyCellSize() }
        cellSizeRatio attach { applyCellSize() }
        filter attach { revisitCurrent() }
        navigationVisible sync {
            if (it) navigationPane.children.add(0, navigation)
            else navigationPane.children -= navigation
        }
        files.onChange { filesMaterialized = files.materialize() }
        files.onChange { revisitTop() }
        files.onChange { placeholder.show(root, files.isEmpty()) }
        onClose += { disposeItems() }

        root.sync1IfInScene {
            revisitCurrent()
        }
    }

    private fun visitUp() {
        item?.parent?.let {
            item?.disposeChildren()
            visit(it)
        }
    }

    private fun visit(dir: Item) {
        if (item!=null) item!!.lastScrollPosition = grid.implGetSkin().position
        if (item===dir) return
        if (item!=null && item!!.isHChildOf(dir)) item!!.disposeChildren()
        visitId.incrementAndGet()

        item = dir
        navigation.values setTo dir.traverse { it.parent }.toList().asReversed()
        lastVisited = dir.value
        fut(dir)
                .then(executorIO) {
                    it.children().sortedWith(buildSortComparator())
                }.ui {
                    grid.itemsRaw.setAll(it)

                    if (dir.lastScrollPosition>=0) grid.implGetSkin().position = dir.lastScrollPosition
                    grid.requestFocus()
                }
                .showAppProgress(
                        widget.custom_name.value+": Fetching view"
                )
    }

    override fun focus() = grid.skinProperty().attach1IfNonNull { grid.implGetSkin().requestFocus() }.toUnit()

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
                f = f.parentDir
            }
            val tmpF = f
            val success = topItem.children().any { it.value!=null && it.value==tmpF }
            if (success) {
                path.push(f)
            }

            // Visit the branch
            if (success) {
                executorIO.execute {
                    var item: Item? = topItem
                    while (!path.isEmpty()) {
                        val tmp = path.pop()
                        item = item?.children()?.find { tmp==it.value }
                    }
                    item = item ?: topItem
                    runFX {
                        visit(item)
                    }
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
        grid.setCellSize(width, height+CELL_TEXT_HEIGHT)
        revisitCurrent()
    }

    private fun applySort() {
        fut(grid.itemsRaw.materialize()).then(executorIO) {
            it.sortedWith(buildSortComparator())
        } ui {
            grid.itemsRaw.setAll(it)
        }
    }

    private fun buildSortComparator() = compareBy<Item> { 0 }
            .thenBy { it.valType }.inSort(sortFile.value.sort)
            .thenBy(sortBy.value.comparator<File> { it.inSort(sort.value).nullsLast() }) { it.value }
            .thenBy { it.value.path }

    private inner class Cell: GridFileThumbCell(imageLoader) {
        private val disposer = Disposer()

        override fun computeCellTextHeight() = CELL_TEXT_HEIGHT

        override fun computeGraphics() {
            super.computeGraphics()
            fitFrom syncTo thumb.fitFrom on disposer
            root install appTooltipForData { thumb.representant }
        }

        override fun computeTask(r: Runnable) = onlyIfMatches(r, visitId)

        override fun onAction(i: Item, edit: Boolean) = doubleClickItem(i, edit)

        override fun dispose() {
            disposer()
            super.dispose()
        }
    }

    private open inner class FItem(parent: Item?, value: File?, type: FileType?): Item(parent, value, type) {

        override fun createItem(parent: Item, value: File, type: FileType) = FItem(parent, value, type)

        override fun filterChildFile(f: File) = !f.isHidden && f.canRead() && filter.getValueAsFilter()(f, arrayOf())

    }

    private inner class TopItem: FItem(null, null, null) {

        init {
            coverStrategy = CoverStrategy(coverLoadingUseComposedDirCover.value, coverUseParentCoverIfNone.value)
        }

        override fun childrenFiles() = fileFlatter.value.flatten(filesMaterialized).map { CachingFile(it) as File }

        override fun getCoverFile() = when (children.size) {
            1 -> children.first().value.parentDir?.let { getImageT(it, "cover") }
            else -> null
        }

    }

    private class Breadcrumbs<T>(converter: (T) -> String, onClick: (T) -> Unit): HBox() {
        val values = observableArrayList<T>()!!

        init {
            padding = Insets(10.0)
            spacing = 10.0

            values.onChange {
                val cs = mutableListOf<Label>()
                values.forEach { value ->
                    cs += label(converter(value)) {
                        val a = anim(150.millis) { setScaleXY(it) }.intpl { 1+0.1*it*it }
                        onHoverOrDragStart { a.playOpen() }
                        onHoverOrDragEnd { a.playClose() }
                        onEventDown(MOUSE_CLICKED) { onClick(value) }
                    }
                    cs += label(">")
                }
                if (!cs.isEmpty()) cs.removeAt(cs.size-1)

                children setTo cs
            }
        }

    }

    companion object {
        private const val CELL_TEXT_HEIGHT = 20.0
    }

}