package sp.it.pl.gui.objects.tree

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
import javafx.scene.input.DataFormat
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import javafx.stage.PopupWindow
import javafx.stage.Stage
import mu.KotlinLogging
import org.reactfx.Subscription
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistSongGroup
import sp.it.pl.gui.objects.contextmenu.ValueContextMenu
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.gui.objects.popover.PopOver
import sp.it.pl.gui.objects.window.stage.Window
import sp.it.pl.gui.objects.window.stage.asWindowOrNull
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.WidgetSource
import sp.it.pl.layout.widget.WidgetUse.ANY
import sp.it.pl.layout.widget.WidgetUse.OPEN_LAYOUT
import sp.it.pl.layout.widget.WidgetUse.OPEN_STANDALONE
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.isValidSkinFile
import sp.it.pl.main.isValidWidgetFile
import sp.it.pl.service.Service
import sp.it.pl.util.HierarchicalBase
import sp.it.pl.util.Util.enumToHuman
import sp.it.pl.util.access.toggle
import sp.it.pl.util.async.executor.ExecuteN
import sp.it.pl.util.async.invoke
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.Configurable.configsFromFxPropertiesOf
import sp.it.pl.util.dev.fail
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.hasExtension
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.nameOrRoot
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.functional.getElementType
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.functional.setTo
import sp.it.pl.util.graphics.createIcon
import sp.it.pl.util.graphics.isAnyParentOf
import sp.it.pl.util.graphics.root
import sp.it.pl.util.reactive.Disposer
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.onItemSyncWhile
import sp.it.pl.util.reactive.syncNonNullWhile
import sp.it.pl.util.system.open
import sp.it.pl.util.text.plural
import sp.it.pl.util.type.Util.getFieldValue
import sp.it.pl.util.type.nullify
import java.io.File
import java.nio.file.Path
import java.util.ArrayList
import java.util.Stack
import kotlin.streams.asSequence

private val logger = KotlinLogging.logger { }
private val globalContextMenu by lazy { ValueContextMenu<Any?>() }

private const val SELECTION_DISTURBED_KEY = "tree_selection_disturbed"
private const val SELECTION_DISTURBED_STACK_KEY = "tree_selection_disturbed_child"
private val SELECTION_DISTURBED_CLEAR = EventType<TreeSelectionClearEvent>("tree_selection_disturbed_clear")
private val SELECTION_DISTURBED_RESTORE = EventType<TreeSelectionRestoreEvent>("tree_selection_disturbed_restore")
private class TreeSelectionClearEvent(target: EventTarget, val removed: TreeItem<*>): Event(null, target, SELECTION_DISTURBED_CLEAR)
private class TreeSelectionRestoreEvent(target: EventTarget): Event(null, target, SELECTION_DISTURBED_RESTORE)

private fun Any?.orNone(): Any = this ?: "<none>"

