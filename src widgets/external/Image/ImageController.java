
package Image;

import Configuration.IsConfig;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.FxTimer;
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
            "    Right click : Previous image\n",
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
    }

    @Override
    public void refresh() {
        // grab fresh images
        images.setAll(GUI.GUI.getGuiImages());
        // set slideshow on/off according to state
        if (slideshow_on) slideshowStart();
        else slideshowEnd();
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
            int index = (active_image > images.size()-2) ? 0 : active_image+1;
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
