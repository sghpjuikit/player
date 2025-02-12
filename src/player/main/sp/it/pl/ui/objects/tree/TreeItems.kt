package sp.it.pl.ui.objects.tree

import javafx.stage.Window as WindowFX
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Path
import java.util.Stack
import javafx.collections.FXCollections.emptyObservableList
import javafx.collections.ObservableList
import javafx.collections.transformation.FilteredList
import javafx.event.Event
import javafx.event.EventTarget
import javafx.event.EventType
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.ContextMenu
import javafx.scene.control.Tooltip
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.input.KeyCode.C
import javafx.scene.input.KeyCode.DELETE
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.TransferMode
import javafx.scene.text.TextBoundsType.VISUAL
import javafx.stage.PopupWindow
import javafx.stage.Stage
import javax.swing.filechooser.FileSystemView
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.Playlist
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.layout.Component
import sp.it.pl.layout.ComponentFactory
import sp.it.pl.layout.Container
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetFactory
import sp.it.pl.layout.WidgetSource.OPEN
import sp.it.pl.layout.WidgetUse.ANY
import sp.it.pl.layout.feature.ConfiguringFeature
import sp.it.pl.layout.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.Df
import sp.it.pl.main.Df.FILES
import sp.it.pl.main.IconFA
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.emScaled
import sp.it.pl.main.fileIcon
import sp.it.pl.main.sysClipboard
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.objects.window.dock.isDockWindow
import sp.it.pl.ui.objects.window.stage.Window
import sp.it.pl.ui.objects.window.stage.asAppWindow
import sp.it.pl.ui.objects.window.stage.asLayout
import sp.it.pl.ui.objects.window.stage.isMainWindow
import sp.it.util.HierarchicalBase
import sp.it.util.access.expanded
import sp.it.util.access.toggle
import sp.it.util.async.CURR
import sp.it.util.async.invoke
import sp.it.util.async.limitExecCount
import sp.it.util.async.runVT
import sp.it.util.collections.getElementType
import sp.it.util.collections.setTo
import sp.it.util.collections.setToOne
import sp.it.util.conf.Configurable
import sp.it.util.conf.toConfigurableFx
import sp.it.util.dev.fail
import sp.it.util.file.FileType
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.children
import sp.it.util.file.hasExtension
import sp.it.util.file.isParentOf
import sp.it.util.file.nameOrRoot
import sp.it.util.file.toFast
import sp.it.util.file.type.MimeType
import sp.it.util.file.type.mimeType
import sp.it.util.functional.asIf
import sp.it.util.functional.asIs
import sp.it.util.functional.net
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.system.open
import sp.it.util.system.recycle
import sp.it.util.text.nullIfBlank
import sp.it.util.type.Util.getFieldValue
import sp.it.util.type.nullify
import sp.it.util.type.type
import sp.it.util.ui.IconExtractor
import sp.it.util.ui.createIcon
import sp.it.util.ui.drag.set
import sp.it.util.ui.height
import sp.it.util.ui.isAnyParentOf
import sp.it.util.ui.lay
import sp.it.util.ui.root
import sp.it.util.ui.setMinPrefMaxSize
import sp.it.util.ui.show
import sp.it.util.ui.stackPane
import sp.it.util.ui.x

private val logger = KotlinLogging.logger { }

private const val SELECTION_DISTURBED_KEY = "tree_selection_disturbed"
private const val SELECTION_DISTURBED_STACK_KEY = "tree_selection_disturbed_child"
private val SELECTION_DISTURBED_CLEAR = EventType<TreeSelectionClearEvent>("tree_selection_disturbed_clear")
private val SELECTION_DISTURBED_RESTORE = EventType<TreeSelectionRestoreEvent>("tree_selection_disturbed_restore")

private class TreeSelectionClearEvent(target: EventTarget, val removed: TreeItem<*>): Event(null, target, SELECTION_DISTURBED_CLEAR)
private class TreeSelectionRestoreEvent(target: EventTarget): Event(null, target, SELECTION_DISTURBED_RESTORE)

private fun Any?.orNone(): Any = this ?: "<none>"
private fun <T> seqOf(vararg elements: T) = elements.asSequence()