@Suppress("UNCHECKED_CAST", "RemoveExplicitTypeArguments")
fun <T> tree(o: T): TreeItem<T> = when (o) {
    is TreeItem<*> -> o
    is Widget -> WidgetItem(o)
    is WidgetFactory<*> -> SimpleTreeItem(o)
    is Widget.Group -> STreeItem<Any>(o, { APP.widgetManager.widgets.findAll(WidgetSource.OPEN).asSequence().filter { it.info.group()==o }.sortedBy { it.name } })
    is WidgetSource -> STreeItem<Any>(o, { APP.widgetManager.widgets.findAll(o).asSequence().sortedBy { it.name } })
    is Feature -> STreeItem<Any>(o, { APP.widgetManager.factories.getFactories().filter { it.hasFeature(o) }.sortedBy { it.nameGui() } })
    is Container<*> -> LayoutItem(o)
    is File -> FileTreeItem(o)
    is Node -> NodeTreeItem(o)
    is Image -> tree("Image", tree("Url", o.url.orNone()), "Width${o.width}", "Height${o.height}")
    is Thumbnail.ContextMenuData -> tree("Thumbnail", tree("Data", o.representant.orNone()), tree("Image", o.image.orNone()), tree("Image file", o.iFile.orNone()))
    is Scene -> tree("Scene", o.root)
    is javafx.stage.Window -> STreeItem(o, { seqOf(o.scene)+seqOf(o.asWindowOrNull()?.layout).filterNotNull() })
    is Window -> tree(o.stage)
    is PopOver<*> -> STreeItem(o, { seqOf(o.scene.root) })
    is Name -> STreeItem(o, { o.hChildren.asSequence() }, { o.hChildren.isEmpty() })
    is Song -> STreeItem(o.uri, { seqOf() }, { true })
    is MetadataGroup -> STreeItem<Any?>("Library songs", { o.grouped.asSequence() }, { o.grouped.isEmpty() })
    is PlaylistSongGroup -> STreeItem<Any?>("Playlist songs", { o.songs.asSequence() }, { o.songs.isEmpty() })
    is List<*> -> STreeItem<Any?>("List of "+APP.className.get(o.getElementType()).plural(), { o.asSequence() }, { o.isEmpty() })
    is Set<*> -> STreeItem<Any?>("Set of "+APP.className.get(o.getElementType()).plural(), { o.asSequence() }, { o.isEmpty() })
    else -> if (o is HierarchicalBase<*, *>) STreeItem(o, { o.getHChildren().asSequence() }, { true }) else SimpleTreeItem(o)
}.let { it as TreeItem<T> }

fun <T> tree(v: T, vararg children: Any): TreeItem<Any> = SimpleTreeItem(v as Any, children.asSequence())

fun tree(v: Any, cs: Collection<Any>): TreeItem<Any> = if (cs is ObservableList<Any>) OTreeItem(v, cs) else SimpleTreeItem(v, cs.asSequence())

fun tree(v: Any, cs: () -> Sequence<Any>): TreeItem<Any> = STreeItem(v, cs)

fun treeApp(): TreeItem<Any> {
    return tree("App",
            tree("Behavior",
                    tree("Widgets",
                            tree("Categories", Widget.Group.values().asList()),
                            tree("Types", { APP.widgetManager.factories.getFactories().sortedBy { it.nameGui() } }),
                            tree("Open", { seqOf(ANY, OPEN_LAYOUT, OPEN_STANDALONE) }),
                            tree("Features", { APP.widgetManager.factories.getFeatures().sortedBy { it.name } })
                    ),
                    tree("Services", { APP.services.getAllServices().sortedBy { it.name } })
            ),
            tree("UI",
                    tree("Windows", FilteredList(Stage.getWindows()) { it !is Tooltip && it !is ContextMenu }),
                    tree("Layouts", { APP.widgetManager.layouts.findAllActive().sortedBy { it.name } })
            ),
            tree("Location", APP.DIR_APP),
            tree("File system", File.listRoots().map { FileTreeItem(it) }),
            tree(Name.treeOfPaths("Settings", APP.configuration.fields.map { it.group }))
    )
}

fun <T: Any> buildTreeView() = TreeView<T>().initTreeView()

