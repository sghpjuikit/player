package inspector

import javafx.css.PseudoClass.getPseudoClass
import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.TreeItem
import javafx.scene.input.ScrollEvent
import javafx.util.Callback
import sp.it.pl.gui.objects.tree.buildTreeCell
import sp.it.pl.gui.objects.tree.buildTreeView
import sp.it.pl.gui.objects.tree.tree
import sp.it.pl.gui.objects.tree.treeApp
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.io.Output
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.main.APP
import sp.it.pl.main.Widgets
import sp.it.pl.main.rowHeight
import sp.it.pl.util.file.isAnyChildOf
import sp.it.pl.util.functional.ifIs
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.expandToRootAndSelect
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.graphics.propagateESCAPE
import sp.it.pl.util.reactive.sync
import java.io.File

@Widget.Info(
        author = "Martin Polakovic",
        name = Widgets.INSPECTOR,
        description = "Inspects application as hierarchy. Displays windows, widgets,"+"file system and more. Allows editing if possible.",
        howto = ""
                +"Available actions:\n"
                +"    Right click: Open context menu\n"
                +"    Double click file: Open file in native application\n"
                +"    Double click skin file: Apply skin on application\n"
                +"    Double click widget file: Open widget\n"
                +"    Drag & drop file: Explore file\n"
                +"    Drag & drop files: Explore files' first common parent directory\n",
        version = "0.8",
        year = "2015",
        group = Widget.Group.APP
)
class Inspector(widget: Widget<*>): SimpleController(widget), FileExplorerFeature, Opener {
    private val tree = buildTreeView<Any>()
    private val outSelected: Output<Any?> = outputs.create(widget.id, "Selected", Any::class.java, null)

    init {
        this layFullArea tree.apply {
            selectionModel.selectionMode = MULTIPLE
            cellFactory = Callback { buildTreeCell(it) }
            onClose += APP.ui.font sync { fixedCellSize = it.rowHeight() }
            root = treeApp()
            selectionModel.selectedItemProperty().addListener { o, ov, nv ->
                val valueOld = ov?.value
                val valueNew = nv?.value
                if (valueOld is Node) valueOld.highlightNode(false)
                if (valueNew is Node) valueNew.highlightNode(true)
                outSelected.value = valueNew
            }
            propagateESCAPE()
        }

        onDragOver = DragUtil.fileDragAcceptHandler
        onDragDropped = EventHandler { exploreCommonFileOf(DragUtil.getFiles(it)) }
        onScroll = EventHandler<ScrollEvent> { it.consume() } // prevent scrolling event from propagating

        onClose += { outSelected.value.ifIs<Node> { it.highlightNode(false) } }
    }

    override fun exploreFile(f: File) {
        val fileRoot = tree.root.children[3]
        fun TreeItem<Any>.findChild() = children.find { it.value?.let { it is File && (f==it || f.isAnyChildOf(it)) }==true }
        generateSequence(fileRoot) { it.findChild() }.drop(1).lastOrNull()?.expandToRootAndSelect(tree)
    }

    override fun open(data: Any) {
        val item = tree(data)
        tree.root.children += item
        tree.expandToRootAndSelect(item)
    }

    override fun focus() = tree.requestFocus()

        companion object {

        private val selected = getPseudoClass("selected")

        private fun Node.highlightNode(value: Boolean) {
            pseudoClassStateChanged(selected, value)
            style = if (value) "-fx-background-color: rgba(90,200,200,0.2);" else ""
        }

    }
}