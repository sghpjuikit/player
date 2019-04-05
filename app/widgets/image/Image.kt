package image

import javafx.scene.input.KeyCode.ENTER
import javafx.scene.input.KeyCode.SPACE
import javafx.scene.input.KeyEvent.KEY_PRESSED
import sp.it.pl.gui.objects.image.Thumbnail
import sp.it.pl.layout.widget.Widget
import sp.it.pl.layout.widget.Widget.Group.OTHER
import sp.it.pl.layout.widget.controller.SimpleController
import sp.it.pl.layout.widget.controller.io.IsInput
import sp.it.pl.layout.widget.feature.ImageDisplayFeature
import sp.it.pl.main.APP
import sp.it.pl.main.IconMD
import sp.it.pl.main.getImageFile
import sp.it.pl.main.getImageFileOrUrl
import sp.it.pl.main.hasImageFileOrUrl
import sp.it.pl.main.installDrag
import sp.it.pl.main.scaleEM
import sp.it.pl.util.conf.IsConfig
import sp.it.pl.util.conf.cn
import sp.it.pl.util.conf.cv
import sp.it.pl.util.conf.only
import sp.it.pl.util.reactive.on
import sp.it.pl.util.reactive.onEventDown
import sp.it.pl.util.reactive.sync1IfInScene
import sp.it.pl.util.reactive.syncFrom
import sp.it.pl.util.ui.image.FitFrom
import sp.it.pl.util.ui.lay
import sp.it.pl.util.ui.prefSize
import sp.it.pl.util.ui.x
import sp.it.pl.util.validation.Constraint.FileActor.FILE
import java.io.File

@Widget.Info(
        author = "Martin Polakovic",
        name = "Image",
        description = "Shows a static image",
        howto = "Available actions:\n    Drag & drop image : Set custom image",
        version = "1.0.0",
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
        root.prefSize = 400.scaleEM() x 400.scaleEM()

        thumb.isBackgroundVisible = false
        thumb.borderVisible = false
        thumb.isDragEnabled = true
        thumb.fitFrom syncFrom fitFrom on onClose
        root.lay += thumb.pane

        installDrag(
                root, IconMD.DETAILS, "Display",
                { e -> e.dragboard.hasImageFileOrUrl() },
                { e -> img!=null && img==e.dragboard.getImageFile() },
                { e -> e.dragboard.getImageFileOrUrl() ui { showImage(it) } }
        )
        root.onEventDown(KEY_PRESSED, ENTER) { img?.let { APP.actions.openImageFullscreen(it) } }
        root.onEventDown(KEY_PRESSED, SPACE) { fitFrom.setNextValue() }

        showImage(img)
    }

    @IsInput("To display")
    override fun showImage(imgFile: File?) {
        img = imgFile

        onClose += root.sync1IfInScene {
            thumb.loadImage(imgFile)
            root.requestFocus()
        }
    }

}