package inspector

import javafx.event.EventHandler
import javafx.geometry.Pos.CENTER
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.shape.Shape
import javafx.stage.Stage
import javafx.stage.Window
import javafx.util.Callback
import org.reactfx.Subscription
import sp.it.pl.gui.objects.icon.Icon
import sp.it.pl.gui.objects.tree.buildTreeCell
import sp.it.pl.gui.objects.tree.buildTreeView
import sp.it.pl.gui.objects.tree.disposeIfDisposable
import sp.it.pl.gui.objects.tree.tree
import sp.it.pl.gui.objects.tree.treeApp
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.Widgets
import sp.it.pl.util.file.isAnyChildOf
import sp.it.pl.util.functional.asIf
import sp.it.pl.util.functional.getElementType
import sp.it.pl.util.functional.net
import sp.it.pl.util.functional.traverse
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.graphics.expandToRootAndSelect
import sp.it.pl.util.graphics.hBox
import sp.it.pl.util.graphics.isAnyChildOf
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.graphics.pickTopMostAt
import sp.it.pl.util.graphics.propagateESCAPE
import sp.it.pl.util.reactive.Subscribed
import sp.it.pl.util.reactive.attach
import sp.it.pl.util.reactive.onEventUp
import sp.it.pl.util.reactive.onItemSync
import sp.it.pl.util.reactive.plus
import sp.it.pl.util.reactive.syncIntoWhile
import java.io.File

@Widget.Info(
        author = "Martin Polakovic",
        name = Widgets.INSPECTOR,
        description = "Inspects hierarchy of all application information. Includes windows, widgets, "+
                "file system and more. Allows editing if possible.",
        howto = ""
                +"Available actions:\n"
                +"    Right click: Open context menu\n"
                +"    Double click file: Open file in native application\n"
                +"    Double click skin file: Apply skin on application\n"
                +"    Double click widget file: Open widget\n"
                +"    Drag & drop file: Explore file\n"
                +"    Drag & drop files: Explore files' first common parent directory\n",
        version = "1.0",
        year = "2015",
        group = Widget.Group.APP
)
class Inspector(widget: Widget<*>): SimpleController(widget), FileExplorerFeature, Opener {
    private val tree = buildTreeView<Any>()
    private val outSelected: Output<Any?> = outputs.create(widget.id, "Selected", Any::class.java, null)
    private var highlighted: Node? = null
    val selectingNode = Subscribed { feature ->
        var selected: Node? = null
        observeWindowRoots { root ->
            val d1 = root.onEventUp(MOUSE_MOVED) {
                selected = selected?.highlight(false)
                selected = root.pickTopMostVisible(it)?.takeIf { !it.isAnyChildOf(this@Inspector) }?.highlight(true)
            }
            val d2 = root.onEventUp(MOUSE_CLICKED) {
                feature.unsubscribe()
                selected = selected?.highlight(false)
                root.pickTopMostVisible(it)?.takeIf { !it.isAnyChildOf(this@Inspector) }?.let { exploreNode(it) }
                it.consume()
            }
            d1+d2
        } + {
            selected = selected?.highlight(false)
        }
    }
    val selectingWindow = Subscribed { feature ->
        observeWindowRoots { root ->
            root.onEventUp(MOUSE_CLICKED) {
                feature.unsubscribe()
                exploreWindow(root.scene.window)
                it.consume()
            }
        }
    }

    init {
        layFullArea += tree.apply {
            selectionModel.selectionMode = MULTIPLE
            cellFactory = Callback { buildTreeCell(it) }
            root = treeApp()
            root.isExpanded = true
            selectionModel.selectedItemProperty() attach {
                highlighted = highlighted?.highlight(false)
                highlighted = it?.value.asIf<Node>()?.highlight(true)
                outSelected.value = it
            }
            propagateESCAPE()
        }
        lay(0, 0, null, null) += hBox(10, CENTER) {
            lay += Icon(IconMA.COLLECTIONS).apply {
                tooltip("Inspect window")
                onClickDo { !selectingWindow }
            }
            lay += Icon(IconMA.COLLECTIONS_BOOKMARK).apply {
                tooltip("Inspect element")
                onClickDo { !selectingNode }
            }
            lay += Icon().blank()
        }

        installDrag(
                this,
                IconFA.LIST_ALT,
                { "Inspect" },
                { true },
                { open(DragUtil.getAny(it)) }
        )
        onScroll = EventHandler { it.consume() } // prevent scrolling event from propagating

        onClose += { tree.selectionModel.clearSelection() }
        onClose += { tree.root?.disposeIfDisposable() }
        onClose += { outSelected.value.asIf<Node>()?.highlight(false) }
        onClose += selectingNode
        onClose += selectingWindow
    }

    fun exploreWindow(window: javafx.stage.Window) {
        val windowRoot = tree.root.children[1].children[0]
        windowRoot.children.find { it.value===window }?.expandToRootAndSelect(tree)
    }

    fun exploreNode(node: Node) {
        val nodeRoot = tree.root.children[1].children[0].children
                .find { it.value===node.scene?.window }
                ?.net { it.children[0].children[0] }
        nodeRoot?.findAnyChild(node, Node::isAnyChildOf)?.expandToRootAndSelect(tree)
    }

    override fun exploreFile(file: File) {
        val fileRoot = tree.root.children[3]
        fileRoot.findAnyChild(file, File::isAnyChildOf)?.expandToRootAndSelect(tree)
    }

    @Suppress("UNCHECKED_CAST")
    override fun open(data: Any?) {
        when {
            data is Window -> exploreWindow(data)
            data is File -> exploreFile(data)
            data is Node -> exploreNode(data)
            (data is Collection<*> && data.getElementType()==File::class.java) -> {
                exploreCommonFileOf(data as Collection<File>)
            }
            else -> {
                val item = tree(data ?: "<none>")
                tree.root.children += item
                tree.expandToRootAndSelect(item)
            }
        }
    }

    override fun focus() = tree.requestFocus()

    companion object {

        private fun Node.traverseToFirst(test: (Node) -> Boolean) = traverse { it.parent }.find(test)

        private fun <T: Any> T.traverseChildrenToLast(next: (T) -> T?) = traverse(next).drop(1).lastOrNull()

        private inline fun <reified R> TreeItem<Any>.findChild(r: R, isAnyChildOf: R.(R) -> Boolean) = children.find { it.value?.let { it is R && (r==it || r.isAnyChildOf(it)) }==true }

        private inline fun <reified R> TreeItem<Any>.findAnyChild(r: R, crossinline isAnyChildOf: R.(R) -> Boolean): TreeItem<Any>? = traverseChildrenToLast { it.findChild(r, isAnyChildOf) }

        private fun Node.pickTopMostVisible(event: MouseEvent) = pickTopMostAt(event.sceneX, event.sceneY) { it.isVisible }

        private fun Node?.highlight(value: Boolean): Node? {
            fun Node.isHighlightable() = this !is Shape
            fun Node.styleHighlighted(value: Boolean) {
                if (value) styleClass += "inspector-highlighted"
                else styleClass -= "inspector-highlighted"
            }

            return if (value) {
                this?.traverseToFirst { it.isHighlightable() }?.also { it.styleHighlighted(value) }
            } else {
                this?.styleHighlighted(value)
                null
            }
        }

        private fun observeWindowRoots(subscriber: (Parent) -> Subscription) = Stage.getWindows().onItemSync {
            it.sceneProperty().syncIntoWhile(Scene::rootProperty) {
                it?.net(subscriber) ?: Subscription.EMPTY
            }
        }
    }
}