/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import Configuration.IsConfig;
import GUI.ContextManager;
import GUI.Traits.ScaleOnHoverTrait;
import PseudoObjects.TODO;
import java.io.File;
import java.io.IOException;
import javafx.animation.FadeTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import utilities.Log;
import utilities.Util;

/**
 * Thumbnail.
 * (Not necessarily) small preview of the image. Custom component with borders
 * and mouse over animation. Can stand well on its own too as its visually
 * complete functionally standalone component with no necessary setting up.
 * 
 * Thumbnail is resizable component wrapping image.
 * 
 * Thumbnails initialized with File instead of Image object have
 * additional functionalities (context menu). It is recommended to use file
 * to pass an image into the thumbnail object, when possible.
 */
@TODO("add picture stick from outside/inside for keep ratio=true case")
public final class Thumbnail extends ImageElement implements ScaleOnHoverTrait {
    
    @IsConfig(name="Thumbnail size", info = "Preffered size for thumbnails.")
    public static double default_Thumbnail_Size = 50;
    @IsConfig(name="Thumbnail anim duration", info = "Preffered hover scale animation duration for thumbnails.")
    public static double animDur = 200;
    public static boolean animated = false;
    
    private AnchorPane THIS = new AnchorPane();
    @FXML
    ImageView image;
    @FXML
    Rectangle border;
    
    /**
     * Optional file representing the image. Unneeded, but recommended. Its
     * needed to achieve context menu functionality that allows manipulation with
     * the image file.
     * More/other file-related functionalities could be supported in the future.
     */
    File img_file;
    
    /** Constructor. 
     * Use if you need  default thumbnail size and the image is expected to
     * change during life cycle.
     */
    public Thumbnail() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Thumbnail.fxml"));
        fxmlLoader.setRoot(THIS);
        fxmlLoader.setController(this);
        try {
            THIS = (AnchorPane) fxmlLoader.load();
            initialize(default_Thumbnail_Size);
        } catch (IOException exception) {
            Log.err("Thumbnail source data coudlnt be read.");
        }
    }
    
    /**
     * Constructor.
     * Use when you want to use default sized thumbnail no post-initial changes
     * of the image are expected. In other words situations, where thumbnail object
     * is 'immutable' create-destroy type.
     * @param img 
     */
    public Thumbnail(Image img) {
        this();
        loadImage(img);
    }
    
    /**
     * Constructor.
     * Use if you need different size than default thumbnail size and the image
     * is expected to change during life cycle.
     * @param _size 
     */
    public Thumbnail (double _size) {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("Thumbnail.fxml"));
        fxmlLoader.setRoot(THIS);
        fxmlLoader.setController(this);
        try {
            THIS = (AnchorPane) fxmlLoader.load();
            initialize(_size);
        } catch (IOException exception) {
            Log.err("Thumbnail source data coudlnt be read.");
        }        
    }    
    
    /**
     * Constructor.
     * Use to create more specific thumbnail object from the get go. */
    public Thumbnail (Image img, double size) {
        this(size);
        loadImage(img);
    }
    
    private void initialize(double size) {
        // initialize values
        image.setCache(false);
        image.setSmooth(false);
        image.setPreserveRatio(true);
//        image.setCacheHint(CacheHint.SPEED);
        
        // animations
        border.setOpacity(0.3);
        installScaleOnHover();
        
        final FadeTransition fadeIn = new FadeTransition(Duration.millis(animDur), border);
            fadeIn.setFromValue(0.3);
            fadeIn.setToValue(0.8);
        final FadeTransition fadeOut = new FadeTransition(Duration.millis(animDur), border);
            fadeOut.setFromValue(0.8);
            fadeOut.setToValue(0.3);
            
        THIS.setOnMouseEntered((MouseEvent t) -> {layout();
            if (animated)
                fadeIn.play();
        });            
        THIS.setOnMouseExited((MouseEvent t) -> {
            if (animated)
                fadeOut.play();
        });
        THIS.addEventHandler(MouseEvent.MOUSE_CLICKED, (MouseEvent event) -> {
            if (event.getButton() == MouseButton.SECONDARY)
                if (img_file != null)
                    ContextManager.showMenu(ContextManager.imageFileMenu,img_file);
                else if (getImage() != null)
                    ContextManager.showMenu(ContextManager.imageFileMenu,getImage());
        });
        
        // initialize layout values
        THIS.setPrefSize(size,size);
        image.relocate(0, 0);
        image.setFitHeight(size); // do not remove this line
        image.setFitWidth(size);  // do not remove this line, the bnding doesnt initialize the sizes!
        image.fitHeightProperty().bind(THIS.prefHeightProperty());
        image.fitWidthProperty().bind(THIS.prefWidthProperty());
        
        
        // update ratios
        ratioALL.bind(THIS.prefWidthProperty().divide(THIS.prefHeightProperty()));
        image.imageProperty().addListener((ObservableValue<? extends Image> arg0, Image arg1, Image arg2) -> {
            if (!hasImage()) ratioIMG.set(1);
            else ratioIMG.set(getImage().getWidth()/getImage().getHeight());
            layout();
        });
        
        // relayout on property change
        borderToImageProperty().addListener(l->layout());
        
        
        layout();
    }
    
 /******************************************************************************/
    
    @Override
    public void loadImage(Image img) {
        img_file = null; // removing this could produce nasty bug
        image.setImage(img);
    }
    @Override
    public void loadImage(File img) {
        img_file = img;
        image.setImage(Util.loadImage(img, calculateImageLoadSize(THIS)));
    }
    @Override
    public void setFile(File img) {
        img_file = img;
    }
    @Override
    public Image getImage() {
        return image.getImage();
    }
    public boolean hasImage() {
        return getImage() != null;
    }
    @Override
    protected ImageView getImageView() {
        return image;
    }
    
    @Override
    public AnchorPane getPane() {
        return THIS;
    }
    
    /** @return border */
    public Rectangle getBorder() {
        return border;
    }
    
