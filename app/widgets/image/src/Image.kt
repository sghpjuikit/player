package image

import java.io.File
import javafx.scene.input.KeyCode.*
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.layout.Widget
import sp.it.pl.layout.WidgetCompanion
import sp.it.pl.layout.controller.SimpleController
import sp.it.pl.layout.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.WidgetTags.IMAGE
import sp.it.pl.main.emScaled
import sp.it.pl.main.getImageFileOrUrl
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.toggleNext
import sp.it.util.async.FX
import sp.it.util.async.future.orNull
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.text.keys
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import sp.it.util.units.version
import sp.it.util.units.year

class Image(widget: Widget): SimpleController(widget), ImageDisplayFeature {

   val inputFile = io.i.create<File?>("To display", null) { showImageImpl(it) }

   private val thumb = Thumbnail()
   private val file by cvn<File>(null).only(FILE)
      .def(name = "Custom image", info = "File to display. Does not necessarily have to be an image. Audio or Video files can also display image.")
   private val fitFrom by cv(thumb.fitFrom)
      .def(name = "Fit from", info = "Image fitting.")

   init {
      root.prefSize = 400.emScaled x 400.emScaled

      thumb.isBackgroundVisible = false
      thumb.borderVisible = false
      thumb.isDragEnabled = true
      thumb.viewportEditable.value = true
      root.lay += thumb.pane
      root.isFocusTraversable = true

      root.onEventDown(KEY_PRESSED, ENTER) { file.value?.let { APP.actions.openImageFullscreen(it) } }
      root.onEventDown(KEY_PRESSED, SPACE) { fitFrom.toggleNext() }
      root.onEventDown(KEY_PRESSED, SHIFT, RIGHT)    { thumb.viewportShift(+1 x 0) }
      root.onEventDown(KEY_PRESSED, SHIFT, KP_RIGHT) { thumb.viewportShift(+1 x 0) }
      root.onEventDown(KEY_PRESSED, SHIFT, LEFT)     { thumb.viewportShift(-1 x 0) }
      root.onEventDown(KEY_PRESSED, SHIFT, KP_LEFT)  { thumb.viewportShift(-1 x 0) }
      root.onEventDown(KEY_PRESSED, SHIFT, UP)       { thumb.viewportShift(0 x -1) }
      root.onEventDown(KEY_PRESSED, SHIFT, KP_UP)    { thumb.viewportShift(0 x -1) }
      root.onEventDown(KEY_PRESSED, SHIFT, DOWN)     { thumb.viewportShift(0 x +1) }
      root.onEventDown(KEY_PRESSED, SHIFT, KP_DOWN)  { thumb.viewportShift(0 x +1) }
      root.onEventDown(KEY_PRESSED, M)  { thumb.view.scaleX = -1*thumb.view.scaleX }
      root.onEventDown(KEY_PRESSED, V)  { thumb.view.scaleY = -1*thumb.view.scaleY }
      root.installDrag(
         IconMD.DETAILS,
         "Display image of the file",
         { e -> e.dragboard.hasFiles() || e.dragboard.hasImageFileOrUrl() },
         { e -> file.value!=null && file.value==e.dragboard.files.firstOrNull() },
         { e ->
            if (e.dragboard.hasFiles()) inputFile.value = e.dragboard.files.firstOrNull()
            else e.dragboard.getImageFileOrUrl().onDone(FX) { inputFile.value = it.orNull() }
         }
      )

      file sync { inputFile.value = it }
   }

   override fun focus() = root.requestFocus()

   override fun showImage(imgFile: File?) {
      inputFile.value = imgFile
   }

   private fun showImageImpl(imgFile: File?) {
      file.value = imgFile
      root.sync1IfInScene {
         thumb.view.scaleX = 1.0
         thumb.view.scaleY = 1.0
         thumb.loadFile(file.value)
      }
   }

   companion object: WidgetCompanion {
      override val name = "Image"
      override val description = "Shows a static image"
      override val descriptionLong = "$description."
      override val icon = IconFA.IMAGE
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val tags = setOf(IMAGE)
      override val summaryActions = listOf(
         Entry("Image", "Change image", "Drag & Drop file"),
         Entry("Image", "Toggle fit from inside/outside", keys(SPACE)),
         Entry("Image", "Show fullscreen", keys(ENTER)),
         Entry("Image", "Mirror horizontally", keys(M)),
         Entry("Image", "Mirror vertically", keys(V)),
         Entry("Image", "Move left | right | top | down",  "Mouse drag"),
         Entry("Image", "Move left",  keys(SHIFT, LEFT)),
         Entry("Image", "Move left",  keys(SHIFT, KP_LEFT)),
         Entry("Image", "Move right", keys(SHIFT, RIGHT)),
         Entry("Image", "Move right", keys(SHIFT, KP_RIGHT)),
         Entry("Image", "Move up",    keys(SHIFT, UP)),
         Entry("Image", "Move up",    keys(SHIFT, KP_UP)),
         Entry("Image", "Move down",  keys(SHIFT, DOWN)),
         Entry("Image", "Move down",  keys(SHIFT, KP_DOWN)),
      )
   }
}