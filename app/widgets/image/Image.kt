package image

import java.io.File
import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.WidgetCompanion
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconFA
import sp.it.pl.main.IconMD
import sp.it.pl.main.emScaled
import sp.it.pl.main.getImageFileOrUrl
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.ui.pane.ShortcutPane.Entry
import sp.it.util.access.toggleNext
import sp.it.util.conf.cv
import sp.it.util.conf.cvn
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
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
      thumb.fitFrom syncFrom fitFrom on onClose
      root.lay += thumb.pane

      root.onEventDown(KEY_PRESSED, ENTER) { file.value?.let { APP.actions.openImageFullscreen(it) } }
      root.onEventDown(KEY_PRESSED, SPACE) { fitFrom.toggleNext() }
      root.installDrag(
         IconMD.DETAILS,
         "Display image of the file",
         { e -> e.dragboard.hasFiles() || e.dragboard.hasImageFileOrUrl() },
         { e -> file.value!=null && file.value==e.dragboard.files.firstOrNull() },
         { e ->
            if (e.dragboard.hasFiles()) inputFile.value = e.dragboard.files.firstOrNull()
            else e.dragboard.getImageFileOrUrl() ui { inputFile.value = it }
         }
      )

      file sync { inputFile.value = it }
   }

   override fun showImage(imgFile: File?) {
      inputFile.value = imgFile
   }

   private fun showImageImpl(imgFile: File?) {
      file.value = imgFile
      root.sync1IfInScene { thumb.loadFile(file.value) } on onClose
   }

   companion object: WidgetCompanion {
      override val name = "Image"
      override val description = "Shows a static image"
      override val descriptionLong = "$description."
      override val icon = IconFA.FONTICONS
      override val version = version(1, 1, 0)
      override val isSupported = true
      override val year = year(2015)
      override val author = "spit"
      override val contributor = ""
      override val summaryActions = listOf(
         Entry("Image", "Show image representing the file", "Drag & Drop file"),
         Entry("Image", "Toggle fit from", keys(SPACE)),
         Entry("Image", "Show fullscreen", keys(ENTER)),
      )
      override val group = OTHER
   }
}