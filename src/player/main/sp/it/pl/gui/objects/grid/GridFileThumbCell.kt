package sp.it.pl.gui.objects.grid

import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import sp.it.pl.gui.objects.hierarchy.Item
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.main.emScaled
import sp.it.util.JavaLegacy
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.FX
import sp.it.util.async.burstTPExecutor
import sp.it.util.async.sleep
import sp.it.util.async.threadFactory
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileType
import sp.it.util.file.nameOrRoot
import sp.it.util.file.nameWithoutExtensionOrRoot
import sp.it.util.functional.orNull
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.doIfImageLoaded
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfImageLoaded
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.lookupId
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.minutes
import java.io.File
import java.util.concurrent.ExecutorService
import kotlin.math.sqrt

/**
 * GridCell implementation for file using [sp.it.pl.gui.objects.hierarchy.Item]
 * that shows a thumbnail image. Supports asynchronous loading of thumbnails and loading animation.
 */
open class GridFileThumbCell: GridCell<Item, File>() {
   protected lateinit var root: Pane
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
      styleClass += "grid-file-thumb-cell"
      parentProperty() sync { parentVolatile = it?.parent } on onDispose
   }

   protected open fun computeName(item: Item): String = when (item.valType) {
      FileType.DIRECTORY -> item.value.nameOrRoot
      FileType.FILE -> item.value.nameWithoutExtensionOrRoot
   }

   protected open fun computeCellTextHeight(): Double = 40.0

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
      name = Label()
      name.alignment = Pos.CENTER

      thumb = object: Thumbnail() {
         override fun getRepresentant() = item?.value
      }.apply {
         borderVisible = false
         pane.isSnapToPixel = true
         view.isSmooth = true
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
         isMouseTransparent = true
      }

      root = object: Pane(thumb!!.pane, name, r) {
         override fun layoutChildren() {
            val x = 0.0
            val y = 0.0
            val w = width
            val h = height
            val th = computeCellTextHeight()
            thumb!!.pane.resizeRelocate(x, y, w, h - th)
            name.resizeRelocate(x, h - th, w, th)
            r.x = x
            r.y = y
            r.width = w
            r.height = h
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
   protected fun computeThumbSize(): ImageSize = gridView.value
      ?.let { ImageSize(it.cellWidth.value, it.cellHeight.value - computeCellTextHeight()) }
      ?: ImageSize(100.0, 100.0)

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
         failIf(size.width<=0 || size.height<=0)

         loader.execute {
            if (!isInvalid(item, i)) {
               // Determines minimum loading time/max loading throughput
               // Has a positive effect when hundreds of covers load at once
               sleep(5)

               // Executing this on FX thread would allow us avoid volatiles for invalid checks and futures
               // I do not know which is better. Out of fear we will need thread-safety in the future, I'm using this approach
               if (!isInvalid(item, i))
                  item.loadCover(size).onOk(FX) { setCoverPost(item, i, item.coverFile, item.cover) }
            }
         }
      }
   }

   private fun setCoverPost(item: Item, i: Int, imgFile: File, img: Image?) {
      if (!disposed && !isInvalid(item, i) && img!=null)
         img.sync1IfImageLoaded { thumb!!.loadImage(img, imgFile) }
   }

   class Loader(val executorThumbs: ExecutorService?) {
      fun shutdown() {
         executorThumbs?.shutdownNow()
      }
   }

   companion object {
      private val loader = burstTPExecutor(1, 1.minutes, threadFactory("dirView-img-loader", true))
   }
}