package sp.it.pl.ui.objects.grid

import java.io.File
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.StackPane
import javafx.scene.shape.Rectangle
import sp.it.pl.main.emScaled
import sp.it.pl.ui.objects.grid.GridView.Companion.CELL_SIZE_UNBOUND
import sp.it.pl.ui.objects.hierarchy.Item
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.util.JavaLegacy
import sp.it.util.access.fieldvalue.FileField
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.FX
import sp.it.util.async.burstTPExecutor
import sp.it.util.async.sleep
import sp.it.util.async.threadFactory
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileType
import sp.it.util.file.nameOrRoot
import sp.it.util.functional.orNull
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.doIfImageLoaded
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfImageLoaded
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.label
import sp.it.util.ui.lookupId
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.x
import sp.it.util.units.em
import sp.it.util.units.millis
import sp.it.util.units.minutes

/**
 * GridCell implementation for file using [sp.it.pl.ui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileThumbCell: GridCell<Item, File>() {
   protected lateinit var root: StackPane
   protected lateinit var name: Label
   protected var thumb: Thumbnail? = null
   protected var imgLoadAnim: Anim? = null
   private var imgLoadAnimItem: Item? = null
   private val hoverAnim = lazy {
      anim(150.millis) { root.lookupId<Rectangle>("grid-cell-stroke").strokeWidth = 1+it*2.emScaled }
   }
   @Volatile protected var disposed = false
   private val onDispose = Disposer()
   @Volatile private var itemVolatile: Item? = null
   @Volatile private var parentVolatile: Parent? = null
   @Volatile private var indexVolatile: Int = -1

   init {
      styleClass += "thumb-file-grid-cell"
      parentProperty() sync { parentVolatile = it?.parent } on onDispose
   }

   protected open fun computeName(item: Item): String = when (item.valType) {
      FileType.DIRECTORY -> item.value.nameOrRoot
      FileType.FILE -> item.value.nameWithoutExtension
   }

   protected open fun computeCellTextHeight(): Double = 2 * (font?.size ?: 1.em)

   protected open fun computeTask(r: () -> Unit): () -> Unit = r

   protected open fun onAction(i: Item, edit: Boolean) = Unit

   override fun dispose() {
      failIfNotFxThread()

      disposed = true
      imgLoadAnim?.stop()
      imgLoadAnim = null
      imgLoadAnimItem = null
      hoverAnim.orNull()?.stop()
      onDispose()
      if (thumb!=null) {
         val img = thumb?.view?.image
         thumb?.view?.image = null
         if (img!=null) JavaLegacy.destroyImage(img)
      }
      thumb = null
      itemVolatile = null
      parentVolatile = null
      indexVolatile = -1
   }

   override fun updateItem(item: Item?, empty: Boolean) {
      if (disposed) return
      if (item===getItem()) {
         if (!empty) setCoverNow(item!!)
         return
      }
      super.updateItem(item, empty)
      itemVolatile = item

      if (imgLoadAnim!=null) {
         imgLoadAnim?.stop()
         imgLoadAnimItem = item
         imgLoadAnim?.applyAt(item?.loadProgress ?: 0.0)
      }

      if (empty) {
         graphic = null   // do not discard contents of the graphics
      } else {
         if (!::root.isInitialized) computeGraphics()  // create graphics lazily and only once
         if (graphic!==root) graphic = root           // set graphics only when necessary
      }

      if (graphic!=null) {
         name.text = if (item==null) null else computeName(item)
         setCoverNow(item!!)
         pseudoClassChanged("file-hidden", FileField.IS_HIDDEN.getOf(item.value))
      }
   }

   override fun updateSelected(selected: Boolean) {
      super.updateSelected(selected)
      hoverAnim.value.playFromDir(selected || root.isHover)
      if (thumb!=null && thumb!!.image.value!=null) thumb!!.animationPlayPause(selected)
   }

   override fun updateIndex(i: Int) {
      indexVolatile = i
      super.updateIndex(i)
   }

   protected open fun computeGraphics() {
      name = label {
         alignment = Pos.CENTER
         isManaged = false
         isWrapText = true
      }

      thumb = object: Thumbnail() {
         override fun getRepresentant() = item?.value
      }.apply {
         borderVisible = false
         pane.isManaged = false
         pane.isSnapToPixel = true
         view.isSmooth = false
         view.doIfImageLoaded { img ->
            imgLoadAnim?.stop()
            imgLoadAnimItem = item
            if (img==null)
               imgLoadAnim?.applyAt(0.0)
            else
               imgLoadAnim?.playOpenFrom(imgLoadAnimItem!!.loadProgress)
         } on onDispose
      }

      imgLoadAnim = anim(200.millis) {
         if (imgLoadAnimItem!=null) {
            imgLoadAnimItem?.loadProgress = it
            thumb?.view?.opacity = it*it*it*it
         }
      }

      val r = Rectangle(1.0, 1.0).apply {
         id = "grid-cell-stroke"
         styleClass += "grid-cell-stroke"
         isManaged = false
         isMouseTransparent = true
      }

      root = object: StackPane(thumb!!.pane, name, r) {
         override fun layoutChildren() {
            super.layoutChildren()
            val x = 0.0
            val y = 0.0
            val w = width
            val h = height
            val nameGap = 5.emScaled
            val th = computeCellTextHeight()

            if (gridView.value?.cellWidth?.value == CELL_SIZE_UNBOUND) {
               name.alignment = Pos.CENTER_LEFT
               name.resizeRelocate(h + nameGap, y, (w-h-2*nameGap) max 0.0, h)
               thumb!!.pane.resizeRelocate(x, y, h, h)
               r.x = x
               r.y = y
               r.width = h
               r.height = h
            } else {
               name.alignment = Pos.CENTER
               name.resizeRelocate(x + nameGap, h - th + nameGap, (w-2*nameGap) max 0.0, (th-2*nameGap) max 0.0)
               thumb!!.pane.resizeRelocate(x, y, w, h - th)
               r.x = x
               r.y = y
               r.width = w
               r.height = h
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
         hoverProperty() sync { h ->
            hoverAnim.value.playFromDir(h || isSelected)
         }
      }
   }

   /**
    * @implSpec called on fx application thread, must return positive width and height
    * @return size of an image to be loaded for the thumbnail
    */
   protected fun computeThumbSize(): ImageSize = gridView.value.let {
      when {
         it == null -> ImageSize(100.0, 100.0)
         it.cellWidth.value == CELL_SIZE_UNBOUND -> ImageSize(it.cellHeight.value, it.cellHeight.value)
         else -> ImageSize(it.cellWidth.value , it.cellHeight.value - computeCellTextHeight())
      }
   }

   /**
    * @implSpec must be thread safe
    * @return true if the item of this cell is not the same object as the item specified
    */
   @ThreadSafe
   protected fun isInvalidItem(item: Item): Boolean = itemVolatile!==item

   /**
    * @implSpec must be thread safe
    * @return true if the index of this cell is not the same as the index specified
    */
   @ThreadSafe
   protected fun isInvalidIndex(i: Int): Boolean = indexVolatile!=i

   /**
    * @implSpec must be thread safe
    * @return true if this cell is detached from the grid (i.e. not its child)
    */
   @ThreadSafe
   protected fun isInvalidVisibility(): Boolean = parentVolatile==null

   @ThreadSafe
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
      failIfNotFxThread()
      val i = indexVolatile

      if (item.isLoadedCover) {
         if (thumb!!.getImage()!==item.cover) {
            setCoverPost(item, i, item.coverFile, item.cover)
         }
      } else {
         thumb!!.loadFile(null)

         val size = computeThumbSize()
//         failIf(size.width<=0 || size.height<=0)

         loader.execute {
               // Determines minimum loading time/max loading throughput
               // Has a positive effect when hundreds of covers load at once
               sleep(1)

               // Executing this on FX thread would allow us to avoid volatiles for invalid checks and futures
               // I do not know which is better. Out of fear we will need thread-safety in the future, I'm using this approach
               if (!isInvalid(item, i))
                  item.loadCover(size).onOk(FX) { setCoverPost(item, i, item.coverFile, item.cover) }
         }
      }
   }

   private fun setCoverPost(item: Item, i: Int, imgFile: File, img: Image?) {
      if (!disposed && !isInvalid(item, i) && img!=null)
         img.sync1IfImageLoaded { thumb!!.loadImage(img, imgFile) }
   }

   companion object {
      private val loader = burstTPExecutor(1, 1.minutes, threadFactory("dirView-img-loader", true))
   }
}