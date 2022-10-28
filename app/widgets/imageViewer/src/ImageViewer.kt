package imageViewer

import javafx.animation.Animation.INDEFINITE
import javafx.beans.property.SimpleObjectProperty
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
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.IconUN
import sp.it.pl.main.emScaled
import sp.it.pl.main.getAudio
import sp.it.pl.main.hasAudio
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.pl.main.isImage
import sp.it.pl.main.toMetadata
import sp.it.pl.ui.nodeinfo.SongInfo
import sp.it.pl.ui.objects.icon.Icon
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.toggle
import sp.it.util.animation.Anim
import sp.it.util.animation.Anim.Companion.anim
import sp.it.util.async.executor.EventReducer
import sp.it.util.async.executor.FxTimer.Companion.fxTimer
import sp.it.util.collections.setToOne
import sp.it.util.conf.EditMode
import sp.it.util.conf.c
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.file.Util.getCommonRoot
import sp.it.util.file.div
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.onEventUp
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.text.nameUi
import sp.it.util.ui.Util.layAnchor
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.pseudoClassChanged
import sp.it.util.ui.stackPane
import sp.it.util.ui.styleclassToggle
import sp.it.util.ui.x
import sp.it.util.units.millis
import sp.it.util.units.seconds
import sp.it.util.units.version
import sp.it.util.units.year
import java.io.File
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.main.WidgetTags.IMAGE
import sp.it.pl.ui.objects.icon.onClickDelegateKeyTo
import sp.it.pl.ui.objects.icon.onClickDelegateMouseTo
import sp.it.util.access.toggleNext
import sp.it.util.async.runVT
import sp.it.util.file.FileType.FILE
import sp.it.util.file.getFilesR
import sp.it.util.text.keys
import sp.it.util.ui.anchorPane
import sp.it.util.ui.layFullArea
import sp.it.util.ui.minSize

class ImageViewer(widget: Widget): SimpleController(widget) {
   private val inputLocation = io.i.create<File?>("Location", null) { dataChanged(it) }
   private val inputLocationOf = io.io.mapped<File?, Song?>(inputLocation, "Location of") { it?.getLocation() }
   private val mainImage = Thumbnail()
   private var itemPane: SongInfo? = null
   private val navAnim: Anim
   private val folder = SimpleObjectProperty<File?>(null)
   private val images = mutableListOf<File>()
   private val slideshow = fxTimer(ZERO, INDEFINITE) { visitNextImage() }

   val slideshowDur by cv(15.seconds).sync { slideshow.setTimeoutAndRestart(it) }
      .def(name = "Slideshow reload time", info = "Time between picture change.")
   val slideshowOn by cv(true).sync { slideshow.setRunning(it) }
      .def(name = "Slideshow", info = "Turn slideshow on/off.")
   val showImage by cv(true).attach { mainImage.pane.isVisible = it }
      .def(name = "Show big image", info = "Show thumbnails.")
   val fitFrom by cv(mainImage.fitFrom)
      .def(name = "Fit from", info = "Image fitting.")
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

      root.lay += anchorPane {
         minSize = 0 x 0
         layFullArea += mainImage.pane
      }
      root.onEventDown(KEY_PRESSED, ENTER) { images.getOrNull(activeImage)?.let { APP.actions.openImageFullscreen(it) } }
      root.onEventDown(KEY_PRESSED, SPACE) { fitFrom.toggleNext() }
      root.onEventDown(KEY_PRESSED, HOME) { visitFirstImage() }
      root.onEventDown(KEY_PRESSED, LEFT) { visitPrevImage() }
      root.onEventDown(KEY_PRESSED, KP_LEFT) { visitPrevImage() }
      root.onEventDown(KEY_PRESSED, RIGHT) { visitNextImage() }
      root.onEventDown(KEY_PRESSED, KP_RIGHT) { visitNextImage() }
      root.onEventDown(KEY_PRESSED, END) { visitLastImage() }