@OptIn(ExperimentalUnsignedTypes::class)
@Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
fun <T> tree(o: T): TreeItem<T> = when (o) {
   is TreeItem<*> -> o
   is Widget -> WidgetItem(o)
   is WidgetFactory<*> -> SimpleTreeItem(o)
   is Feature -> STreeItem<Any>(o, { APP.widgetManager.factories.getFactories().filter { it.hasFeature(o) }.sortedBy { it.name } })
   is Container<*> -> LayoutItem(o)
   is File -> FileTreeItem(o)
   is Node -> NodeTreeItem(o)
   is Image -> tree(o, tree("Url", o.url.orNone()), "Width${o.width}", "Height${o.height}")
   is Thumbnail.ContextMenuData -> tree("Thumbnail", tree("Data", o.representant.orNone()), tree("Image", o.image.orNone()), tree("Image file", o.iFile.orNone()))
   is Scene -> tree(o, o.root)
   is WindowFX -> STreeItem(o, { seqOf(o.scene) + seqOf(o.asLayout()).filterNotNull() })
   is Window -> tree(o.stage)
   is Name -> STreeItem(o, { o.hChildren.asSequence() }, { o.isHLeaf })
   is Song -> STreeItem(o.uri, { seqOf() }, { true })
   is Playlist -> PlaylistTreeItem(o)
   is PlaylistSongGroup -> STreeItem<Any?>(o, { o.songs.asSequence() }, { o.songs.isEmpty() })
   is MetadataGroup -> STreeItem<Any?>(o, { o.grouped.asSequence() }, { o.grouped.isEmpty() })
   is BooleanArray -> STreeItem<Any>(type<Array<BooleanArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is ByteArray -> STreeItem<Any>(type<Array<ByteArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is UByteArray -> STreeItem<Any>(type<Array<UByteArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is ShortArray -> STreeItem<Any>(type<Array<ShortArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is IntArray -> STreeItem<Any>(type<Array<IntArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is UIntArray -> STreeItem<Any>(type<Array<UIntArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is LongArray -> STreeItem<Any>(type<Array<LongArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is ULongArray -> STreeItem<Any>(type<Array<ULongArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is DoubleArray -> STreeItem<Any>(type<Array<DoubleArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is FloatArray -> STreeItem<Any>(type<Array<FloatArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is CharArray -> STreeItem<Any>(type<Array<CharArray>>().toUi(), { o.asSequence() }, { o.isEmpty() })
   is Array<*> -> STreeItem<Any?>("Array<" + o.toList().getElementType().toUi() + ">", { o.asSequence() }, { o.isEmpty() })
   is List<*> -> STreeItem<Any?>("List<" + o.getElementType().toUi() + ">", { o.asSequence() }, { o.isEmpty() })
   is Set<*> -> STreeItem<Any?>("Set<" + o.getElementType().toUi() + ">", { o.asSequence() }, { o.isEmpty() })
   is Map<*, *> -> STreeItem<Any?>("Map<" + o.keys.getElementType().toUi() + "," + o.values.getElementType().toUi() + ">", { o.asSequence() }, { o.isEmpty() })
   is Map.Entry<*, *> -> STreeItem<Any?>(o.key.toUi(), { sequenceOf(o.value) })
   else -> if (o is HierarchicalBase<*, *>) STreeItem(o, { o.hChildren.asSequence() }, { true }) else SimpleTreeItem(o)
}.let { it as TreeItem<T> }

fun <T> tree(v: T, vararg children: Any): TreeItem<Any> = SimpleTreeItem(v as Any, children.asSequence())

fun tree(v: Any, cs: Collection<Any>): TreeItem<Any> = if (cs is ObservableList<Any>) OTreeItem(v, cs) else SimpleTreeItem(v, cs.asSequence())

fun tree(v: Any, cs: () -> Sequence<Any>): TreeItem<Any> = STreeItem(v, cs)

fun treeApp(): TreeItem<Any> {
   return tree("App",
      tree("Behavior",
         tree("Widgets",
            tree("Types", APP.widgetManager.factories.getComponentFactoriesObservable().toJavaFx().sorted { a,b -> a.name compareTo b.name }),
            tree("Open", { APP.widgetManager.widgets.findAll(OPEN).sortedBy { it.name } }),
            tree("Features", { APP.widgetManager.factories.getFeatures().sortedBy { it.name } })
         ),
         tree("Plugins", APP.plugins.pluginsObservable.toJavaFx().sorted { a,b -> a.info.name compareTo b.info.name })
      ),
      tree("UI",
         tree("Windows", FilteredList(Stage.getWindows()) { it !is Tooltip && it !is ContextMenu }),
         tree("Layouts", { APP.widgetManager.layouts.findAll(OPEN).sortedBy { it.name } })
      ),
      tree("Location", APP.location),
      tree("File system", File.listRoots().map { it.toFast(DIRECTORY) }.map { FileTreeItem(it) }),
      tree(Name.treeOfPaths("Settings", APP.configuration.getConfigs().map { it.group }))
   )
}

fun <T: Any> buildTreeView() = TreeView<T>().initTreeView()

fun <T: Any> TreeView<T>.initTreeView() = apply {

   onEventUp(KEY_PRESSED, ENTER) {
      doAction(selectionModel.selectedItem?.value, {})
   }
   onEventUp(KEY_PRESSED, C, false) { e ->
      if (e.isShortcutDown && !selectionModel.isEmpty) {
         val items = selectionModel.selectedItems
         val strings = items
            .map { computeTreeCellText(it.value) }
         val files = items.asSequence()
            .map { it.value }
            .filterIsInstance<File>()
            .toList()

         if (strings.isNotEmpty()) sysClipboard[Df.PLAIN_TEXT] = strings.joinToString("\n")
         if (files.isNotEmpty()) sysClipboard[FILES] = files
         e.consume()
      }
   }
   onEventUp(KEY_PRESSED, DELETE, false) { e ->
      if (!selectionModel.isEmpty) {
         val items = selectionModel.selectedItems.asSequence()
            .map { it.value }
            .filterIsInstance<File>()
            .toList()
         if (items.isNotEmpty()) {
            items.forEach { it.recycle() }
            e.consume()
         }
      }
   }
   onEventDown(DRAG_DETECTED, PRIMARY, false) { e ->
      if (!selectionModel.isEmpty) {
         val items = selectionModel.selectedItems.asSequence()
            .map { it.value }
            .filterIsInstance<File>()
            .toList()
         if (items.isNotEmpty()) {
            startDragAndDrop(*TransferMode.ANY)[FILES] = items
            e.consume()
         }
      }
   }

   // preserve selection when observable tree item children source changes
   @Suppress("UNCHECKED_CAST")
   rootProperty() syncNonNullWhile { root ->
      Subscription(
         root.onEventDown(SELECTION_DISTURBED_CLEAR) {
            val childStack: Stack<Unit> = properties.getOrPut(SELECTION_DISTURBED_STACK_KEY) { Stack<Unit>() }.asIs()
            childStack.push(Unit)

            if (childStack.size==1) {
               properties[SELECTION_DISTURBED_KEY] = when {
                  selectionModel.isEmpty -> null
                  else -> {
                     val removed = it.removed as TreeItem<T>
                     val removedI = getRow(removed)
                     val remainingSelection = selectionModel.selectedIndices.filter { it!=removedI && !removed.isAnyParentOf(getTreeItem(it)) }
                     when {
                        remainingSelection.isNotEmpty() -> remainingSelection
                        else -> {
                           val i = null
                              ?: removed.previousSibling()?.let { getRow(it) }    // try preceding
                              ?: (removedI + 1).takeIf { it<expandedItemCount }?.let { it - 1 }    // try following

                           i?.let { listOf(it) }
                           if (i!=null) listOf(i) else null
                        }
                     }
                  }
               }
               selectionModel.clearSelection()
            }
         },
         root.onEventDown(SELECTION_DISTURBED_RESTORE) {
            val childStack: Stack<Unit> = properties[SELECTION_DISTURBED_STACK_KEY].asIs()
            childStack.pop()

            if (properties[SELECTION_DISTURBED_KEY]!=null && childStack.isEmpty()) {
               selectionModel.clearSelection()
               val s = properties[SELECTION_DISTURBED_KEY] as List<Int>
               selectionModel.selectIndices(s[0], *IntArray(s.size - 1) { s[it + 1] })
               properties -= SELECTION_DISTURBED_KEY
            }
         }
      )
   }

}

fun computeTreeCellText(o: Any?): String = when (o) {
   null -> "<none>"
   is File -> when {
      // root file shows system name of the file
      o.parentFile == null -> FileSystemView.getFileSystemView().getSystemDisplayName(o).orEmpty().ifBlank { o.nameOrRoot }
      // parent is not visible -> we want absolute path
      // treeItem.parent?.value?.let { it is File && it.isParentOf(o) } != true -> o.absolutePath
      else -> o.nameOrRoot
   }
   is Node -> o.toUi() + (if (o.parent==null && o===o.scene?.root) " (root)" else "")
   is Tooltip -> "Tooltip"
   is MetadataGroup -> "Library songs"
   is PlaylistSongGroup -> "Playlist songs"
   is Image -> "Image"
   is PopupWindow -> "Popup"
   is Scene -> "Scene"
   is ComponentFactory<*> -> o.name
   is WindowFX -> {
      val w = o.asAppWindow()
      if (w==null) {
         o.asIf<Stage>()?.title.nullIfBlank() ?: "Window (generic)"
      } else {
         val ws = w.layout?.getAllWidgets().orEmpty().take(2).toList()
         var n = if (ws.size==1) ws.first().name else "Window " + APP.windowManager.windows.indexOf(w)
         if (w.isMainWindow()) n += " (main)"
         if (w.isDockWindow()) n += " (dock)"
         n
      }
   }
   is HierarchicalBase<*, *> -> computeTreeCellText(o.value)
   else -> o.toUi()
}

@Suppress("UNUSED_PARAMETER")
fun <T> buildTreeCell(t: TreeView<T>) = object: TreeCell<T>() {
   init {
      // disable default expand behavior
      // https://bugs.openjdk.java.net/browse/JDK-8092146
      // https://stackoverflow.com/questions/15509203/disable-treeitems-default-expand-collapse-on-double-click-javafx-2-2
      onEventUp(MouseEvent.ANY) {
         if (!isEmpty && it.clickCount == 2 && it.button==PRIMARY) {
            if (it.eventType == MOUSE_CLICKED) doOnClick(it)
            it.consume()
         }
      }
      onEventDown(MOUSE_CLICKED) { doOnClick(it) }
   }

   override fun updateItem(o: T?, empty: Boolean) {
      if (o!==item) {
         super.updateItem(o, empty)

         if (!empty && o!=null) {
            graphic = computeGraphics(o).also { it?.setMinPrefMaxSize(height, padding.height) }
            graphicTextGap = 0.0
            text = computeText(o)
         } else {
            graphic = null
            text = null
         }

         // pretty, but confusing UX
         // pseudoClassChanged("no-arrow", graphic!=null)
      }
   }

   fun computeText(o: Any?): String = when (o) {
      is File -> when {
         // root file shows system name of the file
         o.parentFile == null -> FileSystemView.getFileSystemView().getSystemDisplayName(o).orEmpty().ifBlank { o.nameOrRoot }
         // parent is not visible -> we want absolute path
         treeItem.parent?.value?.let { it is File && it.isParentOf(o) } != true -> o.absolutePath
         else -> o.nameOrRoot
      }
      else -> computeTreeCellText(o)
   }

   private fun computeGraphics(p: Any): Node? = when (p) {
      is Path -> computeGraphics(p.toFile())
      is File -> stackPane {
         val type = if (treeItem.isLeaf) FILE else FileType(p)
         val glyph = fileIcon(p, type)

         lay += if (glyph == IconFA.FILE) {
            if (p hasExtension "exe" || p.mimeType() == MimeType.`application∕x-ms-shortcut`) null
               ?: IconExtractor.getFileIcon(p)?.net { Thumbnail(it.width x it.height).apply { loadImage(it) }.pane }
               ?: createIcon(IconFA.FILE, 12.0.emScaled).apply { boundsType = VISUAL }
            else
               createIcon(IconFA.FILE, 12.0.emScaled).apply { boundsType = VISUAL }
         } else {
            createIcon(glyph, 12.0.emScaled).apply { boundsType = VISUAL }
         }
      }
      else -> null
   }

   fun doOnClick(e: MouseEvent) {
      if (!isEmpty && item!=null) {
         e.consume()
         when (e.button) {
            PRIMARY -> {
               when (e.clickCount) {
                  1 -> doOnSingleClick(item)
                  2 -> doOnDoubleClick(item)
               }
            }
            SECONDARY -> {
               when (e.clickCount) {
                  1 -> {
                     if (!isSelected) treeView.selectionModel.clearAndSelect(index)
                     showMenu(item, treeView, this, e)
                  }
               }
            }
            else -> Unit
         }
      }
   }

   fun doOnSingleClick(o: Any?) {
      when (o) {
         is HierarchicalBase<*, *> -> doOnSingleClick(o.value)
      }
   }

   fun doOnDoubleClick(o: Any?) {
      if (treeItem.isLeaf) doAction(o) { treeItem?.expanded?.toggle() }
      else treeItem?.expanded?.toggle()
   }

   fun <T> showMenu(o: T?, t: TreeView<T>, n: Node, e: MouseEvent) {
      contextMenuFor(t.selectionModel.selectedItems.map { it.value }).show(n, e)
   }

}

private fun doAction(o: Any?, otherwise: () -> Unit) {
   when (o) {
      is Node -> APP.widgetManager.widgets.use<ConfiguringFeature>(ANY) { it.configureAsync { o.toConfigurableFx() } }
      is WindowFX -> APP.widgetManager.widgets.use<ConfiguringFeature>(ANY) { it.configureAsync { o.toConfigurableFx() } }
      is File -> o.open()
      is Configurable<*> -> APP.widgetManager.widgets.use<ConfiguringFeature>(ANY) { it.configure(o) }
      is TreeItem<*> -> doAction(o.value, otherwise)
      is HierarchicalBase<*, *> -> doAction(o.value, otherwise)
      else -> otherwise()
   }
}

open class OTreeItem<T> constructor(v: T, private val childrenO: ObservableList<out T>): TreeItem<T>(v), DisposableTreeItem {
   private val once = CURR.limitExecCount(1)
   private val childrenDisposer = Disposer()
   private var isDisposed = false

   override fun isLeaf() = if (isDisposed) true else childrenO.isEmpty()
   override fun getChildren(): ObservableList<TreeItem<T>> {
      return super.getChildren().also { children ->
         once {
            childrenDisposer += childrenO.onItemSyncWhile {
               val item = tree(it)
               children += item
               Subscription {
                  val r = root
                  Event.fireEvent(r, TreeSelectionClearEvent(r, item))
                  children -= item.also { it.disposeIfDisposable() }
                  Event.fireEvent(r, TreeSelectionRestoreEvent(r))
               }
            }
         }
      }
   }

   override fun dispose() {
      isDisposed = true
      childrenDisposer()
      nullify(::childrenO)
   }
}

open class SimpleTreeItem<T> constructor(value: T, childrenSeq: Sequence<T> = seqOf()): TreeItem<T>(value) {
   private val childrenAll = childrenSeq.toList()
   private val once = CURR.limitExecCount(1)

   override fun isLeaf() = childrenAll.isEmpty()
   override fun getChildren(): ObservableList<TreeItem<T>> {
      return super.getChildren().also { children ->
         once {
            children setTo childrenAll.map { tree(it) }
         }
      }
   }
}

open class STreeItem<T> constructor(v: T, private val childrenLazy: () -> Sequence<T>, private val isLeafLazy: () -> Boolean = { false }): TreeItem<T>(v) {
   private val once = CURR.limitExecCount(1)

   override fun isLeaf() = isLeafLazy()
   override fun getChildren(): ObservableList<TreeItem<T>> {
      return super.getChildren().also { children ->
         once {
            children setTo childrenLazy().map { tree(it) }
         }
      }
   }
}

class PlaylistTreeItem(value: Playlist): OTreeItem<Any>(value, value)

class NodeTreeItem(value: Node): OTreeItem<Node>(value, (value as? Parent)?.childrenUnmodifiable ?: emptyObservableList())

class WidgetItem(v: Widget): STreeItem<Any>(v, { seqOf(v.ui?.root).filterNotNull() }, { false })

class LayoutItem(v: Component): STreeItem<Component>(v, { if (v is Container<*>) v.children.values.asSequence().filterNotNull() else seqOf() })

class FileTreeItem: SimpleTreeItem<File> {
   private val isLeaf: Boolean
   private var isFirstTimeChildren = true

   @JvmOverloads constructor(value: File, isLeaf: Boolean = value.isFile): super(value) {
      this.isLeaf = isLeaf
      valueProperty() attach { fail { "${FileTreeItem::class} value must never change" } }
   }

   override fun getChildren(): ObservableList<TreeItem<File>> = super.getChildren().apply {
      if (isFirstTimeChildren) {
         isFirstTimeChildren = false

         this setToOne TreeItem<File>(File("Loading..."))
         buildChildren(this@FileTreeItem)
            .ui { this setTo it }
            .onError { this setToOne TreeItem<File>(File("Error...")) }
      }
   }

   override fun isLeaf() = isLeaf

   private fun buildChildren(i: TreeItem<File>) = runVT {
      val dirs = ArrayList<FileTreeItem>()
      val files = ArrayList<FileTreeItem>()
      i.value.children().forEach {
         val f = it.toFast()
         (if (f.isFile) files else dirs) += FileTreeItem(f, f.isFile)
      }
      dirs + files
   }

   fun removeChild(file: File) {
      if (!isFirstTimeChildren) {
         children.removeIf { it.value==file }
         children.forEach { if (it is FileTreeItem) it.removeChild(file) }
      }
   }
}

/** [TreeItem] which requires an explicit [dispose] to be called. */
interface DisposableTreeItem {
   /** Dispose of this and all children. */
   fun dispose()
}

/** Traverses this tree until it finds [DisposableTreeItem] and then calls [DisposableTreeItem.dispose]. */
fun <T> TreeItem<T>.disposeIfDisposable() {
   if (this is DisposableTreeItem) {
      dispose()
   } else {
      // children?.forEach { it.disposeIfDisposable() }   // Causes Stackoverflow or something and hangs application
      getFieldValue<ObservableList<TreeItem<T>>?>(this, "children")?.forEach { it.disposeIfDisposable() }
   }
}