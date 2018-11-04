package image

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS
import javafx.fxml.FXML
import javafx.scene.input.KeyCode
import javafx.scene.layout.AnchorPane
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.FXMLController
import sp.it.pl.layout.widget.controller.io.IsInput
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.util.async.FX
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.graphics.Util.setAnchor
import sp.it.pl.util.graphics.drag.DragUtil
import sp.it.pl.util.graphics.drag.DragUtil.installDrag
import sp.it.pl.util.validation.Constraint
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
class Image: FXMLController(), ImageDisplayFeature {

    @FXML private lateinit var root: AnchorPane
    private val thumb = Thumbnail()

    @Constraint.FileType(FILE)
    @IsConfig(name = "Custom image", info = "Image file to display.")
    private var img: File? = null

    override fun init() {
        thumb.isBackgroundVisible = false
        thumb.borderVisible = false
        thumb.isDragEnabled = true
        setAnchor(root, thumb.pane, 0.0)

        installDrag(
                root, DETAILS, "Display",
                { DragUtil.hasImage(it) },
                { e -> img!=null && img==DragUtil.getImageNoUrl(e) },
                { e -> DragUtil.getImage(e).use(FX, Consumer<File> { this.showImage(it) }) }
        )
        root.setOnKeyPressed {
            if (it.code==KeyCode.ENTER) {
                APP.actions.openImageFullscreen(img)
                it.consume()
            }
        }
    }

    override fun refresh() {
        thumb.loadImage(img)
    }

    @IsInput("To display")
    override fun showImage(imgFile: File) {
        img = imgFile
        thumb.loadImage(imgFile)
        root.requestFocus()
    }

}