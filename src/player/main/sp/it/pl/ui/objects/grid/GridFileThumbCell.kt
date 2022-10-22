package sp.it.pl.ui.objects.grid

import java.io.File
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.StackPane
import javafx.scene.shape.Rectangle
import kotlin.math.sqrt
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.grid.GridView.Companion.CELL_SIZE_UNBOUND
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.util.JavaLegacy
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.file.FileType
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfImageLoaded
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.label
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.millis

/**
 * GridCell implementation for file using [sp.it.pl.ui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileThumbCell: GridCell<Item, File>() {
   protected lateinit var root: StackPane
   protected lateinit var name: Label
   protected lateinit var stroke: Rectangle
   protected var thumb: Thumbnail? = null
   protected var imgLoadAnim: Anim? = null
   protected var isJustVisible = false
   private var imgLoadAnimItem: Item? = null
   private val hoverAnim = lazy {
      anim(150.millis) {
         val p = sqrt(it)
         val s = 2.emScaled
         val x = -s+(1-p)*(s + computeCellTextHeight())
         stroke.strokeWidth = (p*s) max 1.0
         name.style = "-fx-background-insets: $x 0 0 0;"
      }
   }
   protected var onLayoutChildren: (Double, Double, Double, Double) -> Unit = { _, _, _, _ -> }
   protected var disposed = false
   protected val onDispose = Disposer()

   init {
      styleClass += "thumb-file-grid-cell"
   }

   protected open fun computeName(item: Item): String = when (item.valType) {
      FileType.DIRECTORY -> item.value.nameOrRoot
      FileType.FILE -> item.value.nameWithoutExtension
   }

   protected open fun computeCellTextHeight(): Double = 2 * (font?.size ?: 1.em)

   protected open fun computeTask(r: () -> Unit): () -> Unit = r

   protected open fun onAction(i: Item, edit: Boolean) = Unit

   override fun dispose() {
      disposed = true
      imgLoadAnim?.stop()
      imgLoadAnim = null
      imgLoadAnimItem = null
      hoverAnim.orNull()?.stop()
      onDispose()
      item?.loadingThread.ifNotNull { it.interrupt() }
      if (thumb!=null) {
         val img = thumb?.view?.image
         thumb?.view?.image = null
         if (img!=null) JavaLegacy.destroyImage(img)
      }
      thumb = null
   }

   override fun updateIndex(i: Int) {
      isJustVisible = index==-1 && i!=-1
      if (i==-1) item?.loadingThread.ifNotNull { it.interrupt() }
      super.updateIndex(i)
   }

   override fun updateItem(item: Item?, empty: Boolean) {
      if (disposed) return
      if (item===getItem()) {
         super.updateItem(item, empty)
         if (isJustVisible) {
            if (item!=null) setCoverNow(item)
         }
         return
      }

      super.updateItem(item, empty)

      if (empty) {
         // do not discard contents of the graphics
      } else {
         if (!::root.isInitialized) computeGraphics()  // create graphics lazily and only once
         if (graphic!==root) graphic = root           // set graphics only when necessary

         name.text = if (item==null) null else computeName(item)
         if (item!=null) {
            setCoverNow(item)
            pseudoClassChanged("file-hidden", FileField.IS_HIDDEN.getOf(item.value))
         }
      }
   }

   override fun updateSelected(selected: Boolean) {
      if (disposed) return
      if (this.isSelected==selected) return

      super.updateSelected(selected)
      hoverAnim.value.playFromDir(selected || root.isHover)
      if (thumb!=null && thumb!!.image.value!=null) thumb!!.animationPlayPause(selected)
   }

   protected open fun computeGraphics() {
      name = label {
         alignment = Pos.CENTER
         isManaged = false
         isWrapText = true
      }
      thumb = object: Thumbnail() {
         init {
            borderVisible = false
            pane.isManaged = false
            pane.isSnapToPixel = true
            view.isSmooth = false
         }
         override fun getRepresentant() = item?.value
      }
      imgLoadAnim = anim(200.millis) {
         if (imgLoadAnimItem!=null) {
            imgLoadAnimItem?.loadProgress = it
            thumb?.view?.opacity = it*it*it*it
         }
      }
      stroke = Rectangle(1.0, 1.0).apply {
         id = "grid-cell-stroke"
         styleClass += "grid-cell-stroke"
         isManaged = false
         isMouseTransparent = true
      }
      root = object: StackPane(thumb!!.pane, name, stroke) {
         override fun layoutChildren() {
            val x = 0.0 ; val y = 0.0 ; val w = width ; val h = height ; val th = computeCellTextHeight() max 0.0

            if (gridView.value?.cellWidth?.value == CELL_SIZE_UNBOUND) {
               name.alignment = Pos.CENTER_LEFT
               name.resizeRelocate(h, y, (w-h) max 0.0, h)
               thumb!!.pane.resizeRelocate(x, y, h, h)
               stroke.x = x; stroke.y = y; stroke.width = h; stroke.height = h
            } else {
               name.alignment = Pos.CENTER
               name.resizeRelocate(x, h - th, w, th)
               thumb!!.pane.resizeRelocate(x, y, w, h - th)
               stroke.x = x; stroke.y = y; stroke.width = w; stroke.height = h
            }
            onLayoutChildren(x, y, w, h)
         }
      }.apply {
         isSnapToPixel = true
         minSize = -1.0 x -1.0
         prefSize = -1.0 x -1.0
         maxSize = -1.0 x -1.0
         isCache = true
         isCacheShape = true
         onEventDown(MOUSE_CLICKED) {
            if (it.button==PRIMARY && it.clickCount==2) {
               onAction(item, it.isShiftDown)
               it.consume()
            }
         }
         hoverProperty() sync { h ->
            hoverAnim.value.playFromDir(h || isSelected)
         }
      }
   }

   /** @return size of an image to be loaded for the thumbnail */
   protected fun computeThumbSize(): ImageSize = gridView.value.let {
      when {
         it == null -> ImageSize(100.0, 100.0)
         it.cellWidth.value == CELL_SIZE_UNBOUND -> ImageSize(it.cellHeight.value, it.cellHeight.value)
         else -> ImageSize(it.cellWidth.value , it.cellHeight.value - computeCellTextHeight())
      }
   }

   /** @return true if the item of this cell is not the same object as the item specified */
   protected fun isInvalidItem(item: Item): Boolean = this.item!==item

   /** @return true if the index of this cell is not the same as the index specified */
   protected fun isInvalidIndex(index: Int): Boolean = this.index!=index

   /** @return true if this cell is detached from the grid (i.e. not its child) */
   protected fun isInvalidVisibility(): Boolean = parent==null

   private fun isInvalid(item: Item, i: Int): Boolean = isInvalidItem(item) || isInvalidIndex(i) || isInvalidVisibility()

   /**
    * Begins loading cover for the item. If item changes meanwhile, the result is stored
    * (it will not need to load again) to the old item, but not showed.
    *
    * Thumbnail quality may be decreased to achieve good performance, while loading high
    * quality thumbnail in the bgr. Each phase uses its own executor.
    *
    * Must be called on FX thread.
    */
   private fun setCoverNow(item: Item) {
      when (val cover = item.cover) {
         is ImageLoad.NotStarted, is ImageLoad.DoneInterrupted -> {
            thumb!!.loadImage(null)
            val i = index
            item.computeCover(computeThumbSize()) ui { setCoverPost(item, i, it) }
         }
         is ImageLoad.Loading -> {
            thumb!!.loadImage(null)
            val i = index
            cover.loading ui { setCoverPost(item, i, it) }
         }
         is ImageLoad.DoneErr -> {}
         is ImageLoad.DoneOk -> setCoverPost(item, index, cover)
      }
   }

   private fun setCoverPost(item: Item, index: Int, img: ImageLoad) {
      if (!disposed && !isInvalid(item, index)) {
         if (thumb!!.getImage()!==img.image)
         imgLoadAnim?.stop()
         imgLoadAnimItem = item
         imgLoadAnim?.playOpenFrom(item.loadProgress)
         img.image.sync1IfImageLoaded {
            thumb!!.loadImage(img.image, img.file)
         }
      }
   }

}