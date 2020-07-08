package inspector

import javafx.geometry.Pos.TOP_RIGHT
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.SelectionMode.MULTIPLE
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.scene.shape.Shape
import javafx.stage.Window
import javafx.util.Callback
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.tree.buildTreeCell
import sp.it.pl.ui.objects.tree.buildTreeView
import sp.it.pl.ui.objects.tree.disposeIfDisposable
import sp.it.pl.ui.objects.tree.tree
import sp.it.pl.ui.objects.tree.treeApp
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.FileExplorerFeature
import sp.it.pl.layout.widget.feature.Opener
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMA
import sp.it.pl.main.Widgets
import sp.it.pl.main.getAny
import sp.it.pl.main.installDrag
import sp.it.pl.main.emScaled
import sp.it.util.collections.getElementClass
import sp.it.util.file.isAnyChildOf
import sp.it.util.functional.asIf
import sp.it.util.functional.net
import sp.it.util.functional.traverse
import sp.it.util.reactive.Subscribed
import sp.it.util.reactive.Subscription
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.onItemSyncWhile
import sp.it.util.reactive.plus
import sp.it.util.reactive.propagateESCAPE
import sp.it.util.reactive.syncNonNullIntoWhile
import sp.it.util.ui.expandToRootAndSelect
import sp.it.util.ui.hBox
import sp.it.util.ui.isAnyChildOf
import sp.it.util.ui.lay
import sp.it.util.ui.pickTopMostAt
import sp.it.util.ui.prefSize
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.x
import java.io.File
import javafx.stage.Window as WindowFX

@Widget.Info(
   author = "Martin Polakovic",
   name = Widgets.INSPECTOR_NAME,
   description = "Inspects hierarchy of all application information. Includes windows, widgets, " +
      "file system and more. Allows editing if possible.",
   howto = ""
      + "Available actions:\n"
      + "    Right click: Open context menu\n"
      + "    Double click file: Open file in native application\n"
      + "    Double click skin file: Apply skin on application\n"
      + "    Double click widget file: Open widget\n"
      + "    Drag & drop file: Explore file\n"
      + "    Drag & drop files: Explore files' first common parent directory\n",
   version = "1.0.0",
   year = "2015",
   group = Widget.Group.APP
)
class Inspector(widget: Widget): SimpleController(widget), FileExplorerFeature, Opener {
   private val outputSelected = io.o.create<Any>("Selected", null)
   private val tree = buildTreeView<Any>()
   private var highlighted: Node? = null
   val selectingNode = Subscribed { feature ->
      var selected: Node? = null
      highlighted?.highlight(false)       // suppress highlighted node while selecting
      observeWindowRoots { wRoot ->
         val d1 = wRoot.onEventUp(MOUSE_MOVED) {
            selected = selected?.highlight(false)
            selected = wRoot.pickTopMostVisible(it)?.takeIf { !it.isAnyChildOf(root) }?.highlight(true)
         }
         val d2 = wRoot.onEventUp(MOUSE_CLICKED, PRIMARY) {
            feature.unsubscribe()
            selected = selected?.highlight(false)
            wRoot.pickTopMostVisible(it)?.takeIf { !it.isAnyChildOf(root) }?.let { exploreNode(it) }
            it.consume()
         }
         d1 + d2
      } + {
         selected = selected?.highlight(false)
         highlighted?.highlight(true)    // restore highlighted node after selecting
      }
   }
   val selectingWindow = Subscribed { feature ->
      observeWindowRoots { root ->
         root.onEventUp(MOUSE_CLICKED, PRIMARY) {
            feature.unsubscribe()
            exploreWindow(root.scene.window)
            it.consume()
         }
      }
   }

   init {
      root.prefSize = 600.emScaled x 600.emScaled
      root.consumeScrolling()

      root.lay += tree.apply {
         selectionModel.selectionMode = MULTIPLE
         cellFactory = Callback { buildTreeCell(it) }
         root = treeApp()
         root.isExpanded = true
         selectionModel.selectedItemProperty() attach {
            highlighted = highlighted?.highlight(false)
            highlighted = it?.value.asIf<Node>()?.highlight(true)
            outputSelected.value = it
         }
         propagateESCAPE()
      }
      root.lay(TOP_RIGHT) += hBox(10, TOP_RIGHT) {
         isPickOnBounds = false

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

      root.installDrag(
         IconFA.LIST_ALT,
         "Inspect",
         { true },
         { e -> open(e.dragboard.getAny()) }
      )

      onClose += { tree.selectionModel.clearSelection() }
      onClose += { tree.root?.disposeIfDisposable() }
      onClose += { outputSelected.value.asIf<Node>()?.highlight(false) }
      onClose += selectingNode
      onClose += selectingWindow
   }

   fun exploreWindow(window: WindowFX) {
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
         (data is Collection<*> && data.getElementClass()==File::class.java) -> {
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
         fun Node.styleHighlighted(value: Boolean) = styleclassToggle("inspector-highlighted", value)

         return if (value) {
            this?.traverseToFirst { it.isHighlightable() }?.also { it.styleHighlighted(value) }
         } else {
            this?.styleHighlighted(value)
            null
         }
      }

      private fun observeWindowRoots(subscriber: (Parent) -> Subscription) = WindowFX.getWindows().onItemSyncWhile {
         it.sceneProperty().syncNonNullIntoWhile(Scene::rootProperty, subscriber)
      }

   }
}