fun <T: Any> TreeView<T>.initTreeView() = apply {

    addEventFilter(KeyEvent.KEY_PRESSED) {
        when (it.code) {
            KeyCode.ENTER -> {
                doAction(selectionModel.selectedItem?.value, {})
                it.consume()
            }
            else -> {
            }
        }
    }
    setOnDragDetected { e ->
        if (e.button!=MouseButton.PRIMARY) return@setOnDragDetected
        if (selectionModel.isEmpty) return@setOnDragDetected

        val items = selectionModel.selectedItems.asSequence()
                .map { it.value }
                .filterIsInstance<File>()
                .toList()
        val mode = if (e.isShiftDown) TransferMode.MOVE else TransferMode.COPY
        startDragAndDrop(mode).setContent(mapOf(DataFormat.FILES to items))
        e.consume()
    }

    // preserve selection when observable tree item children source changes
    @Suppress("UNCHECKED_CAST")
    rootProperty() syncNonNullWhile { root ->
        Subscription.multi(
                root.onEventDown(SELECTION_DISTURBED_CLEAR) {
                    val childStack = properties.computeIfAbsent(SELECTION_DISTURBED_STACK_KEY) { Stack<Unit>() } as Stack<Unit>
                    childStack.push(Unit)

                    if (childStack.size==1) {
                        properties[SELECTION_DISTURBED_KEY] = when {
                            selectionModel.isEmpty -> null
                            else -> {
                                val removed = it.removed as TreeItem<T>
                                val removedI = getRow(removed)
                                val remainingSelection = selectionModel.selectedIndices.filter { it!=removedI && !removed.isAnyParentOf(getTreeItem(it)) }
                                when {
                                    !remainingSelection.isEmpty() -> remainingSelection
                                    else -> {
                                        val i = null
                                                ?: removed.previousSibling()?.let { getRow(it) }    // try preceding
                                                ?: (removedI+1).takeIf { it<expandedItemCount }?.let { it-1 }    // try following

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
                    val childStack = properties[SELECTION_DISTURBED_STACK_KEY] as Stack<Unit>
                    childStack.pop()

                    if (properties[SELECTION_DISTURBED_KEY]!=null && childStack.isEmpty()) {
                        selectionModel.clearSelection()
                        val s = properties[SELECTION_DISTURBED_KEY] as List<Int>
                        selectionModel.selectIndices(s[0], *IntArray(s.size-1) { s[it+1] })
                        properties -= SELECTION_DISTURBED_KEY
                    }
                }
        )
    }

}

@Suppress("UNUSED_PARAMETER")
fun <T> buildTreeCell(t: TreeView<T>) = object: TreeCell<T>() {
    init {
        addEventFilter(MouseEvent.ANY) { doOnClick(it) }
    }

    override fun updateItem(o: T?, empty: Boolean) {
        super.updateItem(o, empty)
        if (!empty && o!=null) {
            graphic = computeGraphics(o)
            text = computeText(o)
        } else {
            graphic = null
            text = null
        }
    }

    private fun computeText(o: Any?): String = when {
        o==null -> "<none>"
        o is Component -> o.name
        o is Service -> o.name
        o is WidgetFactory<*> -> o.nameGui()
        o::class.java.isEnum -> enumToHuman(o.toString())
        o is File -> o.nameOrRoot
        o is Node -> o.id?.trim().orEmpty()+":"+APP.className.getOf(o)+(if (o.parent==null && o===o.scene?.root) " (root)" else "")
        o is Tooltip -> "Tooltip"
        o is PopOver<*> -> "Popup " + PopOver.active_popups.indexOf(o)
        o is PopupWindow -> "Popup (generic)"
        o is javafx.stage.Window -> {
            val w = o.asWindowOrNull()
            if (w==null) {
                "Window (generic)"
            } else {
                var n = "Window "+APP.windowManager.windows.indexOf(w)
                if (w===APP.windowManager.getMain().orNull()) n += " (main)"
                if (w===APP.windowManager.miniWindow) n += " (mini-docked)"
                n
            }
        }
        o is Name -> o.`val`
        o is Feature -> o.name
        o is HierarchicalBase<*, *> -> computeText(o.`val`)
        else -> o.toString()
    }

    private fun computeGraphics(p: Any): Node? = when (p) {
        is Path -> computeGraphics(p.toFile())
        is File -> {
            if (p hasExtension "css")
                createIcon(IconFA.CSS3, 8.0)

            val type = if (treeItem.isLeaf) FileType.FILE else FileType.of(p)

            if (type==FileType.DIRECTORY && APP.DIR_SKINS==p.parentDir || p.isValidSkinFile())
                createIcon(IconFA.PAINT_BRUSH, 8.0)
            if (type==FileType.DIRECTORY && APP.DIR_WIDGETS==p.parentDir || p.isValidWidgetFile())
                createIcon(IconFA.GE, 8.0)

            if (type==FileType.FILE) createIcon(IconFA.FILE, 8.0)
            else createIcon(IconFA.FOLDER, 8.0)
        }
        else -> null
    }

    fun doOnClick(e: MouseEvent) {
        // We can not override default double click behavior, hence this workaround.
        // TODO: this may cause issues by consuming in EventFilter instead of EventHandler, rarely (for double click)
        // https://bugs.openjdk.java.net/browse/JDK-8092146
        // https://stackoverflow.com/questions/15509203/disable-treeitems-default-expand-collapse-on-double-click-javafx-2-2
        if (e.clickCount==2) e.consume()
        if (e.eventType!=MouseEvent.MOUSE_CLICKED) return

        if (item!=null) {
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
                else -> {}
            }
        }
    }

    fun doOnSingleClick(o: Any?) {
        when (o) {
            is HierarchicalBase<*, *> -> doOnSingleClick(o.`val`)
        }
    }

    fun doOnDoubleClick(o: Any?) {
        if (treeItem.isLeaf) doAction(o) { treeItem?.expandedProperty()?.toggle() }
        else treeItem?.expandedProperty()?.toggle()
    }

    fun <T> showMenu(o: T?, t: TreeView<T>, n: Node, e: MouseEvent) {
        globalContextMenu.setValueAndItems(t.selectionModel.selectedItems.map { it.value })
        globalContextMenu.show(n, e)
    }

}

private fun doAction(o: Any?, otherwise: () -> Unit) {
    when (o) {
        is Node -> APP.widgetManager.widgets.use<ConfiguringFeature>(ANY) { it.configure(configsFromFxPropertiesOf(o)) }
        is javafx.stage.Window -> APP.widgetManager.widgets.use<ConfiguringFeature>(ANY) { it.configure(configsFromFxPropertiesOf(o)) }
        is File -> o.open()
        is Configurable<*> -> APP.widgetManager.widgets.use<ConfiguringFeature>(ANY) { it.configure(o) }
        is TreeItem<*> -> doAction(o.value, otherwise)
        is HierarchicalBase<*, *> -> doAction(o.`val`, otherwise)
        else -> otherwise()
    }
}

open class OTreeItem<T> constructor(v: T, private val childrenO: ObservableList<out T>): TreeItem<T>(v), DisposableTreeItem {
    private val once = ExecuteN(1)
    private val childrenDisposer = Disposer()

    override fun isLeaf() = childrenO.isEmpty()
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
        childrenDisposer()
        nullify(::childrenO)
    }
}

open class SimpleTreeItem<T> constructor(value: T, childrenSeq: Sequence<T> = seqOf()): TreeItem<T>(value) {
    private val childrenAll = childrenSeq.toList()
    private val once = ExecuteN(1)

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
    private val once = ExecuteN(1)

    override fun isLeaf() = isLeafLazy()
    override fun getChildren(): ObservableList<TreeItem<T>> {
        return super.getChildren().also { children ->
            once {
                children setTo childrenLazy().map { tree(it) }
            }
        }
    }
}

class NodeTreeItem(value: Node): OTreeItem<Node>(value, (value as? Parent)?.childrenUnmodifiable ?: emptyObservableList())

class WidgetItem(v: Widget): STreeItem<Any>(v, { seqOf(v.areaTemp?.root).filterNotNull() }, { false })

class LayoutItem(v: Component): STreeItem<Component>(v, { if (v is Container<*>) v.children.values.asSequence() else seqOf()})

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
            this setTo buildChildren(this@FileTreeItem)
        }
    }

    override fun isLeaf() = isLeaf

    private fun buildChildren(i: TreeItem<File>): List<TreeItem<File>> {
        val dirs = ArrayList<FileTreeItem>()
        val files = ArrayList<FileTreeItem>()
        i.value.listChildren().forEach {
            val isFile = it.isFile
            (if (isFile) files else dirs) += FileTreeItem(it, isFile)
        }
        dirs += files
        return dirs
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
        getFieldValue<ObservableList<TreeItem<T>>?>(this, "children")?.forEach { it.disposeIfDisposable() }
    }
}