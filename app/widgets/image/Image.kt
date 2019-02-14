package image

import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.io.IsInput
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconMD
import sp.it.pl.util.async.runLater
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cn
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.graphics.image.FitFrom
import sp.it.pl.util.graphics.lay
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.sync1IfNonNull
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.validation.Constraint.FileActor.FILE
import java.io.File

@Widget.Info(
        author = "Martin Polakovic",
        name = "Image",
        description = "Shows a static image",
        howto = "Available actions:\n"+"    Drag & drop image : Set custom image",
        version = "1.0",
        year = "2015",
        group = OTHER
)
class Image(widget: Widget): SimpleController(widget), ImageDisplayFeature {

    private val thumb = Thumbnail()

    @IsConfig(name = "Custom image", info = "Image file to display.")
    private var img by cn<File?>(null).only(FILE)
    @IsConfig(name = "Fit from", info = "Image fitting.")
    private val fitFrom by cv(FitFrom.INSIDE)

    init {
        thumb.isBackgroundVisible = false
        thumb.borderVisible = false
        thumb.isDragEnabled = true
        thumb.fitFrom syncFrom fitFrom on onClose

        root.lay += thumb.pane

        installDrag(
                root, IconMD.DETAILS, "Display",
                { DragUtil.hasImage(it) },
                { e -> img!=null && img==DragUtil.getImageNoUrl(e) },
                { e -> DragUtil.getImage(e) ui { showImage(it) } }
        )
        root.onEventDown(KEY_PRESSED) {
            if (it.code==KeyCode.ENTER) {
                img?.let { APP.actions.openImageFullscreen(it) }
                it.consume()
            }
        }

        root.sceneProperty().sync1IfNonNull { showImage(img) } on onClose
    }

    @IsInput("To display")
    override fun showImage(imgFile: File?) {
        img = imgFile
        if (root.scene==null) return

        runLater {
            thumb.loadImage(imgFile)    // runLater to prevent loading image with size of 0 (full size), TODO: fix
        }

        root.requestFocus()
    }

}