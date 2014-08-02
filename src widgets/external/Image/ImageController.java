
package Image;

import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import java.io.File;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.MIDDLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.FxTimer;
import utilities.ImageFileFormat;
import utilities.functional.functor.Procedure;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
@WidgetInfo(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Image",
    description = "Shows an image associaed with the skin.",
    howto = "Available actions:\n" +
            "    Left click : Next image\n" +
            "    Right click : Previous image\n" +
            "    Middle click : Show skin images if custom image\n" +
            "    Drag & drop image : Show given custom image",
    notes = "Note: Some skins might not have any associated image, while some"
          + "will have many.",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ImageController extends FXMLController {
    @FXML AnchorPane root;
    Thumbnail thumb;
    final ObservableList<File> images = FXCollections.observableArrayList();
    
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public boolean slideshow_on = true;
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public double slideshow_dur = 15000l;
    // invisible for now
    // 1 - we do not have a good image picker
    // 2 - we need to make it possible to pick 'empty' image or null
    @IsConfig(name = "Custom image", info = "Display custom static image file.", visible = false)
    public File custom_image = new File("");
    
    
    @Override
    public void init() {
        thumb = new Thumbnail();
        thumb.setBackgroundVisible(false);
        thumb.setBorderVisible(false);
        thumb.setDragImage(false);
        thumb.allowContextMenu(false);
        thumb.getPane().setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                nextImage.run();
                e.consume();
            } else
            if(e.getButton()==SECONDARY) {
                prevImage.run();
                e.consume();
            } else
            if(e.getButton()==MIDDLE) {
                custom_image = new File("");
                refresh();
                e.consume();
            }
        });
        root.getChildren().add(thumb.getPane());
        // this currenttly causes thumbnail image not to rezie properly because
        // it is bound to prefSize which is not changed by anchors
//        AnchorPane.setBottomAnchor(thumb.getPane(), 0.0);
//        AnchorPane.setTopAnchor(thumb.getPane(), 0.0);
//        AnchorPane.setLeftAnchor(thumb.getPane(), 0.0);
//        AnchorPane.setRightAnchor(thumb.getPane(), 0.0);
        // bind manually for now so image resizes properly
        thumb.getPane().prefWidthProperty().bind(root.widthProperty());
        thumb.getPane().prefHeightProperty().bind(root.heightProperty());
        
        
        root.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        root.setOnDragDropped( e -> {
            List<File> images = DragUtil.getImageItems(e);
            if(!images.isEmpty()) custom_image = images.get(0);
            refresh();
        });
    }

    @Override
    public void refresh() {
        // grab fresh images
        if(ImageFileFormat.isSupported(custom_image)) images.setAll(custom_image);
        else images.setAll(GUI.GUI.getGuiImages());
        // set slideshow on/off according to state
        if (slideshow_on) slideshowStart();
        else slideshowEnd();
        setImage(0);
    }

    @Override
    public void OnClosing() {
        slideshow.stop();
        images.clear();
    }
    
    private void setImage(int index) {
        if (images.isEmpty()) index = -1;
        if (index >= images.size()) index = images.size()-1;
        
        if (index == -1) {
            Image i = null;
            thumb.loadImage(i);
            active_image = -1;
        } else {
            thumb.loadImage(images.get(index));
            active_image = index;
        }
    }
    
/********************************** SLIDESHOW *********************************/
    
    private int active_image = -1;
    private final Procedure nextImage = () -> {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else { 
            int index = (active_image >= images.size()-1) ? 0 : active_image+1;
            setImage(index);
        }
    };
    private final Procedure prevImage = () -> {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image < 1) ? images.size()-1 : active_image-1;
            setImage(index);
        }
    };
    FxTimer slideshow = FxTimer.createPeriodic(Duration.millis(slideshow_dur),nextImage);
    
    public void slideshowStart() {
        nextImage.run();
        slideshow.restart(Duration.millis(slideshow_dur));
    }
    
    public void slideshowEnd() {
        slideshow.stop();
    }
    
    
}
