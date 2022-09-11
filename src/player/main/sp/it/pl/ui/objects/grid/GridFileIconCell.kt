package sp.it.pl.ui.objects.grid

import java.io.File
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import sp.it.pl.main.Double01
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.emScaled
import sp.it.pl.main.fileIcon
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.net
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.label
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.show
import sp.it.util.ui.x
import sp.it.util.units.millis

/**
 * GridCell implementation for file using [sp.it.pl.ui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileIconCell: GridCell<Item, File>() {
   protected lateinit var root: Pane
   protected lateinit var name: Label
   protected lateinit var icon: Icon
   protected var imgLoadAnimation: Anim? = null
   private var loadProgress: Double01 = 0.0

   init {
      styleClass += "icon-file-grid-cell"
   }

   protected open fun computeName(item: Item): String = when (item.valType) {
      DIRECTORY -> item.value.nameOrRoot
      FILE -> item.value.nameWithoutExtension
   }

   protected open fun computeCellTextHeight(): Double = 40.0

   protected open fun onAction(i: Item, edit: Boolean) = Unit

   override fun updateSelected(selected: Boolean) {
      super.updateSelected(selected)
      icon.select(selected)
   }

   override fun dispose() {
      failIfNotFxThread()

      imgLoadAnimation?.stop()
      imgLoadAnimation = null
   }

   override fun updateItem(item: Item?, empty: Boolean) {
      if (item===getItem()) {
         if (!empty) updateIcon(item!!)
         return
      }
      super.updateItem(item, empty)

      if (imgLoadAnimation!=null) {
         imgLoadAnimation?.stop()
         imgLoadAnimation?.applyAt(loadProgress)
      }

      if (empty) {
         graphic = null   // do not discard contents of the graphics
      } else {
         if (!::root.isInitialized) computeGraphics()  // create graphics lazily and only once
         if (graphic!==root) graphic = root           // set graphics only when necessary
      }

      if (graphic!=null) {
         name.text = if (item==null) null else computeName(item)
         updateIcon(item!!)
         pseudoClassChanged("file-hidden", FileField.IS_HIDDEN.getOf(item.value))
      }
   }

   protected open fun computeGraphics() {
      val iconAnimationParent = StackPane() // prevents opacity clash with css
      name = label {
         alignment = Pos.CENTER
      }

      icon = Icon().apply {
         isManaged = false
         isAnimated.value = false
         isFocusTraversable = false
         iconAnimationParent.lay += this
      }
      root = object: Pane(iconAnimationParent, name) {
         override fun layoutChildren() {
            val x = 0.0
            val y = 0.0
            val w = layoutBounds.width
            val h = layoutBounds.height
            val nameGap = 5.emScaled
            val th = computeCellTextHeight()

            if (gridView.value?.cellWidth?.value == GridView.CELL_SIZE_UNBOUND) {
               name.alignment = Pos.CENTER_LEFT
               name.resizeRelocate(h + nameGap, y, (w-h-2*nameGap) max 0.0, h)
               icon.relocateCenter(h/2, h/2)
            } else {
               name.alignment = Pos.CENTER
               iconAnimationParent.resizeRelocate(0.0, 0.0, w, h - th)
               name.resizeRelocate(0.0, h - th, w, th)
               name.resizeRelocate(x + nameGap, h - th + nameGap, (w-2*nameGap) max 0.0, (th-2*nameGap) max 0.0)
               icon.relocate(x, y)
            }
         }
      }.apply {
         isSnapToPixel = true
         minSize = -1.0 x -1.0
         prefSize = -1.0 x -1.0
         maxSize = -1.0 x -1.0
         onEventDown(MOUSE_CLICKED) {
            if (it.button==PRIMARY && it.clickCount==2) {
               onAction(item, it.isShiftDown)
               it.consume()
            }
         }
         onEventDown(MOUSE_CLICKED, SECONDARY) {
            contextMenuFor(item?.value).show(root, it)
         }
      }
      imgLoadAnimation = anim(200.millis) {
         loadProgress = it
         iconAnimationParent.opacity = it*it*it*it
      }
   }

   fun updateIcon(i: Item) {
      val iconSize = when (gridView.value?.cellWidth?.value) {
         GridView.CELL_SIZE_UNBOUND -> gridView.value?.cellHeight?.value?.net { it*0.5 } ?: 50.0
         else -> gridView.value?.let { it.cellHeight.value min (it.cellHeight.value - computeCellTextHeight()) } ?: 50.0
      }
      icon.scale(1.125)
      icon.isFocusTraversable = false
      icon.size(iconSize)
      icon.icon(fileIcon(i.value, i.valType))
      imgLoadAnimation?.playOpenFrom(loadProgress)
   }

}
fun Node.relocateCenter(x: Double, y: Double) {
   relocate(x + layoutBounds.width/2, y + layoutBounds.width/2)
}
