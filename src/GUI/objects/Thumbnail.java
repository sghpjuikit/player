/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package GUI.objects;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import GUI.Traits.ScaleOnHoverTrait;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.DataFormat;
import static javafx.scene.input.DataFormat.FILES;
import javafx.scene.input.Dragboard;
import static javafx.scene.input.MouseButton.MIDDLE;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.input.MouseEvent;
import static javafx.scene.input.MouseEvent.DRAG_DETECTED;
import static javafx.scene.input.MouseEvent.MOUSE_CLICKED;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import main.App;
import utilities.Enviroment;
import utilities.FileUtil;
import utilities.ImageFileFormat;
import utilities.Log;
import utilities.TODO;
import utilities.Util;

/**
 * Thumbnail.
 * (Not necessarily) small image component. Supports resizing operarion and
 * has several added funcionalities like border, background or  mouse over
 * animations.
 * <p>
 * Thumbnail's background and border can be fully styled with css. Additionally,
 * border can be set to frame thumbnail or picture respectively.
 * <p>
 * Image is always positioned inside the thumbnail and aligned to center both
 * vertically and horizontally.
 * <p>
 * In order to save memory, thumbnail will load the image with load scale
 * factor determining the load size. For details see {@link #LOAD_COEFICIENT}
 * and {@link #calculateImageLoadSize()}.
 * <p>
 * Thumbnails initialized with File instead of Image object have
 * additional functionalities (context menu). It is recommended to use file
 * to pass an image into the thumbnail object, when possible.
 */
@TODO("add picture stick from outside/inside for keep ratio=true case")
@IsConfigurable
public final class Thumbnail extends ImageNode implements ScaleOnHoverTrait {
    
    // styleclasses
    public static final String bgr_styleclass = "thumbnail-bgr";
    public static final String border_styleclass = "thumbnail-border";
    
    // global propertiea
    @IsConfig(name="Thumbnail size", info = "Preffered size for thumbnails.")
    public static double default_Thumbnail_Size = 60;
    @IsConfig(name="Thumbnail anim duration", info = "Preffered hover scale animation duration for thumbnails.")
    public static double animDur = 200;
    public static boolean animated = false;
    
    private AnchorPane root = new AnchorPane();
    @FXML ImageView image;
    @FXML StackPane img_container;
    @FXML Pane img_border;
    
    /**
     * Optional file representing the image. Not needed, but recommended. Its
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
        this(default_Thumbnail_Size);
    }
    
    /**
     * Constructor.
     * Use when you want to use default sized thumbnail no post-initial changes
     * of the image are expected. In other words situations, where thumbnail object
     * is viewed as immutable create-destroy type.
     * @param img 
     */
    public Thumbnail(Image img) {
        this(default_Thumbnail_Size);
        loadImage(img);
    }
    
