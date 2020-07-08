package sp.it.pl.plugin.impl

import com.sun.jna.Native
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.ShlObj
import com.sun.jna.platform.win32.WinDef
import javafx.collections.FXCollections.observableArrayList
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.util.Callback
import mu.KLogging
import sp.it.pl.layout.container.ComponentUi
import sp.it.pl.layout.exportFxwl
import sp.it.pl.layout.widget.ExperimentalController
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.AppSearch.Source
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.appTooltipForData
import sp.it.pl.main.emScaled
import sp.it.pl.main.installDrag
import sp.it.pl.main.withAppProgress
import sp.it.pl.plugin.PluginBase
import sp.it.pl.plugin.PluginInfo
import sp.it.pl.ui.objects.autocomplete.ConfigSearch.Entry
import sp.it.pl.ui.objects.grid.GridFileThumbCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellGap.ABSOLUTE
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.KEY_PRESS
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_CLICK
import sp.it.pl.ui.objects.grid.GridView.SelectionOn.MOUSE_HOVER
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.pane.OverlayPane
import sp.it.util.Sort
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.action.IsAction
import sp.it.util.async.IO
import sp.it.util.async.future.Fut
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.c
import sp.it.util.conf.cList
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.min
import sp.it.util.conf.only
import sp.it.util.file.FileType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.div
import sp.it.util.file.isParentOrSelfOf
import sp.it.util.file.parentDirOrRoot
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.functional.nullsLast
import sp.it.util.functional.toUnit
import sp.it.util.inSort
import sp.it.util.math.max
import sp.it.util.reactive.attach
import sp.it.util.reactive.attach1IfNonNull
import sp.it.util.reactive.map
import sp.it.util.reactive.onChange
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.system.Os
import sp.it.util.system.isExecutable
import sp.it.util.system.open
import sp.it.util.system.runAsProgram
import sp.it.util.ui.anchorPane
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.install
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.removeFromParent
import sp.it.util.ui.x
import sp.it.util.units.millis
import java.io.File
import java.util.concurrent.atomic.AtomicLong

class AppSearchPlugin: PluginBase() {

   private val searchDirs by cList<File>().only(DIRECTORY).def(name = "Location", info = "Locations to find applications in.")
   private val searchDepth by cv(Int.MAX_VALUE).min(1).def(name = "Search depth", info = "Max search depth used for each location")
   private val searchSourceApps = observableArrayList<File>()
   private val searchSource = Source("Applications ($name plugin)", searchSourceApps) by { "Run app: ${it.absolutePath}" } toSource {
      Entry.of(
         "Run app: ${it.absolutePath}",
         IconMA.APPS,
         { "Runs application: ${it.absolutePath}" },
         null,
         { it.runAsProgram() }
      )
   }

   /** Application launcher widget factory. Registered only when this plugin is running. */
   val widgetFactory = WidgetFactory(AppLauncher::class, APP.location.widgets/"AppLauncher")

   override fun start() {
      APP.search.sources += searchSource
      findApps()
      APP.widgetManager.factories.register(widgetFactory)
   }

   override fun stop() {
      APP.widgetManager.factories.unregister(widgetFactory)
      APP.search.sources -= searchSource
   }

   @IsAction(name = "Re-index", info = "Updates application executables index")
   private fun findApps() {
      val dirs = searchDirs.materialize()
      runIO {
         (dirs.asSequence().distinct().flatMap { findApps(it) } + startMenuPrograms())
            .filter { !it.name.equals("Desktop.ini", true) }
            .distinct()
            .toList()
      }.ui {
         searchSourceApps setTo it
      }.withAppProgress(
         "$name: Searching for applications"
      )
   }

   private fun findApps(dir: File): Sequence<File> {
      return dir.walkTopDown()
         .maxDepth(searchDepth.value)
         .filter { it.isExecutable() }
   }

   @IsAction(name = "Open program launcher", info = "Opens program launcher widget", keys = "CTRL+P")
   fun openLauncher() {
      val f = userLocation/"MainProgramLauncher.fxwl"
      val c = APP.windowManager.instantiateComponent(f) ?: widgetFactory.create()

      val op = object: OverlayPane<Unit>() {

         init {
            onHidden += {
               c.exportFxwl(f) ui {
                  c.close()
                  removeFromParent()
               }
            }
         }

         override fun show(data: Unit) {
            val componentRoot = c.load() as Pane
            content = anchorPane {
               lay(20) += componentRoot
            }
            if (c is Widget) {
               val parent = this
               val controller = c.controller as AppLauncher
               controller.closeOnProgramOpened = true
               controller.closeOnRightClick = true
               c.uiTemp = object: ComponentUi {
                  override val root = parent
                  override fun show() {}
                  override fun hide() {}
               }
            }
            super.show()
         }

      }
      op.display.value = OverlayPane.Display.SCREEN_OF_MOUSE
      op.displayBgr.value = APP.ui.viewDisplayBgr.value
      op.show(Unit)
      op.makeResizableByUser()
      c.load().apply {
         prefWidth(900.0)
         prefHeight(700.0)
      }
      c.focus()
   }

   @Widget.Info(
      author = "Martin Polakovic",
      name = "App launcher",
      description = "Application menu and launcher",
      version = "0.8.0",
      year = "2016",
      group = Widget.Group.OTHER
   )
   @ExperimentalController(reason = "DirView widget could be improved to be fulfill this widget's purpose. Also needs better UX.")
   class AppLauncher(widget: Widget): SimpleController(widget) {

