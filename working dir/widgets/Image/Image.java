package Image;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;

import util.conf.IsConfig;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import layout.widget.controller.io.IsInput;
import layout.widget.feature.ImageDisplayFeature;
import gui.objects.image.Thumbnail;
import util.async.future.Fut;
import util.graphics.drag.DragUtil;

import static layout.widget.Widget.Group.OTHER;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static util.async.Async.FX;
import static util.graphics.Util.setAnchor;
import static util.graphics.drag.DragUtil.installDrag;

/**
 * FXML Controller class
 *
 * @author Martin Polakovic
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
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
            e -> DragUtil.hasImage(e),
            e -> img!=null && img.equals(DragUtil.getImageNoUrl(e)),
            e -> {
                Fut<File> future = DragUtil.getImage(e);
                future.use(img -> showImage(img),FX)
                      .showProgress(!future.isDone(),getWidget().getWindow()::taskAdd)
                      .run();
            }
        );
    }

    @Override
    public void refresh() {
        thumb.loadImage(img);
    }

    @Override
    @IsInput("To display")
    public void showImage(File img_file) {
        thumb.loadImage(img_file);
        img = img_file==null ? new File("") : img_file;
    }

}
