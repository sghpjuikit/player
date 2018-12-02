package image

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS
import javafx.scene.input.KeyCode
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.io.IsInput
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.util.async.FX
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cn
import sp.it.pl.util.conf.only
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.graphics.layFullArea
import sp.it.pl.util.validation.Constraint.FileActor.FILE
import java.io.File
import java.util.function.Consumer

@Widget.Info(
        author = "Martin Polakovic",
        name = "Image",
        description = "Shows a static image",
        howto = "Available actions:\n"+"    Drag & drop image : Set custom image",
        version = "1.0",
        year = "2015",
        group = OTHER
)
class Image(widget: Widget<*>): SimpleController(widget), ImageDisplayFeature {

    private val thumb = Thumbnail()

    @IsConfig(name = "Custom image", info = "Image file to display.")
    private var img by cn<File?>(null).only(FILE)

    override fun init() {
        thumb.isBackgroundVisible = false
        thumb.borderVisible = false
        thumb.isDragEnabled = true

        this layFullArea thumb.pane

        installDrag(
                this, DETAILS, "Display",
                { DragUtil.hasImage(it) },
                { e -> img!=null && img==DragUtil.getImageNoUrl(e) },
                { e -> DragUtil.getImage(e) ui { showImage(it) } }
        )
        setOnKeyPressed {
            if (it.code==KeyCode.ENTER) {
                APP.actions.openImageFullscreen(img)
                it.consume()
            }
        }
    }

    override fun refresh() = thumb.loadImage(img)

    @IsInput("To display")
    override fun showImage(imgFile: File) {
        img = imgFile
        thumb.loadImage(imgFile)
        requestFocus()
    }

}