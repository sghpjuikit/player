package imageViewer

import javafx.animation.Animation.INDEFINITE
import javafx.beans.property.SimpleObjectProperty
import javafx.event.EventHandler
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.input.MouseButton.PRIMARY
import javafx.scene.input.MouseButton.SECONDARY
import javafx.scene.input.MouseEvent
import javafx.scene.input.MouseEvent.MOUSE_CLICKED
import javafx.scene.input.MouseEvent.MOUSE_ENTERED
import javafx.scene.input.MouseEvent.MOUSE_EXITED
import javafx.scene.input.MouseEvent.MOUSE_MOVED
import javafx.util.Duration.ZERO
import sp.it.pl.audio.Song
import sp.it.pl.audio.tagging.Metadata
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.Widget.Info
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconOC
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.pl.main.isImage
import sp.it.pl.main.toMetadata
import sp.it.pl.ui.nodeinfo.ItemInfo
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.util.access.toggle
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.async.runIO
import sp.it.util.collections.setToOne
import sp.it.util.conf.EditMode
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.file.Util.getCommonRoot
import sp.it.util.file.Util.getFilesR
import sp.it.util.file.div
import sp.it.util.functional.invoke
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.ui.Util.layAnchor
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.seconds
import java.io.File
import java.util.ArrayList
import java.util.function.Predicate
import kotlin.streams.toList

@Info(
   author = "Martin Polakovic",
   name = "Image Viewer",
   description = "Displays images in a directory or song location. Looks for images in subfolders.",
   howto = """
      The widget displays an image and image thumbnails for images in specific directory - data source. Main image can change automatically  (slideshow) or manually by clicking on the thumbnail, or navigating to next/previous image.
      User can display image or images in a location by setting the file or directory, e.g., by drag & drop. The widget can also follow playing or selected songs, displaying images in their parent directory.
      The image search is recursive and search depth configurable.
      
      Available actions:
          Left click: Shows/hides thumbnails
          Left click bottom : Toggles info pane
          Nav icon click : Previous/Next image
          Info pane right click : Shows/hides bacground for info pane
          Image right click : Opens image context menu
          Thumbnail left click : Set as image
          Thumbnail right click : Opens thumbnail context menu
          Drag&Drop audio : Displays images for the first dropped item
          Drag&Drop image : Show images
   """,
   version = "1.0.0",
   year = "2015",
   group = OTHER
)
class ImageViewer(widget: Widget): SimpleController(widget) {

   private val inputLocation = io.i.create<File>("Location") { dataChanged(it) }
   private val inputLocationOf = io.io.mapped<File, Song>(inputLocation, "Location of") { it.getLocation() }

   private val mainImage = Thumbnail()
   private var itemPane: ItemInfo? = null
   private val navAnim: Anim
   private val folder = SimpleObjectProperty<File?>(null)
   private val images = ArrayList<File>()
   private val slideshow = fxTimer(ZERO, INDEFINITE) { nextImage() }

   val slideshowDur by cv(15.seconds).sync { slideshow.setTimeoutAndRestart(it) }
      .def(name = "Slideshow reload time", info = "Time between picture change.")
   val slideshowOn by cv(true).sync { slideshow.setRunning(it) }
      .def(name = "Slideshow", info = "Turn slideshow on/off.")
   val showImage by cv(true).attach { mainImage.pane.isVisible = it }
      .def(name = "Show big image", info = "Show thumbnails.")
   val theaterMode by cv(false)
      .def(name = "Theater mode", info = "Turns off slideshow, shows image background to fill the screen, disables image border and displays information about the song.")
   var keepContentOnEmpty by c(true)
      .def(name = "Forbid no content", info = "Ignores empty directories and does not change displayed images if there is nothing to show.")
   var folderTreeDepth by c(2)
      .def(name = "File search depth", info = "Depth to search for files in folders. 1 for current folder only.")
   var thumbsLimit by c(50)
      .def(name = "Max number of thumbnails", info = "Important for directories with lots of images.")
   private var activeImage by c(-1)
      .def(name = "Displayed image", editable = EditMode.APP)

