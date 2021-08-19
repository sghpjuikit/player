package favLocations

import javafx.geometry.Pos
import javafx.scene.control.SelectionMode.SINGLE
import javafx.scene.control.TreeItem
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.DRAG_DETECTED
import javafx.scene.input.TransferMode.COPY
import javafx.scene.input.TransferMode.LINK
import javafx.scene.layout.Priority.ALWAYS
import javafx.scene.layout.Priority.NEVER
import javafx.util.Callback
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.tree.tree
import sp.it.pl.layout.Widget
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.FileExplorerFeature
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.emScaled
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.tree.buildTreeCell
import sp.it.util.action.IsAction
import sp.it.util.async.IO
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runIO
import sp.it.util.conf.cList
import sp.it.util.conf.def
import sp.it.util.conf.getDelegateAction
import sp.it.util.conf.only
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.isAnyChildOf
import sp.it.util.file.toFast
import sp.it.util.functional.asIf
import sp.it.util.functional.traverse
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.ui.expandToRootAndSelect
import sp.it.util.ui.hBox
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.scrollPane
import sp.it.util.ui.treeView
import sp.it.util.ui.vBox
import sp.it.util.ui.x
import java.io.File
import sp.it.pl.main.Df.FILES
import sp.it.util.conf.butElement
import sp.it.util.functional.ifNotNull
import sp.it.util.ui.drag.set

@Widget.Info(
   author = "Martin Polakovic",
   name = "Favourite Locations",
   description = "Displays favourite or convenient file locations. Useful after redirecting selected location to another widget",
   version = "1.0.1",
   year = "2020",
   tags = []
)
class FavLocations(widget: Widget): SimpleController(widget), FileExplorerFeature {

   private val selected = io.o.create<File?>("Selected", null)
   private val locations by cList<File>().def(name = "Favourite locations", info = "Favourite locations").butElement { only(DIRECTORY) }
   private val tree = treeView<Any> {
      isShowRoot = false
      selectionModel.selectionMode = SINGLE
      selectionModel.selectedItemProperty() sync { selected.value = it?.value.asIf<File>() }
      cellFactory = Callback { buildTreeCell(it) }
   }

   init {
      root.prefSize = 200.emScaled x 500.emScaled
      root.consumeScrolling()

      root.lay += vBox {
         lay(NEVER) += hBox(0, Pos.CENTER_RIGHT) {
            id = "header"

            lay += Icon(IconFA.RECYCLE, 13.0).action(::readFromDisc.getDelegateAction(this@FavLocations))
            lay += Icon().blank()
         }
         lay(ALWAYS) += scrollPane {
            isFitToWidth = true
            isFitToHeight = true

            content = tree
         }
      }

      // drag selected files
      tree.onEventDown(DRAG_DETECTED, PRIMARY) {
         tree.selectionModel.selectedItem?.value?.asIf<File>()?.ifNotNull { f ->
            tree.startDragAndDrop(if (it.isShortcutDown) LINK else COPY)[FILES] = listOf(f)
         }
      }

      // drag & drop directories
      root.installDrag(
         IconMD.STAR,
         "Add directories to favourites",
         { it.dragboard.hasFiles() },
         { fut(it.dragboard.files).then(IO) { it.filter { it.isDirectory } } ui { locations += it } }
      )

      readFromDisc()
   }

   @IsAction(name = "Refresh", info = "Refresh file structure from disc")
   fun readFromDisc() {
      runIO {
         File.listRoots().map { it.toFast(DIRECTORY) }
      } ui { roots ->
         tree.root = tree("Root",
            tree("Favourites", locations).apply { isExpanded = true },
            tree("This PC", roots).apply { isExpanded = true }
         )
      }
   }

   override fun exploreFile(file: File) {
      tree.root.children[1].findAnyChild(file, File::isAnyChildOf)?.expandToRootAndSelect(tree)
   }

   override fun focus() = tree.requestFocus()

   companion object {
      private fun <T: Any> T.traverseChildrenToLast(next: (T) -> T?) = traverse(next).drop(1).lastOrNull()
      private inline fun <reified R> TreeItem<Any>.findChild(r: R, isAnyChildOf: R.(R) -> Boolean) = children.find { it.value?.let { it is R && (r==it || r.isAnyChildOf(it)) }==true }
      private inline fun <reified R> TreeItem<Any>.findAnyChild(r: R, crossinline isAnyChildOf: R.(R) -> Boolean): TreeItem<Any>? = traverseChildrenToLast { it.findChild(r, isAnyChildOf) }
   }
}