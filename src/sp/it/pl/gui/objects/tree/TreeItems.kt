package sp.it.pl.gui.objects.tree

import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.TreeCell
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.DataFormat
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.TransferMode
import mu.KotlinLogging
import sp.it.pl.gui.objects.contextmenu.ImprovedContextMenu
import sp.it.pl.gui.objects.window.stage.Window
import sp.it.pl.layout.Component
import sp.it.pl.layout.container.Container
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.WidgetFactory
import sp.it.pl.layout.widget.WidgetManager.WidgetSource
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.ANY
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.LAYOUT
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.OPEN
import sp.it.pl.layout.widget.WidgetManager.WidgetSource.STANDALONE
import sp.it.pl.layout.widget.feature.ConfiguringFeature
import sp.it.pl.layout.widget.feature.Feature
import sp.it.pl.main.AppUtil.APP
import sp.it.pl.main.browseMultipleFiles
import sp.it.pl.main.use
import sp.it.pl.service.Service
import sp.it.pl.util.HierarchicalBase
import sp.it.pl.util.Util.enumToHuman
import sp.it.pl.util.access.V
import sp.it.pl.util.conf.Configurable
import sp.it.pl.util.conf.Configurable.configsFromFxPropertiesOf
import sp.it.pl.util.file.FileType
import sp.it.pl.util.file.Util
import sp.it.pl.util.file.listChildren
import sp.it.pl.util.file.nameOrRoot
import sp.it.pl.util.functional.Util.stream
import sp.it.pl.util.functional.orNull
import sp.it.pl.util.functional.sortedBy
import sp.it.pl.util.graphics.Util.menuItem
import sp.it.pl.util.graphics.createIcon
import sp.it.pl.util.system.copyToSysClipboard
import sp.it.pl.util.system.edit
import sp.it.pl.util.system.open
import sp.it.pl.util.system.openIn
import sp.it.pl.util.type.ClassName
import sp.it.pl.util.type.Util.getAllMethods
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.nio.file.Path
import java.util.*
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlin.streams.toList

private typealias Settings = ConfiguringFeature
private val logger = KotlinLogging.logger { }

fun <T> tree(o: T): SimpleTreeItem<T> = when (o) {
    is SimpleTreeItem<*> -> o
    is Widget<*> -> WidgetItem(o as Widget<*>)
    is WidgetFactory<*> -> SimpleTreeItem(o)
    is Widget.Group -> STreeItem(o, { APP.widgetManager.findAll(OPEN).filter { it.info.group()==o }.sortedBy { it.name } })
    is WidgetSource -> STreeItem(o, { APP.widgetManager.findAll(o as WidgetSource).sortedBy { it.name } })
    is Feature -> STreeItem(o.name, { APP.widgetManager.getFactories().filter { it.hasFeature(o) }.sortedBy { it.nameGui() } })
    is Container<*> -> LayoutItem(o as Component)
    is File -> FileTreeItem(o as File)
    is Node -> NodeTreeItem(o as Node)
    is Window -> STreeItem(o, { stream((o as Window).stage.scene.root, (o as Window).layout) })
    is Name -> STreeItem(o, { (o as HierarchicalBase<*, *>).getHChildren().stream() }, { (o as Name).hChildren.isEmpty() })
    else -> if (o is HierarchicalBase<*, *>) STreeItem(o, { (o as HierarchicalBase<*, *>).getHChildren().stream() }, { true }) else SimpleTreeItem(o)
}.let { it as SimpleTreeItem<T> }

fun <T> tree(v: T, vararg children: Any): SimpleTreeItem<Any> = SimpleTreeItem(v as Any).apply {
    this.children += children.asSequence().map { tree(it) }
}

fun tree(v: Any, cs: List<Any>): SimpleTreeItem<Any> = SimpleTreeItem(v, cs.stream())

fun tree(v: Any, cs: Stream<out Any>): SimpleTreeItem<Any> = STreeItem(v, { cs })

fun tree(v: Any, cs: () -> Stream<out Any>): SimpleTreeItem<Any> = STreeItem(v, cs)

fun treeApp(): SimpleTreeItem<Any> {
    val widgetT = tree("Widgets",
            tree("Categories", Widget.Group.values().asList()),
            tree("Types", { APP.widgetManager.getFactories().sortedBy { it.nameGui() } }),
            tree("Open", listOf(ANY, LAYOUT, STANDALONE)),
            tree("Features", { APP.widgetManager.features.sortedBy { it.name } })
    )
    return tree("App",
            tree("Behavior",
                    widgetT,
                    tree("Services", { APP.services.getAllServices().sortedBy { ClassName.of(it.javaClass) } })
            ),
            tree("UI",
                    widgetT,
                    tree("Windows", { APP.windowManager.windows.stream() }),
                    tree("Layouts", { APP.widgetManager.layouts.sortedBy { it.name } })
            ),
            tree("Location", APP.DIR_APP.listChildren()),
            tree("File system", File.listRoots().map { FileTreeItem(it) }),
            tree(Name.treeOfPaths("Settings", APP.configuration.fields.stream().map { it.group }.toList()))
    )
}

fun <T: Any> buildTreeView() = TreeView<T>().apply { initTreeView(this) }