      val nextB = Icon(IconUN(0x02af8)).apply {
         styleclass("nav-icon")
         tooltip("Next image")
         onClickDo { visitNextImage() }
      }
      val nextP = stackPane(nextB).apply {
         nextB.isFocusTraversable = false
         nextB.focusOwner.value = this
         nextB.onClickDelegateKeyTo(this)
         nextB.onClickDelegateMouseTo(this)
         styleClass setToOne "nav-pane"
         prefWidthProperty().bind(root.widthProperty().divide(10))
         minWidth = 20.0
         maxWidth = 50.0
         visibleProperty().bind(opacityProperty().isNotEqualTo(0))
      }
      val prevB = Icon(IconUN(0x2af7)).apply {
         styleclass("nav-icon")
         tooltip("Previous image")
         onClickDo { visitPrevImage() }
      }
      val prevP = stackPane(prevB).apply {
         prevB.isFocusTraversable = false
         prevB.focusOwner.value = this
         prevB.onClickDelegateKeyTo(this)
         prevB.onClickDelegateMouseTo(this)
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
      val navActive = EventReducer.toFirstDelayed<MouseEvent>(400.0) { if (images.size>1) navAnim.playOpen() }
      root.onEventUp(MOUSE_EXITED) { navInactive.push(it) }
      root.onEventUp(MOUSE_MOVED) {
         navActive.push(it)
         if (!nextP.isHover && !prevP.isHover) navInactive.push(it)
      }

      root.onEventUp(MOUSE_ENTERED) { if (slideshowOn.value) slideshow.pause() }
      root.onEventUp(MOUSE_EXITED) { if (slideshowOn.value) slideshow.unpause() }
      root.onEventDown(MOUSE_CLICKED, PRIMARY) { theaterMode.toggle() }
      theaterMode sync ::applyTheaterMode

      root.installDrag(
         IconMD.DETAILS,
         "Display",
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

      onClose += slideshow::stop
      onClose += root.sync1IfInScene {
         if (!inputLocation.isBoundUnless(widget.id) && !inputLocationOf.isBoundUnless(widget.id) && !widget.isDeserialized)
            inputLocationOf.bind(APP.audio.playing)

         folder sync { readThumbnails() }
      }
   }

   val isEmpty: Boolean
      get() = images.isEmpty()

   private fun dataChanged(newLocation: File?) {
      if (keepContentOnEmpty && newLocation==null) return

      folder.value = newLocation
      if (theaterMode.value) {
         itemPane?.song?.value = null
         val s = inputLocationOf.value
         s?.toMetadata { itemPane?.song?.value = it }
      }
   }

   private fun readThumbnails() {
      images.clear()
      val source = folder.value
      runVT {
         if (source==null) listOf()
         else source.getFilesR(folderTreeDepth, FILE) { p, _ -> p.isImage() }.take(thumbsLimit)
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
      val locationDisplayed = if (mainImage.file==null) null else mainImage.file.parentFile
      val locationCurrent = folder.get()
      if (images.size==1 && locationCurrent!=null && locationCurrent!=locationDisplayed) setImage(0)
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

   fun visitLastImage() = visit { images.lastIndex }

   fun visitNextImage() = visit { if (activeImage>=images.lastIndex) 0 else activeImage + 1 }

   fun visitPrevImage() = visit { if (activeImage<1) images.lastIndex else activeImage - 1 }

   fun visitFirstImage() = visit { 0 }

   private fun visit(block: () -> Int) {
      if (images.size==1) return
      setImage(if (images.isEmpty()) -1 else block())
      if (slideshow.isRunning) slideshow.start()
   }

   private fun applyTheaterMode(v: Boolean) {
      root.pseudoClassChanged("theater", v)
      if (v && itemPane==null) {
         itemPane = SongInfo(false).apply {
            song.value = null
            onEventDown(MOUSE_CLICKED, SECONDARY) { styleclassToggle("block-alternative") }
         }
         root.lay += layAnchor(itemPane, null, 20.0, 20.0, null).apply {
            isPickOnBounds = false
         }
         inputLocationOf.value?.toMetadata { itemPane?.song?.value = it }
      }
      slideshowOn.value = if (v) false else slideshowOn.value
      mainImage.isBackgroundVisible = v
      mainImage.borderVisible = !v
      itemPane?.isVisible = v
   }

   companion object: WidgetCompanion {
      override val name = "Image Viewer"
      override val description = "Displays images in a directory or song location. Looks for images in sub-folders."
      override val descriptionLong = """
         The widget displays an image in an input directory or input song's location. Displayed image can change automatically (slideshow) or by using the navigation icons.
         The widget can also follow playing or selected songs, displaying images in their parent directory.
         The image search within the location is recursive and search depth is configurable.
      """
      override val icon = IconFA.FONTICONS
      override val version = version(1, 0, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(IMAGE)
      override val summaryActions = listOf(
         Entry("Song", "Toggle song detail", "Image ${PRIMARY.nameUi}"),
         Entry("Song", "Toggle song detail background", "Song detail ${SECONDARY.nameUi}"),
         Entry("Image", "Previous image", "Nav icon ${PRIMARY.nameUi}"),
         Entry("Image", "Next image", "Nav icon ${PRIMARY.nameUi}"),
         Entry("Image", "Opens image context menu", "Image ${SECONDARY.nameUi}"),
         Entry("Image", "Show image context menu", "Thumbnail ${SECONDARY.nameUi}"),
         Entry("Image", "Show images for location of the 1st song", "Drag & Drop audio file"),
         Entry("Image", "Show images for location", "Drag & Drop file"),
         Entry("Image", "Toggle fit from", keys(SPACE)),
         Entry("Image", "Show fullscreen", keys(ENTER)),
         Entry("Navigation", "First image", keys(HOME)),
         Entry("Navigation", "Previous image", keys(LEFT)),
         Entry("Navigation", "Previous image", keys(KP_LEFT)),
         Entry("Navigation", "Next image", keys(RIGHT)),
         Entry("Navigation", "Next image", keys(KP_RIGHT)),
         Entry("Navigation", "Last image", keys(END)),
      )
   }
}