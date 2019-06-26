package appLauncher

import com.sun.jna.Native
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShlObj
import com.sun.jna.platform.win32.WinDef
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.util.Callback
import sp.it.pl.gui.objects.grid.GridFileThumbCell
import sp.it.pl.gui.objects.grid.GridFileThumbCell.Loader
import sp.it.pl.gui.objects.grid.GridView
import sp.it.pl.gui.objects.grid.GridView.CellSize.NORMAL
import sp.it.pl.gui.objects.grid.GridView.SelectionOn.KEY_PRESS
import sp.it.pl.gui.objects.grid.GridView.SelectionOn.MOUSE_CLICK
import sp.it.pl.gui.objects.grid.GridView.SelectionOn.MOUSE_HOVER
import sp.it.pl.gui.objects.hierarchy.Item
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.IconFA
import sp.it.pl.main.Widgets
import sp.it.pl.main.installDrag
import sp.it.pl.main.scaleEM
import sp.it.pl.main.withAppProgress
import sp.it.util.Sort.ASCENDING
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.async.IO
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.oneTPExecutor
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.IsConfig
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.only
import sp.it.util.conf.values
import sp.it.util.dev.Dependency
import sp.it.util.file.FileSort.DIR_FIRST
import sp.it.util.file.FileType
import sp.it.util.file.FileType.FILE
import sp.it.util.file.div
import sp.it.util.functional.net
import sp.it.util.functional.nullsLast
import sp.it.util.functional.toUnit
import sp.it.util.inSort
import sp.it.util.math.max
import sp.it.util.reactive.attach1IfNonNull
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.system.Os
import sp.it.util.system.open
import sp.it.util.ui.Resolution
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.validation.Constraint.FileActor
import java.io.File
import java.util.concurrent.atomic.AtomicLong

@Widget.Info(
        author = "Martin Polakovic",
        name = Widgets.APP_LAUNCHER,
        description = "Application menu and launcher",
        version = "0.8.0",
        year = "2016",
        group = OTHER
)
@ExperimentalController(reason = "DirView widget could be improved to be fulfill this widget's purpose. Also needs better UX.")
class AppLauncher(widget: Widget): SimpleController(widget) {

