package image;

import java.io.File;
import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.gui.objects.window.stage.Window;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.FXMLController;
import sp.it.pl.layout.widget.controller.io.IsInput;
import sp.it.pl.layout.widget.feature.ImageDisplayFeature;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.validation.Constraint;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static sp.it.pl.layout.widget.Widget.Group.OTHER;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.graphics.Util.setAnchor;
import static sp.it.pl.util.graphics.drag.DragUtil.installDrag;
import static sp.it.pl.util.validation.Constraint.FileActor.FILE;

/**
 * FXML Controller class
 */
@Widget.Info(
    author = "Martin Polakovic",
    name = "Image",
    description = "Shows a static image",
    howto = "Available actions:\n" +
            "    Drag & drop image : Set custom image",
    notes = "",
    version = "1.0",
    year = "2015",
    group = OTHER
)
public class Image extends FXMLController implements ImageDisplayFeature {

    @FXML AnchorPane root;
    private final Thumbnail thumb = new Thumbnail();

	@Constraint.FileType(FILE)
    @IsConfig(name = "Custom image", info = "Image file to display.")
    private File img = new File("");

    @Override
    public void init() {
        thumb.setBackgroundVisible(false);
        thumb.setBorderVisible(false);
        thumb.setDragEnabled(true);
        setAnchor(root,thumb.getPane(),0d);

        // drag&drop
        installDrag(
            root, DETAILS,"Display",
            DragUtil::hasImage,
            e -> img!=null && img.equals(DragUtil.getImageNoUrl(e)),
            e -> DragUtil.getImage(e)
                    .use(FX, this::showImage)
                    .showProgress(getWidget().getWindowOrActive().map(Window::taskAdd))
        );
    }

    @Override
    public void refresh() {
        thumb.loadImage(img);
    }

    @Override
    @IsInput("To display")
    public void showImage(File imgFile) {
        thumb.loadImage(imgFile);
        img = imgFile ==null ? new File("") : imgFile;
    }

}