    /**
     * Constructor.
     * Use if you need different size than default thumbnail size and the image
     * is expected to change during life cycle.
     * @param _size 
     */
    public Thumbnail (double _size) {
        FXMLLoader fxmlLoader = new FXMLLoader(Thumbnail.class.getResource("Thumbnail.fxml"));
        fxmlLoader.setRoot(root);
        fxmlLoader.setController(this);
        try {
            root = (AnchorPane) fxmlLoader.load();
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
        setSmooth(true);
        setPreserveRatio(true);
//        image.setCacheHint(CacheHint.SPEED);
        borderToImage(false);
        setBackgroundVisible(true);
        setDragImage(true);
        
        // animations
        installScaleOnHover();
        
        // change border framing style on mouse middle button click //experimental
        root.addEventFilter(MOUSE_CLICKED, e -> {
            if(e.getButton()==MIDDLE)
                setBorderToImage(!isBorderToImage());
            
            // close popup if open
            if(imgCM!=null && imgCM.isShowing()) imgCM.hide();
            if(fileCM!=null && fileCM.isShowing()) fileCM.hide();
            
        });
        
        allowContextMenu(true);
        
        size-=borderWidth();
        // set size
        root.setPrefSize(size,size);
        // initialize image size
        image.setFitHeight(size);
        image.setFitWidth(size);
        // bind image sizes to size
        image.fitHeightProperty().bind(Bindings.min(root.prefHeightProperty(), maxIMGH));
        image.fitWidthProperty().bind(Bindings.min(root.prefWidthProperty(), maxIMGW));

        
        // update ratios
        ratioALL.bind(root.prefWidthProperty().divide(root.prefHeightProperty()));
        image.imageProperty().addListener((o,ov,nv) ->
            ratioIMG.set( nv==null ? 1 : nv.getWidth()/nv.getHeight())
        );
        // keep image border size in line with image size bind pref,max size
        ratioIMG.greaterThan(ratioALL).addListener(border_sizer);
    }
    
 /******************************************************************************/
    
    @Override
    public void loadImage(Image img) {
        img_file = null;    // removing this could produce nasty bug
        image.setImage(img);
        border_sizer.changed(null, false, ratioIMG.get()>ratioALL.get());
        if(img!=null) {
            maxIMGH.set(img.getHeight()*maxScaleFactor);
            maxIMGW.set(img.getWidth()*maxScaleFactor);
        }
    }
    @Override
    public void loadImage(File img) {
        img_file = img;
        Point2D size = calculateImageLoadSize(root);
        Image i = Util.loadImage(img_file, size.getX(), size.getY());
        image.setImage(i);
        border_sizer.changed(null, false, ratioIMG.get()>ratioALL.get());
        if(i!=null) {
            maxIMGH.set(i.getHeight()*maxScaleFactor);
            maxIMGW.set(i.getWidth()*maxScaleFactor);
        }
    }
    @Override
    public void setFile(File img) {
        img_file = img;
    }

    @Override
    public File getFile() {
        return img_file;
    }
    
    @Override
    public Image getImage() {
        return image.getImage();
    }
    public boolean isEmpty() {
        return getImage() == null;
    }
    @Override
    protected ImageView getView() {
        return image;
    }
    
    /**
     * Note that in order for the image to resize properly prefSize of this pane
     * must be changed!
     * {@inheritDoc }
     * @return 
     */
    @Override
    public AnchorPane getPane() {
        return root;
    }
    
/*******************************  properties  *********************************/
    
    private double maxScaleFactor = 1.3;
    
    /**
     * Sets maximum allowed scaling factor for the image. 
     * <p>
     * The image in the thumbnail scales with it, but only up to its own maximal
     * size defined by:    imageSize * maximumScaleFactor
     * <p>
     * 
     * Default value is 1.3.
     * <p>
     * Note that original size in this context means size (width and height) the
     * image has been loaded with. The image can be loaded with any size, even
     * surpassing that of the resolution of the file.
     * 
     * @see #calculateImageLoadSize()
     * @throws IllegalArgumentException if parameter < 1
     */
    public void setMaxScaleFactor(double val) {
        if(val < 1) throw new IllegalArgumentException("Scale factor < 1 not allowed.");
        maxScaleFactor = val;
    }
    
    /** 
     * Whether border envelops thumbnail or image specifically.
     * This is important for when the picture doesnt have the same aspect ratio
     * as the thumbnail. Setting the border for thumbnail (false) will frame
     * the thumbnail without respect for image size. Conversely, setting the border
     * to image (true) will frame image itself, but the thumbnail itself will not
     * be resized to image size, therefore leaving empty space either horizontally
     * or vertically.
     */
    private boolean borderToImage = false;
    
    /** Returns value of {@link #borderToImage}. */
    public boolean isBorderToImage() {
        return borderToImage;
    }
    
    /** Sets the {@link #borderToImage}. */
    public void setBorderToImage(boolean val) {
        if(borderToImage==val) return;
        borderToImage(val);
    }
    
    /**
     * Set visibility of the border. Default true.
     * @param val 
     */
    private void borderToImage(boolean val) {
        double b = borderWidth();
        borderToImage = val;
        if(val) {
            root.getStyleClass().remove(border_styleclass);
            img_border.getStyleClass().add(border_styleclass);

        } else {
            img_border.getStyleClass().remove(border_styleclass);
            root.getStyleClass().add(border_styleclass);
        }
    }
    
    /** 
     * Sets visibility of the background. The bgr is visible only when the image
     * size ratio and thumbnail size ratio does not match.
     * Default value is true. Invisible background becomes transparent.
     * Stylizable with css.
     */
    public void setBackgroundVisible(boolean val) {
        if(val) root.getStyleClass().add(bgr_styleclass);
        else root.getStyleClass().remove(bgr_styleclass);
    }

    public void setBorderVisible(boolean val) {
        if(val) {
            borderToImage(borderToImage);
        } else {
            root.getStyleClass().remove(border_styleclass);
            img_border.getStyleClass().remove(border_styleclass);
        }
    }
    
    private double borderWidth() {
        return 4;
    }
    
    /**
     * Set support for dragging file representing the displayed image. The file
     * can be dragged and dropped anywhere within the application.
     * Default true.
     * @param val 
     */
    public void setDragImage(boolean val) {
        if(val) {
            dragHandler = buildDragHandler();
            root.addEventHandler(DRAG_DETECTED, dragHandler);
        } else {
            root.removeEventHandler(DRAG_DETECTED, dragHandler);
            dragHandler = null;
        }
    }
    
    /**
     * Set whether thumbnail context menu should be used for thumbnail.
     * Default true.
     */
    public void allowContextMenu(boolean val) {
        if (val) root.setOnContextMenuRequested(contextMenuRequestHandler);
        else root.setOnContextMenuRequested(null);
    }
    
    private EventHandler<MouseEvent> dragHandler;
    
    private EventHandler<MouseEvent> buildDragHandler() {
        return e -> {
            if(e.getButton()==PRIMARY && img_file!=null) {
                Dragboard db = root.startDragAndDrop(TransferMode.LINK);
                // set image
                if(getImage()!=null) db.setDragView(getImage());
                // set content
                HashMap<DataFormat,Object> c = new HashMap();
                c.put(FILES, Collections.singletonList(img_file));
                db.setContent(c);
            }
        };
    }
    
/***************************  Implemented Traits  *****************************/
    
    // --- scale on hover
    @Override public ObjectProperty<Duration> DurationOnHoverProperty() {
        return durationOnHover;
    }
    @Override public BooleanProperty hoverableProperty() {
        return hoverable;
    }
    private final ObjectProperty<Duration> durationOnHover = new SimpleObjectProperty(this, "durationOnHover", Duration.millis(animDur));
    private final BooleanProperty hoverable = new SimpleBooleanProperty(this, "hoverable", false);
    
    @Override public Node getNode() {
        return root;
    }
    
/******************************************************************************/
    
    private final DoubleProperty ratioIMG = new SimpleDoubleProperty(1);
    private final DoubleProperty ratioALL = new SimpleDoubleProperty(1);
    private final DoubleProperty maxIMGW = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final DoubleProperty maxIMGH = new SimpleDoubleProperty(Double.MAX_VALUE);
    
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
    
    private final ChangeListener<Boolean> border_sizer = (o,ov,nv)-> {
        if(nv) {
            img_border.prefHeightProperty().unbind();
            img_border.prefWidthProperty().unbind();
            img_border.prefHeightProperty().bind(image.fitWidthProperty().divide(ratioIMG));
            img_border.prefWidthProperty().bind(image.fitWidthProperty());
            img_border.maxHeightProperty().unbind();
            img_border.maxWidthProperty().unbind();
            img_border.maxHeightProperty().bind(image.fitWidthProperty().divide(ratioIMG));
            img_border.maxWidthProperty().bind(image.fitWidthProperty());
        } else {
            img_border.prefHeightProperty().unbind();
            img_border.prefWidthProperty().unbind();
            img_border.prefWidthProperty().bind(image.fitHeightProperty().multiply(ratioIMG));
            img_border.prefHeightProperty().bind(image.fitHeightProperty());
            img_border.maxHeightProperty().unbind();
            img_border.maxWidthProperty().unbind();
            img_border.maxWidthProperty().bind(image.fitHeightProperty().multiply(ratioIMG));
            img_border.maxHeightProperty().bind(image.fitHeightProperty());
        }
    };
    

/******************************************************************************/
    
    private ContextMenu imgCM;
    private ContextMenu fileCM;
    
    private final EventHandler<ContextMenuEvent> contextMenuRequestHandler = e -> {
        // decide mode (image vs file), build lazily & show where requested
        if (img_file != null)
            getFileCM().show(root,e.getScreenX(),e.getScreenY());
        else if (getImage() !=null)
            getImgCM().show(root,e.getScreenX(),e.getScreenY());
    };
    
    private ContextMenu getFileCM() {
        if(fileCM==null) fileCM = buildFileCM(this);
        return fileCM;
    }
    
    private ContextMenu getImgCM() {
        if(imgCM==null) imgCM = buildImageCM(this);
        return imgCM;
    }
    
    private static ContextMenu buildImageCM(Thumbnail thumb) {
        final ContextMenu contextMenu = new ContextMenu();
        

        MenuItem item1 = new MenuItem("Save the image as ...");
                 item1.setOnAction(e -> {
                    Image i = thumb.getImage();
                    FileChooser fc = new FileChooser();
                        fc.getExtensionFilters().addAll(ImageFileFormat.extensions()
                                .stream().map(ext->new ExtensionFilter( ext,ext))
                                .collect(Collectors.toList()));
                        fc.setTitle("Save image as...");
                        fc.setInitialFileName("new_image");
                        fc.setInitialDirectory(App.getLocation());
                    File f = fc.showSaveDialog(App.getWindowOwner().getStage());
                    FileUtil.writeImage(i, f);
                 });
        MenuItem item2 = new MenuItem("Copy the image to clipboard");
                 item2.setOnAction(e -> {
                    Image i = thumb.getImage();
                    if (i==null) return;
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                                     content.putImage(i);
                    clipboard.setContent(content);
                 });
                 
        contextMenu.getItems().addAll(item1, item2);
        contextMenu.setConsumeAutoHidingEvents(true);
        return contextMenu;
    }
    
    private static ContextMenu buildFileCM(Thumbnail thumb) {
        final ContextMenu contextMenu = new ContextMenu();

        MenuItem item1 = new MenuItem("Browse location");
                 item1.setOnAction(e -> {
                     if(thumb.img_file==null) return;
                     Enviroment.browse(thumb.img_file.toURI());
                 });
        MenuItem item2 = new MenuItem("Edit the image in editor");
                 item2.setOnAction(e -> {
                     if(thumb.img_file==null) return;
                     Enviroment.edit(thumb.img_file);
                 });
        MenuItem item3 = new MenuItem("Open image");
                 item3.setOnAction(e -> {
                     if(thumb.img_file==null) return;
                     Enviroment.open(thumb.img_file);
                 });
        MenuItem item4 = new MenuItem("Delete the image from disc");
                 item4.setOnAction(e -> {
                     if(thumb.img_file==null) return;
                     FileUtil.deleteFile(thumb.img_file);
                 });
        MenuItem item5 = new MenuItem("Save the image as ...");
                 item5.setOnAction(e -> {
                    File f = thumb.img_file;
                    if (f==null) return;
                    
                    FileChooser fc = new FileChooser();
                        fc.getExtensionFilters().addAll(ImageFileFormat.extensions()
                                .stream().map(ext->new ExtensionFilter(ext,ext))
                                .collect(Collectors.toList()));
                        fc.setTitle("Save image as...");
                        fc.setInitialFileName("new_image");
                        fc.setInitialDirectory(App.getLocation());
                    File newff = fc.showSaveDialog(App.getWindowOwner().getStage());
                    try {
                        Files.copy(f.toPath(), newff.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ex) {
                        Log.info("File export failed.");
                    }
                 });
                 
        contextMenu.getItems().addAll(item1, item2, item3, item4, item5);
        contextMenu.setConsumeAutoHidingEvents(true);
        return contextMenu;
    } 
}