    @IsConfig(name = "Location", info = "Add program")
    private val files by cList<File>().only(FileActor.DIRECTORY)
    private var filesMaterialized = files.materialize()

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnail.")
    private val cellSize by cv(NORMAL) attach { applyCellSize() }
    @IsConfig(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
    private val cellSizeRatio by cv(Resolution.R_1x1) attach { applyCellSize() }
    @IsConfig(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
    private val fitFrom by cv(FitFrom.OUTSIDE)

    private val grid = GridView<Item, File>(File::class.java, { it.value }, cellSize.value.width, cellSize.value.width/cellSizeRatio.value.ratio+CELL_TEXT_HEIGHT, 5.0, 5.0)
    private val imageLoader = Loader(oneTPExecutor())
    private val visitId = AtomicLong(0)

    private var item: Item? = null   // item, children of which are displayed

    @IsConfig(name = "Sort", info = "Sorting effect.")
    private val sort by cv(ASCENDING) attach { applySort() }
    @IsConfig(name = "Sort first", info = "Group directories and files - files first, last or no separation.")
    private val sortFile by cv(DIR_FIRST) attach { applySort() }
    @IsConfig(name = "Sort seconds", info = "Sorting criteria.")
    private val sortBy by cv<FileField<*>>(FileField.NAME).values(FileField.FIELDS) attach { applySort() }
    @Dependency("Must be Config. Accessed in the application by name")
    @IsConfig(name = "Close on launch", info = "Close this widget when it launches a program.")
    private var closeOnLaunch by c(false)
    @IsConfig(name = "Close on right click", info = "Close this widget when right click is detected.")
    private var closeOnRightClick by c(false)

    init {
        root.prefSize = 1000.scaleEM() x 700.scaleEM()

        grid.search.field = FileField.PATH
        grid.primaryFilterField = FileField.NAME_FULL
        grid.selectOn setTo listOf(KEY_PRESS, MOUSE_CLICK, MOUSE_HOVER)
        grid.cellFactory = Callback { Cell() }
        root.lay += grid

        grid.onEventDown(KEY_PRESSED, ENTER, false) { e ->
            grid.selectedItem.value?.let {
                doubleClickItem(it)
                e.consume()
            }
        }
        grid.onEventDown(MOUSE_CLICKED, SECONDARY, false) {
            if (closeOnRightClick) {
                doubleClickItem(null)
                it.consume()
            }
        }

        installDrag(
                root, IconFA.PLUS_SQUARE_ALT, "Add launcher",
                { e -> e.dragboard.hasFiles() },
                { e -> files += e.dragboard.files }
        )

        files.onChange { filesMaterialized = files.materialize() }
        files.onChange { visit() }
        onClose += { disposeItem() }
        onClose += { imageLoader.shutdown() }

        root.sync1IfInScene {
            applyCellSize()
        }
    }

    override fun focus() = grid.skinProperty().attach1IfNonNull { grid.implGetSkin().requestFocus() }.toUnit()

    private fun visit() {
        disposeItem()
        val i = TopItem()
        item = i
        visitId.incrementAndGet()
        runIO {
            i.children().sortedWith(buildSortComparator())
        }.withAppProgress(
                widget.custom_name.value+": Fetching view"
        ) ui {
            grid.itemsRaw setTo it
            grid.implGetSkin().position = i.lastScrollPosition max 0.0
            grid.requestFocus()
        }
    }

    private fun disposeItem() {
        item?.dispose()
        item = null
    }

    private fun doubleClickItem(i: Item?) {
        if (closeOnLaunch) {
            widget.uiTemp.dispose() // TODO: this is for overlayPane inter-op, move out of here and use hide() instead of dispose()
            runFX(250.millis) { i?.value?.open() }
        } else {
            i?.value?.open()
        }
    }

    private fun applyCellSize(width: Double = cellSize.value.width, height: Double = cellSize.value.width/cellSizeRatio.value.ratio) {
        grid.setCellSize(width, height+CELL_TEXT_HEIGHT)
        visit()
    }

    private fun applySort() {
        fut(grid.itemsRaw.materialize()).then(IO) {
            it.sortedWith(buildSortComparator())
        } ui {
            grid.itemsRaw setTo it
        }
    }

    private fun buildSortComparator() = compareBy<Item> { 0 }
            .thenBy { it.valType }.inSort(sortFile.value.sort)
            .thenBy(sortBy.value.comparator<File> { it.inSort(sort.value).nullsLast() }) { it.value }

    private inner class Cell: GridFileThumbCell(imageLoader) {

        override fun computeCellTextHeight() = CELL_TEXT_HEIGHT

        override fun onAction(i: Item, edit: Boolean) = doubleClickItem(i)

    }

    private open class FItem(parent: Item?, value: File?, type: FileType?): Item(parent, value, type) {

        override fun createItem(parent: Item, value: File, type: FileType) = null
                ?: value.getPortableAppExe(type)?.net { FItem(parent, it, FILE) }
                ?: FItem(parent, value, type)

    }

    private inner class TopItem: FItem(null, null, null) {

        override fun childrenFiles() = (filesMaterialized.asSequence()+startMenuPrograms()).distinct()

        override fun getCoverFile() = null

    }

    companion object {

        private const val CELL_TEXT_HEIGHT = 20.0

        fun File.getPortableAppExe(type: FileType) = if (type==FileType.DIRECTORY) File(this, "$name.exe") else null

        fun startMenuPrograms(): Sequence<File> = when (Os.current) {
            Os.WINDOWS -> windowsAppData().walk()
                    .filter { '.' in it.name && !it.name.contains("uninstall", true) }
                    .map { CachingFile(it) }
            else -> sequenceOf()
        }

        fun windowsAppData(): File {
            val pszPath = CharArray(WinDef.MAX_PATH)
            Shell32.INSTANCE.SHGetFolderPath(null, ShlObj.CSIDL_APPDATA, null, ShlObj.SHGFP_TYPE_CURRENT, pszPath)
            val path = Native.toString(pszPath)
            return File(path)/"Microsoft"/"Windows"/"Start Menu"/"Programs"
        }

    }
}