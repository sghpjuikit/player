package Image;

import java.io.File;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.layout.AnchorPane;

import Configuration.IsConfig;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.controller.io.IsInput;
import Layout.Widgets.feature.ImageDisplayFeature;
import gui.objects.image.Thumbnail;
import main.App;
import util.access.Var;
import util.graphics.drag.DragUtil;

import static Layout.Widgets.Widget.Group.OTHER;
import static javafx.geometry.Pos.CENTER;
import static util.async.Async.FX;

/**
 * FXML Controller class
 *
 * @author Plutonium_
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
public class ImageController extends FXMLController implements ImageDisplayFeature {
    
    @FXML AnchorPane root;
    private final Thumbnail thumb = new Thumbnail();
    
    @IsConfig(name = "Alignment", info = "Preferred image alignment.")
    public final Var<Pos> align = new Var<>(CENTER, thumb::applyAlignment);   
    @IsConfig(name = "Custom image", info = "Image file to display.")
    private File img = new File("");

    
    @Override
    public void init() {
        thumb.setBackgroundVisible(false);
        thumb.setBorderVisible(false);
        thumb.setDragEnabled(false);
        root.getChildren().add(thumb.getPane());
        // this currenttly causes thumbnail image not to rezie properly because
        // it is bound to prefSize which is not changed by anchors
        // Util.setAPAnchors(thumb.getPane(), 0);
        // bind manually for now so image resizes properly
        thumb.getPane().prefWidthProperty().bind(root.widthProperty());
        thumb.getPane().prefHeightProperty().bind(root.heightProperty());
        
        
        root.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        root.setOnDragDropped( e -> {
            if(DragUtil.hasImage(e.getDragboard())) {
                DragUtil.getImages(e)
                     .use(imgs -> {
                        if(!imgs.isEmpty()) showImage(imgs.get(0));
                     },FX)
                     .showProgress(App.getWindow().taskAdd())
                     .run();
                e.setDropCompleted(true);
                e.consume();
            }
        });
    }
    
    @Override
    public void refresh() {
        align.applyValue();
        thumb.loadImage(img);
    }

    @Override
    @IsInput("To display")
    public void showImage(File img_file) {
        thumb.loadImage(img_file);
        img = img_file==null ? new File("") : img_file;
    }
    
}
