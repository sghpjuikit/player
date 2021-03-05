package albumView

import java.io.File
import java.util.function.Supplier
import javafx.geometry.Pos
import javafx.scene.Parent
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import kotlin.math.round
import kotlin.streams.toList
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.Companion.ALBUM
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.MetadataGroup.Companion.groupsOf
import sp.it.pl.audio.tagging.MetadataGroup.Field.Companion.VALUE
import sp.it.pl.image.ImageStandardLoader
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.LIBRARY
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.FieldedPredicateItemNode.PredicateData
import sp.it.pl.ui.objects.grid.GridCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellGap
import sp.it.pl.ui.objects.grid.GridView.CellSize
import sp.it.pl.ui.objects.image.Cover.CoverSource.DIRECTORY
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.util.JavaLegacy
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.toggleNext
import sp.it.util.access.togglePrevious
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.burstTPExecutor
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runFX
import sp.it.util.async.runIO
import sp.it.util.async.runLater
import sp.it.util.async.sleep
import sp.it.util.async.threadFactory
import sp.it.util.collections.materialize
import sp.it.util.collections.setTo
import sp.it.util.conf.EditMode
import sp.it.util.conf.c
import sp.it.util.conf.cOr
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.defInherit
import sp.it.util.conf.noUi
import sp.it.util.conf.uiNoOrder
import sp.it.util.dev.ThreadSafe
import sp.it.util.dev.failIf
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.div
import sp.it.util.functional.asIs
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.doIfImageLoaded
import sp.it.util.reactive.map
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfImageLoaded
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.sync1IfNonNull
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.Resolution
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.lay
import sp.it.util.ui.lookupId
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.millis
import sp.it.util.units.minutes

@Widget.Info(
   author = "Martin Polakovic",
   name = "AlbumView",
   description = "Displays grid of albums and their covers.",
   version = "1.0.0",
   year = "2015",
   group = LIBRARY
)
class AlbumView(widget: Widget): SimpleController(widget) {

   val outputSelected = io.o.create<MetadataGroup>("Selected Album", null)
   val outputSelectedM = io.o.create<List<Metadata>>("Selected", listOf())
   val inputSongs = io.i.create<List<Metadata>>("To display") { setItems(it) }