   init {
      root.styleClass += "img-viewer-root"
      root.prefSize = 400.emScaled x 400.emScaled
      root.stylesheets += (location/"skin.css").toURI().toASCIIString()

      mainImage.borderVisible = true
      mainImage.isBorderToImage = true
      root.lay += mainImage.pane

      val nextB = Icon(IconOC.ARROW_RIGHT).apply {
         styleclass("nav-icon")
         tooltip("Next image")
         onClickDo { nextImage() }
         isMouseTransparent = true
      }
      val nextP = stackPane(nextB).apply {
         onEventDown(MOUSE_CLICKED, PRIMARY) { nextB.onClickRunnable?.invoke() }
         styleClass setToOne "nav-pane"
         prefWidthProperty().bind(root.widthProperty().divide(10))
         minWidth = 20.0
         maxWidth = 50.0
         visibleProperty().bind(opacityProperty().isNotEqualTo(0))
      }
      val prevB = Icon(IconOC.ARROW_LEFT).apply {
         styleclass("nav-icon")
         tooltip("Previous image")
         onClickDo { prevImage() }
         isMouseTransparent = true
      }
      val prevP = stackPane(prevB).apply {
         onEventDown(MOUSE_CLICKED, PRIMARY) { prevB.onClickRunnable?.invoke() }
         styleClass setToOne "nav-pane"
         prefWidthProperty().bind(root.widthProperty().divide(10))
         minWidth = 20.0
         maxWidth = 50.0
         visibleProperty().bind(opacityProperty().isNotEqualTo(0))
      }
      root.lay(CENTER_LEFT) += prevP
      root.lay(CENTER_RIGHT) += nextP
      navAnim = anim {
         prevP.opacity = it
         nextP.opacity = it
         prevB.translateX = +40*(it - 1)
         nextB.translateX = -40*(it - 1)
      }.dur(300.millis).apply {
         applyNow()
      }
      val navInactive = EventReducer.toLast<Any>(1000.0) { if (!nextP.isHover && !prevP.isHover) navAnim.playClose() }
      val navActive = EventReducer.toFirstDelayed<MouseEvent>(400.0) { navAnim.playOpen() }
      root.onEventUp(MOUSE_EXITED) { navInactive.push(it) }
      root.onEventUp(MOUSE_MOVED) {
         navActive.push(it)
         if (!nextP.isHover && !prevP.isHover) navInactive.push(it)
      }

      // thumbnails
      root.onMouseClicked = EventHandler { e: MouseEvent ->
         if (e.button==PRIMARY) {
            if (e.y>0.8*root.height && e.x>0.7*root.width) {
               theaterMode.toggle()
               e.consume()
            }
         }
      }

      // slideshow on hold during user activity
      root.onEventUp(MOUSE_ENTERED) { if (slideshowOn.value) slideshow.pause() }
      root.onEventUp(MOUSE_EXITED) { if (slideshowOn.value) slideshow.unpause() }

      installDrag(
         root, IconMD.DETAILS, "Display",
         { it.dragboard.hasImageFileOrUrl() || it.dragboard.hasAudio() || it.dragboard.hasFiles() },
         { it.gestureSource===mainImage.pane },
         {
            if (it.dragboard.hasFiles()) {
               dataChanged(getCommonRoot(it.dragboard.files))
            } else {
               if (it.dragboard.hasAudio()) {
                  val songs = it.dragboard.getAudio()
                  if (songs.isNotEmpty()) inputLocationOf.value = songs[0]
               }
            }
         }
      )
      theaterMode.sync { applyTheaterMode(it) }

      onClose += { slideshow.stop() }
      onClose += root.sync1IfInScene {
         if (!inputLocation.isBound(widget.id) && !inputLocationOf.isBound(widget.id) && !widget.isDeserialized)
            inputLocationOf.bind(APP.audio.playing.o)

         folder sync { readThumbnails() }
      }
   }

   val isEmpty: Boolean
      get() = images.isEmpty()

   private fun dataChanged(newLocation: File?) {
      if (keepContentOnEmpty && newLocation==null) return  // prevent refreshing location if should not
      folder.value = newLocation
      if (theaterMode.value) {
         itemPane?.setValue(Metadata.EMPTY)
         val s = inputLocationOf.value
         s?.toMetadata { itemPane?.setValue(it) }
      }
   }

   private fun readThumbnails() {
      images.clear()
      val source = folder.value
      runIO {
         if (source==null) listOf()
         else getFilesR(source, folderTreeDepth, Predicate { it.isImage() }).limit(thumbsLimit.toLong()).toList()
      } ui { files ->
         val ai = activeImage
         if (files.isEmpty()) {
            setImage(-1)
         } else {
            files.forEachIndexed { i, f ->
               insertThumbnail(f)
               if (i==ai) setImage(ai)
            }
         }
      }
   }

   private fun insertThumbnail(f: File) {
      images.add(f)
      // if this is first thumbnail display it immediately
      // but only if the displayed image is not one of the thumbnails - is not located
      // in folder.get() directory
      // avoids image loading + necessary to display custom image, which fires
      // thumbnail refresh and subsequently would change the displayed image
      val displLoc = if (mainImage.file==null) null else mainImage.file.parentFile
      val currLoc = folder.get()
      if (images.size==1 && currLoc!=null && currLoc!=displLoc) setImage(0)
   }

   private fun setImage(index: Int) {
      var i = index
      if (images.isEmpty()) i = -1
      if (i>=images.size) i = images.size - 1
      if (i==-1) {
         mainImage.loadFile(null)
         // this is unnecessary because we check the index for validity
         // also unwanted, sometimes this would erase our deserialized index
         //  active_image = -1;
      } else {
         mainImage.loadFile(images[i])
         activeImage = i
      }
   }

   fun nextImage() {
      if (images.size==1) return
      if (images.isEmpty()) {
         setImage(-1)
      } else {
         val index = if (activeImage>=images.size - 1) 0 else activeImage + 1
         setImage(index)
      }
      if (slideshow.isRunning) slideshow.start()
   }

   fun prevImage() {
      if (images.size==1) return
      if (images.isEmpty()) {
         setImage(-1)
      } else {
         val index = if (activeImage<1) images.size - 1 else activeImage - 1
         setImage(index)
      }
      if (slideshow.isRunning) slideshow.start()
   }

   private fun applyTheaterMode(v: Boolean) {
      root.pseudoClassChanged("theater", v)
      if (v && itemPane==null) {
         itemPane = ItemInfo(false).apply {
            setValue(Metadata.EMPTY)
            onEventDown(MOUSE_CLICKED, SECONDARY) {
               styleclassToggle("block-alternative")
            }
         }
         val itemPaneRoot = layAnchor(itemPane, null, 20.0, 20.0, null).apply {
            isPickOnBounds = false
         }
         root.lay += itemPaneRoot
         inputLocationOf.value?.toMetadata { itemPane!!.setValue(it) }
      }
      slideshowOn.value = if (v) false else slideshowOn.value
      mainImage.isBackgroundVisible = v
      mainImage.borderVisible = !v
      itemPane?.isVisible = v
   }

}