fun <T: Any> initTreeView(tree: TreeView<T>) = tree.apply {
    onDragDetected = EventHandler{  e -> }
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

fun <T> buildTreeCell(t: TreeView<T>) = object: TreeCell<T>() {
    init {
        setOnMouseClicked { doOnClick(it) }
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
        o is Service -> o.javaClass.simpleName    // TODO: implement Service.name
        o is WidgetFactory<*> -> o.nameGui()
        o::class.java.isEnum -> enumToHuman(o.toString())
        o is File -> o.nameOrRoot
        o is Node -> o.id.trim()+":"+APP.className.get(o::class.java)
        o is Window -> {
            var n = "window "+APP.windowManager.windows.indexOf(o)
            if (o===APP.windowManager.main.orNull()) n += " (main)"
            if (o===APP.windowManager.miniWindow) n += " (mini-docked)"
            n
        }
        o is Name -> o.`val`
        o is HierarchicalBase<*, *> -> computeText(o.`val`)
        else -> o.toString()
    }

    private fun computeGraphics(p: Any): Node? = when (p) {
        is Path -> computeGraphics(p.toFile())
        is File -> {
            if (p.path.endsWith(".css"))
                createIcon(FontAwesomeIcon.CSS3, 11)

            val type = if (treeItem.isLeaf) FileType.FILE else FileType.of(p)

            if (type==FileType.DIRECTORY && APP.DIR_SKINS==p.parentFile || Util.isValidSkinFile(p))
                createIcon(FontAwesomeIcon.PAINT_BRUSH, 11)
            if (type==FileType.DIRECTORY && APP.DIR_WIDGETS==p.parentFile || Util.isValidWidgetFile(p))
                createIcon(FontAwesomeIcon.GE, 11)

            if (type==FileType.FILE) createIcon(FontAwesomeIcon.FILE, 11)
            else createIcon(FontAwesomeIcon.FOLDER, 11)
        }
        else -> null
    }

    fun doOnClick(e: MouseEvent) {
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
        when (o) {
            is Node -> APP.widgetManager.use<Settings>(ANY) { it.configure(configsFromFxPropertiesOf(o)) }
            is Window -> APP.widgetManager.use<Settings>(ANY) { it.configure(configsFromFxPropertiesOf(o.stage)) }
            is File -> o.openIn()
            is Configurable<*> -> APP.widgetManager.use<Settings>(ANY) { it.configure(o) }
            is Name -> APP.widgetManager.use<Settings>(ANY) { it.configure(APP.configuration.fields.filter { it.group==o.pathUp }) }
            is HierarchicalBase<*, *> -> doOnDoubleClick(o.`val`)
        }
    }

    // TODO: implement properly
    fun <T> showMenu(o: T?, t: TreeView<T>, n: Node, e: MouseEvent) {
        when (o) {
            is File -> {
                val files = t.selectionModel.selectedItems.filterIsInstance<File>()
                when (files.size) {
                    0 -> m.items.forEach { it.isDisable = true }
                    1 -> {
                        m.items.forEach { it.isDisable = false }
                        m.items[2].isDisable = !files[0].isFile
                    }
                    else -> {
                        m.items.forEach { it.isDisable = false }
                        m.items[0].isDisable = true
                        m.items[2].isDisable = true
                    }
                }
                m.value = files
                m.show(n, e)
            }
            is Window -> {
                windowMenu.value = o
                windowMenu.show(n, e)
            }
        //	    if (o instanceof HierarchicalBase) showMenu(((HierarchicalBase)o).val, t, n, e); // requires mapping Hierarchical -> T
        }
    }
}

private val m = ImprovedContextMenu<List<File>>().apply {
    items += listOf(
            menuItem("Open") { value[0].open() },
            menuItem("Open in-app") { openIn(value) },
            menuItem("Edit") { value[0].edit() },
            menuItem("Copy") { copyToSysClipboard(DataFormat.FILES, value) },
            menuItem("Explore in browser") { browseMultipleFiles(value.stream()) }
    )
}

private val windowMenu = ImprovedContextMenu<Window>().apply {
    items += getAllMethods(Window::class.java).asSequence()
            .filter { Modifier.isPublic(it.modifiers) }
            .filter { it.parameterCount==0 }
            .filter { it.returnType==Void.TYPE }
            .map {
                menuItem(it.name) { _ ->
                    try {
                        it.invoke(value)
                    } catch (e: IllegalAccessException) {
                        logger.error(e) { "Could not invoke method $it on object $value" }
                    } catch (e: InvocationTargetException) {
                        logger.error(e) { "Could not invoke method $it on object $value" }
                    }
                }
            }
}

open class SimpleTreeItem<T> @JvmOverloads constructor(v: T, children: Stream<out T> = stream()): TreeItem<T>(v) {
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

open class STreeItem<T> @JvmOverloads constructor(v: T, private val childrenLazy: () -> Stream<out T>, private val isLeafLazy: () -> Boolean = { false }): SimpleTreeItem<T>(v) {
    private var isFirstTimeChildren = true
    private val isLeaf: Boolean? = null

    override fun getChildren(): ObservableList<TreeItem<T>> {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false
            super.getChildren().clear()
            super.getChildren() += childrenLazy().asSequence().map { tree(it) }
        }
        return super.getChildren()
    }

    override fun isLeaf() = isLeafLazy()
}

class WidgetItem(v: Widget<*>): STreeItem<Any>(v, { stream<Any>(v.areaTemp.root) }, { true })

class LayoutItem(v: Component): STreeItem<Component>(v, { (v as? Container<*>)?.children?.values?.stream() ?: stream() })

class FileTreeItem: SimpleTreeItem<File> {
    private val isLeaf: Boolean
    private var isFirstTimeChildren = true

    @JvmOverloads constructor(value: File, isLeaf: Boolean = value.isFile): super(value) {
        this.isLeaf = isLeaf
        valueProperty().addListener { _, _, _ -> throw RuntimeException("FileTreeItem value must never change") }
    }

    override fun getChildren(): ObservableList<TreeItem<File>> = super.getChildren().apply {
        if (isFirstTimeChildren) {
            setAll(buildChildren(this@FileTreeItem))
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
            super.getChildren().setAll(buildChildren(this))
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