package Image;

import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.ImageDisplayFeature;
import Layout.Widgets.Widget;
import java.io.File;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.MIDDLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.layout.AnchorPane;
import javafx.util.Duration;
import utilities.FxTimer;
import utilities.ImageFileFormat;
import utilities.access.Accessor;

/**
 * FXML Controller class
 *
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Image",
    description = "Shows an image associaed with the skin.",
    howto = "Available actions:\n" +
            "    Left click left side: Previous image\n" +
            "    Left click right side : Next image\n" +
            "    Middle click : Toggle image source - custom/skin\n" +
            "    Drag & drop image : Set custom image",
    notes = "Note: Some skins may have no associated image, while some may have many.",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ImageController extends FXMLController implements ImageDisplayFeature {
    
    // non configurables
    @FXML AnchorPane root;
    private final Thumbnail thumb = new Thumbnail();
    private final ObservableList<File> images = FXCollections.observableArrayList();
    private int active_image = -1;
    private FxTimer slideshow;
    
    // auto applied configurables
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public final Accessor<Boolean> slideshow_on = new Accessor<>(true,  v -> {
        if (v) slideshowStart(); else slideshowEnd();
    });
    @IsConfig(name = "Use custom image", info = "Display custom static image file.")
    public final Accessor<Boolean> useCustomImage = new Accessor<>(false, this::useCustomImage);
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public final Accessor<Double> slideshow_dur = new Accessor<>(15000d, this::slideshowDur);   
    @IsConfig(name = "Alignment", info = "Preferred image alignment.")
    public final Accessor<Pos> align = new Accessor<>(CENTER, thumb::applyAlignment);   
    
    // non applied configurables
    @IsConfig(name = "Custom image", info = "Custom static image file.", editable = false)
    public File custom_image = new File("");

    
    @Override
    public void init() {
        thumb.setBackgroundVisible(false);
        thumb.setBorderVisible(false);
        thumb.setDragImage(false);
        thumb.getPane().setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getX() < 0.5*thumb.getPane().getWidth()) prevImage();
                else nextImage();
                e.consume();
            } else
            if(e.getButton()==MIDDLE) {
                useCustomImage.setCycledNapplyValue();
                e.consume();
            }
        });
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
                // grab images
                DragUtil.doWithImageItems(e, imgs -> {
                    if(!imgs.isEmpty())
                        showImage(imgs.get(0));
                });
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
        });
    }

    @Override
    public void close() {
        slideshow.stop();
        images.clear();
    }
    
/******************************** PUBLIC API **********************************/
    
    @Override
    public void refresh() {
        useCustomImage.applyValue();
        slideshow_on.applyValue();
        align.applyValue();
    }
    
    public void nextImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else { 
            int index = (active_image >= images.size()-1) ? 0 : active_image+1;
            setImage(index);
        }
        // restart slideshow timer
        if(slideshow!=null && slideshow_on.getValue()) slideshow.restart();
    }
    
    public void prevImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image < 1) ? images.size()-1 : active_image-1;
            setImage(index);
        }
        // restart slideshow timer
        if(slideshow!=null && slideshow_on.getValue()) slideshow.restart();
    }

    @Override
    public void showImage(File img_file) {
        // set custom image to first available
        custom_image = img_file;
        // use custom image
        useCustomImage.setNapplyValue(true);
    }
    
/******************************* HELPER METHODS *******************************/
    
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
    
    private void useCustomImage(boolean val) {
        // change data
        if(val && ImageFileFormat.isSupported(custom_image))
            images.setAll(custom_image);
        else 
            images.setAll(GUI.GUI.getGuiImages());
        // reload image
        setImage(0);
    }
    
    private void slideshowDur(double v) {
        if(slideshow != null && slideshow_on.getValue())
            slideshow.restart(Duration.millis(v));
    }
    
    private void slideshowStart() {
        nextImage();
        // create if needed
        if(slideshow==null)
            slideshow = FxTimer.createPeriodic(Duration.ZERO,this::nextImage);
        // start up
        slideshow.restart(Duration.millis(slideshow_dur.getValue()));
    }
    
    private void slideshowEnd() {
        if (slideshow!=null) slideshow.stop();
    }
    
}