/***************************  Implemented Traits  *****************************/
    
    // --- scale on hover
    @Override public ObjectProperty<Duration> DurationOnHoverProperty() {
        return durationOnHover;
    }
    @Override public BooleanProperty hoverableProperty() {
        return hoverable;
    }
    private final ObjectProperty<Duration> durationOnHover = new SimpleObjectProperty<>(this, "durationOnHover", Duration.millis(animDur));
    private final BooleanProperty hoverable = new SimpleBooleanProperty(this, "hoverable", false);
    
    @Override public Node getNode() {
        return THIS;
    }
    
/********************************  Layouting  *********************************/
    
    /** Whether border frames thumbnail as a whole or picture specifically.
     * This is important for when the picture doesnt have the same aspect ratio
     * as the thumbnail. Setting the border for thumbnail (false) will frame
     * the this thumbnail without repsect for image. Conversely setting the border
     * to image (true) will frame image itself, but the thumbnail itself will not
     * be resized to image size therefore leaving empty space either horizontaly
     * or vertically.
     */
    BooleanProperty borderToImage = new SimpleBooleanProperty(false);
    
    /** Returns {@link borderToImage} property */
    public BooleanProperty borderToImageProperty() {
        return borderToImage;
    }
    /** Returns value of {@link borderToImage}. */
    public boolean getBorderToImage() {
        return borderToImage.get();
    }
    /** Sets the {@link borderToImage}. */
    public void setBorderToImage(boolean val) {
        borderToImage.set(val);
    }
    
    private void layout() {
        //System.out.println(getRatioPane() + " " + ratioIMG.get()); // debug
        
        // unbind
        border.layoutXProperty().unbind();
        border.layoutYProperty().unbind();
        border.heightProperty().unbind();
        border.widthProperty().unbind();
        
        // bind
        if (getBorderToImage()) {
            border.layoutXProperty().bindBidirectional(image.layoutXProperty());
            border.layoutYProperty().bindBidirectional(image.layoutYProperty());
            if (getRatioPane()<getRatioImg()) {
                border.heightProperty().bind(image.fitWidthProperty().divide(ratioIMG));
                border.widthProperty().bind(image.fitWidthProperty());
            } else {
                border.widthProperty().bind(image.fitHeightProperty().multiply(ratioIMG));
                border.heightProperty().bind(image.fitHeightProperty());
            }
        } else {
            border.relocate(0, 0);
            border.heightProperty().bind(THIS.prefHeightProperty());
            border.widthProperty().bind(THIS.prefWidthProperty());
        }
    }
    
/******************************************************************************/
    
    private final DoubleProperty ratioIMG = new SimpleDoubleProperty(1);
    private final DoubleProperty ratioALL = new SimpleDoubleProperty(1);
    
    public double getRatioPane() {
        return ratioALL.get();
    }
    public double getRatioImg() {
        return ratioIMG.get();
    }
    public DoubleProperty ratioPaneProperty() {
        return ratioALL;
    }
    public DoubleProperty ratioImgProperty() {
        return ratioIMG;
    }
}