   val gridShowFooter by cOr(APP.ui::gridShowFooter, Inherit(), onClose)
      .defInherit(APP.ui::gridShowFooter)
   val gridCellAlignment by cOr<CellGap>(APP.ui::gridCellAlignment, Inherit(), onClose)
      .defInherit(APP.ui::gridCellAlignment)
   val cellSize by cv(CellSize.NORMAL).uiNoOrder().attach { applyCellSize() }
      .def(name = "Thumbnail size", info = "Size of the thumbnail.")
   val cellSizeRatio by cv(Resolution.R_1x1).attach { applyCellSize() }
      .def(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
   private val cellTextHeight = APP.ui.font.map(onClose) { 30.0.emScaled }.apply {
      attach { applyCellSize() }
   }

   val grid = GridView<Album, MetadataGroup>(Album::items, 50.emScaled.x2, 5.emScaled.x2)

   init {
      root.prefSize = 800.emScaled x 800.emScaled
      root.consumeScrolling()
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.lay += grid

      grid.styleClass += "album-grid"
      grid.search.field = VALUE
      grid.filterPrimaryField = VALUE
      grid.cellFactory.value = { AlbumCell() }
      grid.cellAlign syncFrom gridCellAlignment on onClose
      grid.footerVisible syncFrom gridShowFooter on onClose
      grid.selectedItem attach {
         outputSelected.value = it?.items
         outputSelectedM.value = it?.items?.grouped.orEmpty()
      }
      grid.onEventDown(KEY_PRESSED, ENTER) { playSelected() }
      grid.onEventUp(SCROLL) { e ->
         if (e.isShortcutDown) {
            e.consume()
            val isInc = e.deltaY<0 || e.deltaX>0
            val useFreeStyle = e.isShiftDown
            if (useFreeStyle) {
               val preserveAspectRatio = true
               val scaleUnit = 1.2
               val w = grid.cellWidth.value
               val h = grid.cellHeight.value
               val nw = 50.0 max round(if (isInc) w*scaleUnit else w/scaleUnit)
               var nh = 50.0 max round(if (isInc) h*scaleUnit else h/scaleUnit)
               if (preserveAspectRatio) nh = nw/cellSizeRatio.value.ratio
               applyCellSize(nw, nh)
            } else {
               if (isInc) cellSize.togglePrevious()
               else cellSize.toggleNext()
            }
         }
      }

      // update filters of VALUE type, we must wait until skin has been built
      grid.skinProperty().sync1IfNonNull {
         grid.skinImpl!!.filter.inconsistentState = true
         grid.skinImpl!!.filter.prefTypeSupplier = Supplier { PredicateData.ofField(VALUE) }
         grid.skinImpl!!.filter.data = MetadataGroup.Field.all.map { mgf -> PredicateData(mgf.toString(ALBUM), mgf.getMFType(ALBUM), mgf.asIs<ObjectField<MetadataGroup, Any?>>()) }
         grid.skinImpl!!.filter.clear()
      }

      // sync outputs
      val selectedItemsReducer = EventReducer.toLast<Void>(100.0) {
         outputSelected.value = grid.selectedItem.value?.items
      }
      grid.selectedItem attach { if (!selIgnore) selectedItemsReducer.push(null) } on onClose
      grid.selectedItem attach { selLast = it?.name ?: "null" } on onClose
      root.sync1IfInScene { inputSongs.bindDefaultIf1stLoad(APP.db.songs.o) } on onClose

      onClose += { grid.itemsRaw.forEach { it.dispose() } }
      root.sync1IfInScene { applyCellSize() } on onClose
   }

   override fun focus() = grid.requestFocus()

   /** Populates metadata groups to table from metadata list.  */
   private fun setItems(list: List<Metadata>?) {
      if (list==null) return
      lastScrollPosition = grid.skinImpl?.position ?: 0.0

      runIO {
         val mgs = groupsOf(ALBUM, list).toList()
         runFX {
            if (mgs.isNotEmpty()) {
               selectionStore()
               val albumsOld = grid.itemsRaw.materialize()
               val albumsByName = albumsOld.associateBy { it.name }
               val albums = mgs
                  .map {
                     Album(it).apply {
                        albumsByName[name].ifNotNull {
                           coverLoading = it.coverLoading
                           loadProgress = it.loadProgress
                        }
                     }
                  }
                  .sortedBy { it.name }
                  .toList()

               grid.itemsRaw setTo albums
               grid.skinImpl?.position = lastScrollPosition max 0.0
               albumsOld.forEach { it.dispose() }
               selectionReStore()
               outputSelectedM.value = filterList(true)
            }
         }
      }
   }

   private fun applyCellSize(width: Double = cellSize.value.width, height: Double = cellSize.value.width/cellSizeRatio.value.ratio) {
      grid.itemsRaw.forEach { it.disposeCover() }
      grid.cellWidth.value = width.emScaled
      grid.cellHeight.value = height.emScaled + cellTextHeight.value
      grid.horizontalCellSpacing.value = 20.emScaled
      grid.verticalCellSpacing.value = 20.emScaled
   }

   private fun filterList(orAll: Boolean): List<Metadata> = grid.getSelectedOrAllItems(orAll).toList().flatMap { it.items.grouped }

   private fun filerSortInputList() = filterList(false).sortedWith(APP.audio.songOrderComparator)

   private fun playSelected() = play(filerSortInputList())

   private fun play(items: List<Metadata>) {
      if (items.isEmpty()) return
      PlaylistManager.use { it.setNplay(items.stream().sorted(APP.audio.songOrderComparator)) }
   }

   // restoring selection if table items change, we want to preserve as many selected items as possible - when selection
   // changes, we select all items (previously selected) that are still in the table
   private var selIgnore = false
   private var selOld = setOf<Any?>()

   // restoring selection from previous session, we serialize string representation and try to restore when application runs again we restore only once
   private var selLast by c("null").noUi().def(name = "Last selected", editable = EditMode.APP)
   private var selLastRestored = false
   private var lastScrollPosition = -1.0 // 0-1

   private fun selectionStore() {
      selOld = grid.selectedItems.toList().mapTo(HashSet()) { it.items.value }
      selIgnore = true
   }

   private fun selectionReStore() {
      if (grid.itemsRaw.isEmpty()) return

      // restore last selected from previous session, runs once
      if (!selLastRestored) {
         selIgnore = false
         selLastRestored = true
         grid.itemsShown.forEachIndexed { i, album ->
            if (album.name==selLast) {
               grid.skinImpl?.select(i)
               runLater { grid.skinImpl!!.select(i) }
            }
         }
         return
      }

      // restore selection
      grid.itemsShown.forEachIndexed { i, album ->
         if (album.items.value in selOld) {
            grid.skinImpl!!.select(i)
         }
      }
      // performance optimization - prevents refreshes of a lot of items
      if (grid.selectedItem.value==null)
         grid.skinImpl!!.select(0)

      selIgnore = false
      outputSelected.value = grid.selectedItem.value?.items
   }

   class FileImage(val file: File?, val image: Image?)

   class Album(val items: MetadataGroup) {
      val name = items.getValueS("")
      var loadProgress = 0.0 // 0-1
      @Volatile var coverLoading: Fut<FileImage>? = null
      @Volatile var disposed = false

      /** Dispose of this as to never be used again. */
      fun dispose() {
         failIfNotFxThread()
         coverLoading = null
         disposed = true
      }

      /** Dispose of the cover as to be able to load it again. */
      fun disposeCover() {
         failIfNotFxThread()
         coverLoading = null
      }

      fun computeCover(size: ImageSize): Fut<FileImage> {
         return if (disposed) {
            fut(FileImage(null, null)).apply { coverLoading = this }
         } else {
            if (coverLoading==null) {
               runIO {
                  val f = computeCoverFile()
                  val i = if (f==null) null else ImageStandardLoader(f, size)
                  FileImage(f, i)
               }.apply {
                  coverLoading = this
               }
            } else
               coverLoading!!
         }
      }

      private fun computeCoverFile(): File? {
         if (disposed) return null
         return items.grouped.firstOrNull()?.getCover(DIRECTORY)?.getFile()
      }
   }

   inner class AlbumCell: GridCell<Album, MetadataGroup>() {
      private lateinit var root: Pane
      private lateinit var name: Label
      private var thumb: Thumbnail? = null
      private var imgLoadAnim: Anim? = null
      private var imgLoadAnimItem: Album? = null

      private val hoverAnim = lazy {
         anim(150.millis) { root.lookupId<Rectangle>("grid-cell-stroke").strokeWidth = 1 + it*2.emScaled }
      }
      @Volatile private var disposed = false
      private val onDispose = Disposer()
      @Volatile private var itemVolatile: Album? = null
      @Volatile private var parentVolatile: Parent? = null
      @Volatile private var indexVolatile: Int = -1

      init {
         styleClass += "album-grid-cell"
         parentProperty() sync { parentVolatile = it?.parent } on onDispose
      }

      private fun computeName(item: Album): String = item.name

      private fun computeCellTextHeight() = cellTextHeight.value!!

      @Suppress("UNUSED_PARAMETER")
      private fun onAction(item: Album, edit: Boolean) = playSelected()

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

      override fun updateItem(item: Album?, empty: Boolean) {
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
            if (graphic!==root) graphic = root            // set graphics only when necessary
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

      private fun computeGraphics() {
         name = Label()
         name.alignment = Pos.CENTER

         thumb = object: Thumbnail() {
            override fun getRepresentant() = item?.items
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
      private fun computeThumbSize(): ImageSize = gridView.value
         ?.let { ImageSize(it.cellWidth.value, it.cellHeight.value - computeCellTextHeight()) }
         ?: ImageSize(100.0, 100.0)

      /**
       * @implSpec must be thread safe
       * @return true if the item of this cell is not the same object as the item specified
       */
      @ThreadSafe
      private fun isInvalidItem(item: Album): Boolean = itemVolatile!==item

      /**
       * @implSpec must be thread safe
       * @return true if the index of this cell is not the same as the index specified
       */
      @ThreadSafe
      private fun isInvalidIndex(i: Int): Boolean = indexVolatile!=i

      /**
       * @implSpec must be thread safe
       * @return true if this cell is detached from the grid (i.e. not its child)
       */
      @ThreadSafe
      private fun isInvalidVisibility(): Boolean = parentVolatile==null

      @ThreadSafe
      private fun isInvalid(item: Album, i: Int): Boolean = isInvalidItem(item) || isInvalidIndex(i) || isInvalidVisibility()

      /**
       * Begins loading cover for the item. If item changes meanwhile, the result is stored
       * (it will not need to load again) to the old item, but not showed.
       *
       * Thumbnail quality may be decreased to achieve good performance, while loading high
       * quality thumbnail in the bgr. Each phase uses its own executor.
       *
       * Must be called on FX thread.
       */
      private fun setCoverNow(item: Album) {
         failIfNotFxThread()
         val i = indexVolatile

         if (item.coverLoading?.isDone()==true) {
            item.coverLoading?.ifDoneOk {
               setCoverPost(item, i, it.file, it.image)
            }
         } else {
            val size = computeThumbSize().apply {
               failIf(width<=0 || height<=0)
            }

            thumb!!.loadFile(null)
            loader.execute {
               if (!isInvalid(item, i)) {
                  // Determines minimum loading time/max loading throughput
                  // Has a positive effect when hundreds of covers load at once
                  sleep(5)

                  // Executing this on FX thread would allow us avoid volatiles for invalid checks and futures
                  // I do not know which is better. Out of fear we will need thread-safety in the future, I'm using this approach
                  if (!isInvalid(item, i))
                     item.computeCover(size) ui { setCoverPost(item, i, it.file, it.image) }
               }
            }
         }
      }

      private fun setCoverPost(item: Album, i: Int, imgFile: File?, img: Image?) {
         if (!disposed && !isInvalid(item, i) && thumb!!.getImage()!==img)
            img?.sync1IfImageLoaded { thumb!!.loadImage(img, imgFile) }
      }

   }

   companion object {
      private val loader = burstTPExecutor(1, 1.minutes, threadFactory("album-cover-img-loader", true))
   }
}