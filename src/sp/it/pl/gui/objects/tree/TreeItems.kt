package sp.it.pl.gui.objects.tree

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.collections.ObservableList
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.DataFormat
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import mu.KotlinLogging
import sp.it.pl.audio.Item
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.PlaylistItemGroup
import sp.it.pl.gui.objects.contextmenu.ValueContextMenu
import sp.it.pl.gui.objects.window.stage.Window
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.WidgetSource
import sp.it.pl.layout.widget.WidgetSource.ANY
import sp.it.pl.layout.widget.WidgetSource.OPEN
import sp.it.pl.layout.widget.WidgetSource.OPEN_LAYOUT
import sp.it.pl.layout.widget.WidgetSource.OPEN_STANDALONE
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.service.Service
import sp.it.pl.util.HierarchicalBase
import sp.it.pl.util.Util.enumToHuman
import sp.it.pl.util.access.V
import sp.it.pl.util.access.toggle
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.Configurable.configsFromFxPropertiesOf
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.endsWithSuffix
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.nameOrRoot
import sp.it.pl.util.file.parentDir
import sp.it.pl.util.functional.clearSet
import sp.it.pl.util.functional.getElementType
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.seqOf
import sp.it.pl.util.graphics.createIcon
import sp.it.pl.util.system.open
import sp.it.pl.util.text.plural
import java.io.File
import java.nio.file.Path
import java.util.ArrayList
import kotlin.streams.asSequence
import kotlin.streams.toList

private typealias Settings = ConfiguringFeature<Any>
private val logger = KotlinLogging.logger { }

@Suppress("UNCHECKED_CAST")
fun <T> tree(o: T): SimpleTreeItem<T> = when (o) {
    is SimpleTreeItem<*> -> o
    is Widget<*> -> WidgetItem(o)
    is WidgetFactory<*> -> SimpleTreeItem(o)
    is Widget.Group -> STreeItem<Any>(o, { APP.widgetManager.widgets.findAll(OPEN).asSequence().filter { it.info.group()==o }.sortedBy { it.name } })
    is WidgetSource -> STreeItem<Any>(o, { APP.widgetManager.widgets.findAll(o).asSequence().sortedBy { it.name } })
    is Feature -> STreeItem<Any>(o, { APP.widgetManager.factories.getFactories().filter { it.hasFeature(o) }.sortedBy { it.nameGui() } })
    is Container<*> -> LayoutItem(o)
    is File -> FileTreeItem(o)
    is Node -> NodeTreeItem(o)
    is Window -> STreeItem(o, { seqOf(o.stage.scene.root, o.layout) })
    is Name -> STreeItem(o, { o.hChildren.asSequence() }, { o.hChildren.isEmpty() })
    is Item -> STreeItem(o.uri, { seqOf() }, { true })
    is MetadataGroup -> STreeItem<Any?>(o.getValueS("<none>"), { o.grouped.asSequence() }, { o.grouped.isEmpty() })
    is PlaylistItemGroup -> STreeItem<Any?>("Playlist Items", { o.items.asSequence() }, { o.items.isEmpty() })
    is List<*> -> STreeItem<Any?>("List of " + APP.className.get(o.getElementType()).plural(), { o.asSequence() }, { o.isEmpty() })
    is Set<*> -> STreeItem<Any?>("Set of " + APP.className.get(o.getElementType()).plural(), { o.asSequence() }, { o.isEmpty() })
    else -> if (o is HierarchicalBase<*, *>) STreeItem(o, { o.getHChildren().asSequence() }, { true }) else SimpleTreeItem(o)
}.let { it as SimpleTreeItem<T> }

fun <T> tree(v: T, vararg children: Any): SimpleTreeItem<Any> = SimpleTreeItem(v as Any).apply {
    this.children += children.asSequence().map { tree(it) }
}

fun tree(v: Any, cs: List<Any>): SimpleTreeItem<Any> = SimpleTreeItem(v, cs.asSequence())

fun tree(v: Any, cs: () -> Sequence<Any>): SimpleTreeItem<Any> = STreeItem(v, cs)

fun treeApp(): SimpleTreeItem<Any> {
    val widgetT = tree("Widgets",
            tree("Categories", Widget.Group.values().asList()),
            tree("Types", { APP.widgetManager.factories.getFactories().sortedBy { it.nameGui() } }),
            tree("Open", { seqOf(ANY, OPEN_LAYOUT, OPEN_STANDALONE) }),
            tree("Features", { APP.widgetManager.factories.getFeatures().sortedBy { it.name } })
    )
    return tree("App",
            tree("Behavior",
                    widgetT,
                    tree("Services", { APP.services.getAllServices().sortedBy { it.name } })
            ),
            tree("UI",
                    widgetT,
                    tree("Windows", { APP.windowManager.windows.asSequence() }),
                    tree("Layouts", { APP.widgetManager.layouts.findAllActive().asSequence().sortedBy { it.name } })
            ),
            tree("Location", APP.DIR_APP),
            tree("File system", File.listRoots().map { FileTreeItem(it) }),
            tree(Name.treeOfPaths("Settings", APP.configuration.getFields().stream().map { it.group }.toList()))
    )
}

fun <T: Any> buildTreeView() = TreeView<T>().apply { initTreeView(this) }

