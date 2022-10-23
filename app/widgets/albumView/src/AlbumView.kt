package albumView

import java.io.File
import java.nio.channels.ClosedByInterruptException
import java.util.function.Supplier
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyEvent.KEY_PRESSED
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.ScrollEvent.SCROLL
import javafx.scene.layout.Pane
import javafx.scene.shape.Rectangle
import kotlin.math.round
import kotlin.math.sqrt
import sp.it.pl.audio.Song
import sp.it.pl.audio.playlist.PlaylistManager
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.audio.tagging.Metadata.Field.ALBUM
import sp.it.pl.audio.tagging.MetadataGroup
import sp.it.pl.audio.tagging.MetadataGroup.Companion.groupsOf
import sp.it.pl.audio.tagging.MetadataGroup.Field.VALUE
import sp.it.pl.image.ImageStandardLoader
import sp.it.pl.layout.Widget
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.SongReader
import sp.it.pl.main.APP
import sp.it.pl.main.Double01
import sp.it.pl.main.WidgetTags.LIBRARY
import sp.it.pl.main.emScaled
import sp.it.pl.ui.itemnode.FieldedPredicateItemNode.PredicateData
import sp.it.pl.ui.objects.grid.GridCell
import sp.it.pl.ui.objects.grid.GridView
import sp.it.pl.ui.objects.grid.GridView.CellGap
import sp.it.pl.ui.objects.grid.GridView.CellSize
import sp.it.pl.ui.objects.grid.ImageLoad
import sp.it.pl.ui.objects.image.Cover.CoverSource.DIRECTORY
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.util.JavaLegacy
import sp.it.util.access.OrV.OrValue.Initial.Inherit
import sp.it.util.access.fieldvalue.ObjectField
import sp.it.util.access.toggle
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.FX
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.runIO
import sp.it.util.async.runLater
import sp.it.util.async.runVT
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
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.div
import sp.it.util.functional.Option
import sp.it.util.functional.Option.None
import sp.it.util.functional.Option.Some
import sp.it.util.functional.asIs
import sp.it.util.functional.getOrSupply
import sp.it.util.functional.ifNotNull
import sp.it.util.functional.orNull
import sp.it.util.math.max
import sp.it.util.reactive.Disposer
import sp.it.util.reactive.attach
import sp.it.util.reactive.consumeScrolling
import sp.it.util.reactive.doIfImageLoaded
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfImageLoaded
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.sync1IfNonNull
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.Resolution
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.Interrupts
import sp.it.util.ui.lay
import sp.it.util.ui.maxSize
import sp.it.util.ui.minSize
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.ui.x2
import sp.it.util.units.millis

@Widget.Info(
   author = "Martin Polakovic",
   name = "Song Album Grid",
   description = "Displays grid of albums and their covers.",
   version = "1.0.0",
   year = "2015",
   tags = [ LIBRARY ]
)
class AlbumView(widget: Widget): SimpleController(widget), SongReader {

   val outputSelected = io.o.create<MetadataGroup?>("Selected Album", null)
   val outputSelectedM = io.o.create<List<Metadata>>("Selected", listOf())
   val inputSongs = io.i.create<List<Metadata>>("To display", listOf()) { setItems(it) }

   val grid = GridView<Album, MetadataGroup>(Album::items, 50.emScaled.x2, 5.emScaled.x2)

   val gridShowFooter by cOr(APP.ui::gridShowFooter, grid.footerVisible, Inherit(), onClose)
      .defInherit(APP.ui::gridShowFooter)
   val gridCellAlignment by cOr<CellGap>(APP.ui::gridCellAlignment, grid.cellAlign, Inherit(), onClose)
      .defInherit(APP.ui::gridCellAlignment)
   val cellSize by cv(CellSize.NORMAL).uiNoOrder().attach { applyCellSize() }
      .def(name = "Thumbnail size", info = "Size of the thumbnail.")
   val cellSizeRatio by cv(Resolution.R_1x1).attach { applyCellSize() }
      .def(name = "Thumbnail size ratio", info = "Size ratio of the thumbnail.")
   val coverFitFrom by cv(FitFrom.OUTSIDE)
      .def(name = "Thumbnail fit image from", info = "Determines whether image will be fit from inside or outside.")
   private val cellTextHeight = APP.ui.font.map { 30.0.emScaled }.apply { attach { applyCellSize() } on onClose }