      private val owner = APP.plugins.getRaw<AppSearchPlugin>()?.plugin!!
      private val grid = GridView<Item, File>(File::class.java, { it.value }, 50.0, 50.0, 50.0, 50.0)
      private val cellTextHeight = APP.ui.font.map(onClose) { 20.0.emScaled }.apply {
         attach { applyCellSize() }
      }

      var closeOnProgramOpened by c(false).def(name = "Close on launch", info = "Close this widget when it launches a program.")
      var closeOnRightClick by c(false).def(name = "Close on right click", info = "Close this widget when right click is detected.")
      private val visitId = AtomicLong(0)
      private var item: Item? = null   // item, children of which are displayed

      init {
         root.prefSize = 1000.emScaled x 700.emScaled

         grid.search.field = FileField.PATH
         grid.primaryFilterField = FileField.PATH
         grid.selectOn setTo listOf(KEY_PRESS, MOUSE_CLICK, MOUSE_HOVER)
         grid.cellFactory.value = Callback { Cell() }
         root.lay += grid

         grid.cellGap.value = ABSOLUTE
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

         root.installDrag(
            IconFA.PLUS_SQUARE_ALT,
            "Add launcher",
            { e -> e.dragboard.hasFiles() },
            { e -> owner.searchDirs += e.dragboard.files }
         )

         owner.searchSourceApps.onChange { visit() }
         onClose += { disposeItem() }

         root.sync1IfInScene {
            applyCellSize()
         }
      }

      override fun focus() = grid.skinProperty().attach1IfNonNull { grid.requestFocus() }.toUnit()

      private fun visit() {
         disposeItem()
         val i = TopItem()
         item = i
         visitId.incrementAndGet()
         runIO {
            i.children().sortedWith(buildSortComparator())
         }.withAppProgress(
            widget.custom_name.value + ": Fetching view"
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
         if (closeOnProgramOpened) {
            widget.uiTemp.root.asIf<OverlayPane<*>>()?.hide()
            runFX(250.millis) { i?.value?.open() }
         } else {
            i?.value?.open()
         }
      }

      private fun applyCellSize() {
         grid.cellWidth.value = 300.emScaled
         grid.cellHeight.value = 50.emScaled + cellTextHeight.value
         grid.horizontalCellSpacing.value = 30.emScaled
         grid.verticalCellSpacing.value = 15.emScaled
         visit()
      }

      private fun applySort() {
         Fut.fut(grid.itemsRaw.materialize()).then(IO) {
            it.sortedWith(buildSortComparator())
         } ui {
            grid.itemsRaw setTo it
         }
      }

      private fun buildSortComparator(): Comparator<Item> {
         val byParent = owner.searchSourceApps.groupBy {
            if (windowsAppDataDirectory.isParentOrSelfOf(it)) null
            else it.parent
         }.mapValues {
            it.value.sortedBy { FileField.NAME_FULL.getOf(it) }
         }
         return compareBy<Item> { 0 }
            .thenBy { byParent[it.value.parent]?.takeIf { it.size>1 }?.first() ?: it.value }
            .thenBy(FileField.NAME.comparator<File> { it.inSort(Sort.ASCENDING).nullsLast() }) { it.value }
      }

      private inner class Cell: GridFileThumbCell() {

         override fun computeGraphics() {
            super.computeGraphics()
            thumb!!.fitFrom.value = FitFrom.INSIDE
            root install appTooltipForData { thumb!!.representant }
         }

         override fun computeName(item: Item) = item.value.parentDirOrRoot.name + " > " + item.value.name

         override fun computeCellTextHeight() = cellTextHeight.value!!

         override fun onAction(i: Item, edit: Boolean) = doubleClickItem(i)

      }

      private open class FItem(parent: Item?, value: File?, type: FileType?): Item(parent, value, type) {

         override fun createItem(parent: Item, value: File, type: FileType) = null
            ?: value.getPortableAppExe(type)?.net { FItem(parent, it, FileType.FILE) }
            ?: FItem(parent, value, type)

      }

      private inner class TopItem: FItem(null, null, null) {

         init {
            coverStrategy = CoverStrategy(false, false, true, true)
         }

         override fun childrenFiles() = owner.searchSourceApps/*.filter { !it.name.contains("unins", true) }*/.asSequence()

         override fun getCoverFile() = null

      }

   }

   companion object: KLogging(), PluginInfo {
      override val name = "App Search"
      override val description = "Provides application search/start capability to application search as well as application launcher widget"
      override val isSupported = true
      override val isSingleton = true
      override val isEnabledByDefault = false

      fun File.getPortableAppExe(type: FileType) = if (type==DIRECTORY) File(this, "$name.exe") else null

      fun startMenuPrograms(): Sequence<File> = when (Os.current) {
         Os.WINDOWS -> windowsAppDataDirectory.walk().filter { '.' in it.name && !it.name.contains("uninstall", true) }
         else -> sequenceOf()
      }

      private val windowsAppDataDirectory by lazy {
         val pszPath = CharArray(WinDef.MAX_PATH)
         Shell32.INSTANCE.SHGetFolderPath(null, ShlObj.CSIDL_APPDATA, null, ShlObj.SHGFP_TYPE_CURRENT, pszPath)
         val path = Native.toString(pszPath)
         File(path)/"Microsoft"/"Windows"/"Start Menu"/"Programs"
      }

   }
}