fun <T: Any> initTreeView(tree: TreeView<T>) = tree.apply {

    addEventFilter(KeyEvent.KEY_PRESSED) {
        when (it.code) {
            KeyCode.ENTER -> {
                doAction(selectionModel.selectedItem?.value, {})
                it.consume()
            }
            else -> {}
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
        o is Node -> o.id?.trim().orEmpty()+":"+APP.className.getOf(o) + (if (o.parent==null && o===o.scene.root) " (root)" else "")
        o is Window -> {
            var n = "window "+APP.windowManager.windows.indexOf(o)
            if (o===APP.windowManager.main.orNull()) n += " (main)"
            if (o===APP.windowManager.miniWindow) n += " (mini-docked)"
            n
        }
        o is Name -> o.`val`
        o is Feature -> o.name
        o is HierarchicalBase<*, *> -> computeText(o.`val`)
        else -> o.toString()
    }

    private fun computeGraphics(p: Any): Node? = when (p) {
        is Path -> computeGraphics(p.toFile())
        is File -> {
            if (p endsWithSuffix "css")
                createIcon(FontAwesomeIcon.CSS3, 8.0)

            val type = if (treeItem.isLeaf) FileType.FILE else FileType.of(p)

            if (type==FileType.DIRECTORY && APP.DIR_SKINS==p.parentDir || Util.isValidSkinFile(p))
                createIcon(FontAwesomeIcon.PAINT_BRUSH, 8.0)
            if (type==FileType.DIRECTORY && APP.DIR_WIDGETS==p.parentDir || Util.isValidWidgetFile(p))
                createIcon(FontAwesomeIcon.GE, 8.0)

            if (type==FileType.FILE) createIcon(FontAwesomeIcon.FILE, 8.0)
            else createIcon(FontAwesomeIcon.FOLDER, 8.0)
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
        doAction(o, { treeItem?.expandedProperty()?.toggle() })
    }

    fun <T> showMenu(o: T?, t: TreeView<T>, n: Node, e: MouseEvent) {
        globalContextMenu.setValueAndItems(t.selectionModel.selectedItems.map { it.value })
        globalContextMenu.show(n, e)
    }

}

private fun doAction(o: Any?, otherwise: () -> Unit) {
    when (o) {
        is Node -> APP.widgetManager.widgets.use<Settings>(ANY) { it.configure(configsFromFxPropertiesOf(o)) }
        is Window -> APP.widgetManager.widgets.use<Settings>(ANY) { it.configure(configsFromFxPropertiesOf(o.stage)) }
        is File -> o.open()
        is Configurable<*> -> APP.widgetManager.widgets.use<Settings>(ANY) { it.configure(o) }
        is Name -> APP.widgetManager.widgets.use<Settings>(ANY) { it.configure(APP.configuration.getFields().filter { it.group==o.pathUp }) }
        is TreeItem<*> -> doAction(o.value, otherwise)
        is HierarchicalBase<*, *> -> doAction(o.`val`, otherwise)
        else -> otherwise()
    }
}

private val globalContextMenu by lazy { ValueContextMenu() }

open class SimpleTreeItem<T> @JvmOverloads constructor(v: T, children: Sequence<T> = seqOf()): TreeItem<T>(v) {
    val showLeaves = V(true)

    init {
        super.getChildren() += children.asSequence().map { tree(it) }
        showLeaves.addListener { _, _, nv ->
            if (nv!!) {
                throw UnsupportedOperationException("Can not repopulate leaves yet")    // TODO: implement properly
            } else {
                super.getChildren().removeIf { it.isLeaf }
            }
            super.getChildren().asSequence()
                    .filterIsInstance<SimpleTreeItem<*>>()
                    .forEach { it.showLeaves.set(nv) }
        }
    }

    override fun isLeaf() = children.isEmpty()

}

open class STreeItem<T> @JvmOverloads constructor(v: T, private val childrenLazy: () -> Sequence<T>, private val isLeafLazy: () -> Boolean = { false }): SimpleTreeItem<T>(v) {
    private var isFirstTimeChildren = true
    private val isLeaf: Boolean? = null

    override fun getChildren(): ObservableList<TreeItem<T>> {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false
            super.getChildren().clear()
            super.getChildren() += childrenLazy().map { tree(it) }
        }
        return super.getChildren()
    }

    override fun isLeaf() = isLeafLazy()
}

class WidgetItem(v: Widget<*>): STreeItem<Any>(v, { seqOf(v.areaTemp?.root).filterNotNull() }, { false })

class LayoutItem(v: Component): STreeItem<Component>(v, { (v as? Container<*>)?.children?.values?.asSequence() ?: seqOf() })

class FileTreeItem: SimpleTreeItem<File> {
    private val isLeaf: Boolean
    private var isFirstTimeChildren = true

    @JvmOverloads constructor(value: File, isLeaf: Boolean = value.isFile): super(value) {
        this.isLeaf = isLeaf
        valueProperty().addListener { _, _, _ -> throw RuntimeException("FileTreeItem value must never change") }
    }

    override fun getChildren(): ObservableList<TreeItem<File>> = super.getChildren().apply {
        if (isFirstTimeChildren) {
            this clearSet buildChildren(this@FileTreeItem)
            isFirstTimeChildren = false
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
        if (showLeaves.get()) dirs += files
        dirs.forEach { it.showLeaves.value = showLeaves.value }
        return dirs
    }
}

class NodeTreeItem(value: Node): SimpleTreeItem<Node>(value) {

    private var isLeaf: Boolean = false
    private var isFirstTimeChildren = true
    private var isFirstTimeLeaf = true

    override fun getChildren(): ObservableList<TreeItem<Node>> {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false

            // First getChildren() call, so we actually go off and
            // determine the children of the File contained in this TreeItem.
            super.getChildren() clearSet buildChildren(this)
        }
        return super.getChildren()
    }

    override fun isLeaf(): Boolean {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false
            isLeaf = children.isEmpty()
        }
        return isLeaf
    }

    private fun buildChildren(i: TreeItem<Node>): List<TreeItem<Node>> =
            (i.value as? Parent)?.childrenUnmodifiable.orEmpty().map { NodeTreeItem(it) }

}