   init {
      root.prefSize = 800.emScaled x 800.emScaled
      root.consumeScrolling()
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()
      root.lay += grid

      grid.styleClass += "album-grid"
      grid.search.field = VALUE
      grid.filterPrimaryField = VALUE
      grid.cellFactory.value = { AlbumCell() }
      grid.selectedItem attach {
         outputSelected.value = it?.items
         outputSelectedM.value = it?.items?.grouped.orEmpty().toList()
      }
      grid.onEventDown(KEY_PRESSED, ENTER) { playSelected() }
      grid.onEventUp(SCROLL) { e ->
         if (e.isShortcutDown) {
            e.consume()
            val isDec = e.deltaY<0 || e.deltaX>0
            val useFreeStyle = e.isShiftDown
            if (useFreeStyle) {
               val preserveAspectRatio = true
               val scaleUnit = 1.2
               val w = grid.cellWidth.value
               val h = grid.cellHeight.value
               val nw = 50.0 max round(if (isDec) w*scaleUnit else w/scaleUnit)
               var nh = 50.0 max round(if (isDec) h*scaleUnit else h/scaleUnit)
               if (preserveAspectRatio) nh = nw/cellSizeRatio.value.ratio
               applyCellSize(nw, nh)
            } else {
               cellSize.toggle(isDec)
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
      root.sync1IfInScene { inputSongs.bindDefaultIf1stLoad(APP.db.songs) } on onClose

      onClose += { grid.itemsRaw.forEach { it.dispose() } }
      root.sync1IfInScene { applyCellSize() } on onClose
   }

   override fun focus() = grid.requestFocus()

   override fun read(songs: List<Song>) {
      inputSongs.value = songs.map { it.toMeta() }
   }

   /** Populates metadata groups to table from metadata list.  */
   private fun setItems(list: List<Metadata>?) {
      if (list==null) return
      lastScrollPosition = grid.skinImpl?.position ?: 0.0

      runIO {
         groupsOf(ALBUM, list)
      } ui { mgs ->
         if (mgs.isNotEmpty()) {
            selectionStore()
            val albumsOld = grid.itemsRaw.materialize()
            val albumsByName = albumsOld.associateBy { it.name }
            val albums = mgs
               .map {
                  Album(it).apply {
                     albumsByName[name].ifNotNull {
                        cover = it.cover
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
      PlaylistManager.use { it.setAndPlay(items.stream().sorted(APP.audio.songOrderComparator)) }
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

   class Album(val items: MetadataGroup) {
      val name = items.getValueS("")
      var loadProgress: Double01 = 0.0
      var cover: ImageLoad = ImageLoad.NotStarted
      @Volatile var loadingThread: Thread? = null

      /** Dispose of this as to never be used again. */
      fun dispose() {
         failIfNotFxThread()
         cover = ImageLoad.DoneErr
         loadingThread = null
      }

      /** Dispose of the cover as to be able to load it again. */
      fun disposeCover() {
         failIfNotFxThread()
         cover = ImageLoad.NotStarted
      }

      fun computeCover(size: ImageSize): Fut<ImageLoad> {
         failIfNotFxThread()

         return when (val c = cover) {
            is ImageLoad.DoneErr -> fut(ImageLoad.DoneErr)
            is ImageLoad.DoneOk -> fut(ImageLoad.DoneErr)
            is ImageLoad.Loading -> c.loading
            is ImageLoad.DoneInterrupted -> computeCoverAsync(Some(c.file), size)
            is ImageLoad.NotStarted -> computeCoverAsync(None, size)
         }
      }

      private fun computeCoverAsync(coverFile: Option<File?>, size: ImageSize): Fut<ImageLoad> =
         runVT {
            loadingThread = Thread.currentThread()
            val f = coverFile.getOrSupply { computeCoverFile() }
            try {
               val i = f?.let { ImageStandardLoader(it, size) }
               if (i==null && Interrupts.isInterrupted) ImageLoad.DoneInterrupted(f)
               else ImageLoad.DoneOk(i, f)
            } catch (e: Throwable) {
               if (e is InterruptedException) ImageLoad.DoneInterrupted(f)
               else if (e is ClosedByInterruptException) ImageLoad.DoneInterrupted(f)
               else if (Interrupts.isInterrupted) ImageLoad.DoneInterrupted(f)
               else ImageLoad.DoneErr
            }
         }.then(FX) {
            cover = it
            it
         }.apply {
            cover = ImageLoad.Loading(this)
         }

      private fun computeCoverFile(): File? =
         items.grouped.firstOrNull()?.getCover(DIRECTORY)?.getFile()
   }

   inner class AlbumCell: GridCell<Album, MetadataGroup>() {
      private lateinit var root: Pane
      private lateinit var name: Label
      private lateinit var stroke: Rectangle
      private var thumb: Thumbnail? = null
      private var imgLoadAnim: Anim? = null
      private var imgLoadAnimItem: Album? = null
      private var isJustVisible = false

      private val hoverAnim = lazy {
         anim(150.millis) {
            val p = sqrt(it)
            val s = 2.emScaled
            val x = -s+(1-p)*(s + computeCellTextHeight())
            stroke.strokeWidth = (p*s) max 1.0
            name.style = "-fx-background-insets: $x 0 0 0;"
         }
      }
      private var disposed = false
      private val disposer = Disposer()

      init {
         styleClass += "album-grid-cell"
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
         disposer()
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

      override fun updateItem(item: Album?, empty: Boolean) {
         if (disposed) return
         if (item===getItem()) return
         super.updateItem(item, empty)

         if (imgLoadAnim!=null) {
            imgLoadAnim?.stop()
            imgLoadAnimItem = item
            imgLoadAnim?.applyAt(item?.loadProgress ?: 0.0)
         }

         if (empty) {
            // do not discard contents of the graphics
         } else {
            if (!::root.isInitialized) computeGraphics()  // create graphics lazily and only once
            if (graphic!==root) graphic = root           // set graphics only when necessary

            if (item!=null) {
               name.text = computeName(item)
               setCoverNow(item)
            }
         }
      }

      override fun updateSelected(selected: Boolean) {
         super.updateSelected(selected)
         hoverAnim.value.playFromDir(selected || root.isHover)
         if (thumb!=null && thumb!!.image.value!=null) thumb!!.animationPlayPause(selected)
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
            fitFrom syncFrom coverFitFrom on disposer
            view.doIfImageLoaded { img ->
               imgLoadAnim?.stop()
               imgLoadAnimItem = item
               if (img==null)
                  imgLoadAnim?.applyAt(0.0)
               else
                  imgLoadAnim?.playOpenFrom(imgLoadAnimItem!!.loadProgress)
            } on disposer
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
            isMouseTransparent = true
         }

         root = object: Pane(thumb!!.pane, name, stroke) {
            override fun layoutChildren() {
               val x = 0.0 ; val y = 0.0 ; val w = width ; val h = height ; val th = computeCellTextHeight()
               thumb!!.pane.resizeRelocate(x, y, w, h - th)
               name.resizeRelocate(x, h - th, w, th)
               stroke.x = x; stroke.y = y; stroke.width = w; stroke.height = h
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

      /** @return size of an image to be loaded for the thumbnail */
      private fun computeThumbSize(): ImageSize = gridView.value
         ?.let { ImageSize(it.cellWidth.value, it.cellHeight.value - computeCellTextHeight()) }
         ?: ImageSize(100.0, 100.0)

      /** @return true if the item of this cell is not the same object as the item specified */
      private fun isInvalidItem(item: Album): Boolean = item!==this.item

      /** @return true if the index of this cell is not the same as the index specified */
      private fun isInvalidIndex(index: Int): Boolean = index!=this.index

      /** @return true if this cell is detached from the grid (i.e. not its child) */
      private fun isInvalidVisibility(): Boolean = parent==null

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

   private fun setCoverPost(item: Album, index: Int, img: ImageLoad) {
      if (!disposed && !isInvalid(item, index) && thumb!!.getImage()!==img.image) {
         imgLoadAnim?.stop()
         imgLoadAnimItem = item
         imgLoadAnim?.playOpenFrom(item.loadProgress)
         img.image.sync1IfImageLoaded {
            thumb!!.loadImage(img.image, img.file)
         }
      }
   }

   }

}