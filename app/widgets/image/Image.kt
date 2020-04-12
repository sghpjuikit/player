package image

import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.ui.objects.image.Thumbnail
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconMD
import sp.it.pl.main.emScaled
import sp.it.pl.main.getImageFileOrUrl
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.util.access.toggleNext
import sp.it.util.conf.cn
import sp.it.util.conf.cv
import sp.it.util.conf.def
import sp.it.util.conf.only
import sp.it.util.file.FileType.FILE
import sp.it.util.reactive.on
import sp.it.util.reactive.onEventDown
import sp.it.util.reactive.sync1IfInScene
import sp.it.util.reactive.syncFrom
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.lay
import sp.it.util.ui.prefSize
import sp.it.util.ui.x
import java.io.File

@Widget.Info(
   author = "Martin Polakovic",
   name = "Image",
   description = "Shows a static image",
   howto = "Available actions:\n    Drag & drop file : Show image representing the file",
   version = "1.1.0",
   year = "2015",
   group = OTHER
)
class Image(widget: Widget): SimpleController(widget), ImageDisplayFeature {

   val inputFile = io.i.create<File>("To display", null) { showImageImpl(it) }

   private val thumb = Thumbnail()
   private var file by cn<File>(null).only(FILE).def(name = "Custom image", info = "File to display. Does not necessarily have to be an image. Audio or Video files can also display image.")
   private val fitFrom by cv(FitFrom.INSIDE).def(name = "Fit from", info = "Image fitting.")

   init {
      root.prefSize = 400.emScaled x 400.emScaled

      thumb.isBackgroundVisible = false
      thumb.borderVisible = false
      thumb.isDragEnabled = true
      thumb.fitFrom syncFrom fitFrom on onClose
      root.lay += thumb.pane

      installDrag(
         root, IconMD.DETAILS, "Display image of the file",
         { e -> e.dragboard.hasFiles() || e.dragboard.hasImageFileOrUrl() },
         { e -> file!=null && file==e.dragboard.files.firstOrNull() },
         { e ->
            if (e.dragboard.hasFiles()) inputFile.value = e.dragboard.files.firstOrNull()
            else e.dragboard.getImageFileOrUrl() ui { inputFile.value = it }
         }
      )
      root.onEventDown(KEY_PRESSED, ENTER) { file?.let { APP.actions.openImageFullscreen(it) } }
      root.onEventDown(KEY_PRESSED, SPACE) { fitFrom.toggleNext() }

      inputFile.value = file
   }

   override fun showImage(imgFile: File?) {
      inputFile.value = imgFile
   }

   private fun showImageImpl(imgFile: File?) {
      file = imgFile

      onClose += root.sync1IfInScene {
         thumb.loadFile(file)
         root.requestFocus()
      }
   }

}