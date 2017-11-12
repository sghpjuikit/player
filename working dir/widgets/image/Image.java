package image;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

import gui.objects.image.Thumbnail;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import layout.widget.controller.io.IsInput;
import layout.widget.feature.ImageDisplayFeature;
import util.async.future.Fut;
import util.conf.IsConfig;
import util.graphics.drag.DragUtil;
import util.validation.Constraint;

import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static layout.widget.Widget.Group.OTHER;
import static util.async.AsyncKt.FX;
import static util.validation.Constraint.FileActor.FILE;
import static util.graphics.Util.setAnchor;
import static util.graphics.drag.DragUtil.installDrag;

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
            e -> {
                Fut<File> future = DragUtil.getImage(e);
                future.use(FX, this::showImage)
                      .showProgress(!future.isDone(), getWidget().getWindow()::taskAdd);
            }
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