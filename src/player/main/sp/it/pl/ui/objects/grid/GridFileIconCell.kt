package sp.it.pl.ui.objects.grid

import java.io.File
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.ContentDisplay.GRAPHIC_ONLY
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import sp.it.pl.main.contextMenuFor
import sp.it.pl.main.fileIcon
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.icon.Icon
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.net
import sp.it.util.math.max
import sp.it.util.math.min
import sp.it.util.reactive.onEventDown
import sp.it.util.ui.label
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.show
import sp.it.util.ui.width
import sp.it.util.ui.x
import sp.it.util.units.millis

/**
 * GridCell implementation for file using [sp.it.pl.ui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileIconCell: GridCell<Item, File>() {
   protected lateinit var name: Label
   protected lateinit var icon: Icon
   protected var disposed = false
   protected var imgLoadAnimation: Anim? = null
   protected var imgLoadAnimationItem: Item? = null

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
      disposed = true
      imgLoadAnimationItem = null
      imgLoadAnimation?.stop()
      imgLoadAnimation = null
   }

   override fun updateItem(item: Item?, empty: Boolean) {
      if (disposed) return

      if (item===getItem()) {
         if (!empty) updateIcon(item!!)
         return
      }

      super.updateItem(item, empty)

      imgLoadAnimationItem = null
      imgLoadAnimation?.stop()
      if (!empty) {
         if (!::icon.isInitialized) computeGraphics()  // create graphics lazily and only once
         name.text = if (item==null) null else computeName(item)
         if (item!=null) updateIcon(item)
         if (item!=null) updateHidden(item)
         imgLoadAnimationItem = item
         imgLoadAnimation?.playOpenFrom(imgLoadAnimationItem!!.loadProgress)
      }
   }

   protected open fun computeGraphics() {
      contentDisplay = GRAPHIC_ONLY
      name = label {
         alignment = Pos.CENTER
      }

      icon = Icon().apply {
         isManaged = false
         isAnimated.value = false
         isFocusTraversable = false
      }
      children += listOf(icon, name)
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
         contextMenuFor(item?.value).show(this, it)
      }
      imgLoadAnimation = anim(200.millis) {
         if (imgLoadAnimationItem!=null) {
            imgLoadAnimationItem!!.loadProgress = it
            icon.opacity = it*it*it*it
         }
      }
   }

   override fun layoutChildren() {
      val x = 0.0; val y = 0.0; val w = width; val h = height; val th = computeCellTextHeight() max 0.0; val lp = labelPadding

      if (gridView.value?.cellWidth?.value == GridView.CELL_SIZE_UNBOUND) {
         name.alignment = Pos.CENTER_LEFT
         name.resizeRelocate(h + lp.left, y, (w - h - lp.width) max 0.0, h)
         icon.relocateCenter(h/2, h/2)
      } else {
         name.alignment = Pos.CENTER
         icon.resizeRelocate(x, y, w, h - th)
         name.resizeRelocate(x + lp.left, h - th, (w - lp.width) max 0.0, th)
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
      icon.opacity = i.loadProgress.net { it*it*it*it }
   }

   /** @return true if the item of this cell is not the same object as the item specified */
   protected fun isInvalidItem(item: Item): Boolean = this.item!==item

   /** @return true if the index of this cell is not the same as the index specified */
   protected fun isInvalidIndex(index: Int): Boolean = this.index!=index

   /** @return true if this cell is detached from the grid (i.e. not its child) */
   protected fun isInvalidVisibility(): Boolean = parent==null

   private fun isInvalid(item: Item, i: Int): Boolean = disposed || isInvalidItem(item) || isInvalidIndex(i) || isInvalidVisibility()

   /**
    * Begins loading isHidden attribute for the item. If item changes meanwhile, the result is stored
    * (it will not need to load again) to the old item, but not showed.
    *
    * Thumbnail quality may be decreased to achieve good performance, while loading high
    * quality thumbnail in the bgr. Each phase uses its own executor.
    *
    * Must be called on FX thread.
    */
   private fun updateHidden(item: Item, i: Int = index) {
      val isHidden = item.computeIsHidden()
      when {
         isHidden.isDone() ->
            pseudoClassChanged("file-hidden", isHidden.getDone().or { false })
         else -> {
            pseudoClassChanged("file-hidden", false)
            isHidden ui { if (!isInvalid(item, i)) pseudoClassChanged("file-hidden", it) }
         }
      }
   }

   companion object {
      private fun Node.relocateCenter(x: Double, y: Double) {
         relocate(x - layoutBounds.width/2, y - layoutBounds.height/2)
      }
   }
}

