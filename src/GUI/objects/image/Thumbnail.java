/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gui.objects.image;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;

import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.*;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Configuration.IsConfig;
import Configuration.IsConfigurable;
import Layout.widget.Widget;
import Layout.widget.feature.ImageDisplayFeature;
import gui.objects.ContextMenu.ImprovedContextMenu;
import gui.objects.Window.stage.WindowBase;
import gui.objects.image.cover.Cover;
import util.File.Environment;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.SingleR;
import util.Util;
import util.animation.Anim;
import util.dev.Dependency;
import util.dev.TODO;

import static java.lang.Double.min;
import static java.util.Collections.singletonList;
import static javafx.scene.input.DataFormat.FILES;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.*;
import static javafx.scene.layout.Region.USE_COMPUTED_SIZE;
import static javafx.scene.paint.Color.BLACK;
import static javafx.util.Duration.millis;
import static main.App.APP;
import static util.File.Environment.copyToSysClipboard;
import static util.Util.getFieldValue;
import static util.Util.menuItem;
import static util.dev.TODO.Purpose.FUNCTIONALITY;
import static util.graphics.Util.setAnchors;

/**
 * Thumbnail.
 * <p>
 * Resizable image container with additional features.
 * <p>
 * Features:
 * <ul>
 * <li> Resizable. The image resizes in layout automatically. For manual resize
 * set preferred, minimal and maximal size of the root {@link #getPane()}.
 * <p>
 * This size is applied on the root of this thubmnail, which contains the image.
 * The image will try to use maximum space available, depending on the aspect
 * ratios of the image and this thumbnail.
 * <p>
 * The image retains aspect ratio and is always fully visible inside this thumbnail.
 * If the aspect ratio of this thumbnail differs from that of the image, there
 * will be empty space left on left+right or top+bottom. Image is center - aligned.
 * <li> File aware. File (representing the image) can be set in addition
 * to the the image, which is useful for context menu
 * <li> Aspect ratio aware. See {@link #ratioTHUMB} and {@link #ratioIMG}. Used
 * for layout.
 * <li> Resolution aware. The images are loaded only up to required size to
 * reduce memory. For details see {@link ImageNode#LOAD_COEFICIENT} and
 * {@link ImageNode#calculateImageLoadSize(javafx.scene.layout.Region) }.
 * <li> Size aware. If this thumbnail is resized then the image will only resize up
 * to a certain maximum size to prevent blurry result of scaling small image
 * too much. See {@link #setMaxScaleFactor(double) }
 * <li> Drag support. Dragging the image will put the the file in the
 * clipboard (if it is available).
 * <li> Background can be used. Background is visible on the empty space resulting
 * from different aspect ratio of this thumbnail and image.
 * <p>
 * Define background style in css.
 * <li> Border. Can be set to frame whole thumbnail or image respectively. Framing
 * whole thumbnail will frame also the empty areas and background.
 * Useful for small thumbnails to give them 'rectangular' size by setting both
 * background and border framing to whole thumbnail. Big image may do the opposite
 *  - display no background and frame image only.
 * <p>
 * Define border style in css.
 * <li> Context menu. Shown on right click. Has additional menu items if the
 * file is set.
 * <p>
 * Image can be opened in native application, edited in native editor,
 * browsed in native file system, exported as file or viewed fullscreen inside
 * the application, and more.
 * </ul>
 */
@TODO(purpose = FUNCTIONALITY, note = "add picture stick from outside/inside for keep ratio=true case")
@IsConfigurable("Images")
public class Thumbnail extends ImageNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(Thumbnail.class);
    private static final String bgr_styleclass = "thumbnail";
    private static final String border_styleclass = "thumbnail-border";
    private static final String image_styleclass = "thumbnail-image";

    @IsConfig(name="Thumbnail anim duration", info = "Preffered hover scale animation duration for thumbnails.")
    public static Duration animDur = millis(100);
    public static boolean animated = false;

    protected final ImageView imageView = new ImageView();
    protected final Pane img_border = new Pane();
    protected final StackPane root = new StackPane(imageView,img_border) {
        @Override
        protected void layoutChildren() {

            // lay out image
            double W = getWidth();
            double H = getHeight();
            double imgW = min(W,maxIMGW.get());
            double imgH = min(H,maxIMGH.get());
            double imgWgap = (W-imgW)/2;
            double imgHgap = (H-imgH)/2;
            imageView.setFitWidth(imgW);
            imageView.setFitHeight(imgH);
//            imageView.resizeRelocate(imgWgap,imgHgap,imgW,imgH);
//            imageView.resize(0,0);

            // lay out other children (maybe introduced in subclass)
            super.layoutChildren();

            // lay out border
            if(borderToImage && imageView.getImage()!=null) {
                if(ratioIMG.get()>ratioTHUMB.get()) {
                    double borderW = imgW;
                    double borderWgap = (W-borderW)/2;
                    double borderH = imgW/ratioIMG.get();
                    double borderHgap = (H-borderH)/2;
                    resizeRelocateBorder(borderWgap,borderHgap,borderW,borderH);
                } else {
                    double borderW = imgH*ratioIMG.get();
                    double borderWgap = (W-borderW)/2;
                    double borderH = imgH;
                    double borderHgap = (H-borderH)/2;
                    resizeRelocateBorder(borderWgap,borderHgap,borderW,borderH);
                }
            } else {
                resizeRelocateBorder(0,0,W,H);
            }
        }
    };

    // bugfix
    // Inconsistent border width and can be blurry, and image under it can stick out (very ugly).
    // Probably problem with drawing at non-integer coordinates. See:
    // http://stackoverflow.com/questions/9779693/javafx-graphics-blurred-or-anti-aliased-no-effects-used
    // workaround: border is moved 1px outside in layoutChildren(). Not ideal, since border still
    //             isnt of the same width on all sizes and now it even sticks out of the image area.
    //             But at least its not so hideous  
    private void resizeRelocateBorder(double x, double y, double w, double h) {
        img_border.resizeRelocate(x-1,y-1,w+2,h+2);
    }

    /**
     * Displayed image. Editable, but it is recommended to use one of the load
 methods instead. Note, that those load the image on bgr thread and setting
 this property is delayed until the image fully loads. Until then this
 thumbnail will keep showing the previous image and this property will
 reflect that. Thus calling getM() on this property may not provide the
 expected result.
     */
    public final ObjectProperty<Image> image = imageView.imageProperty();

    /** Constructor.
     * Use if you need  default thumbnail size and the image is expected to
     * change during life cycle.
     */
    public Thumbnail() {
        this(USE_COMPUTED_SIZE,USE_COMPUTED_SIZE);
    }

    /**
     * Use when you want to use default sized thumbnail no post-initial changes
     * of the image are expected. In other words situations, where thumbnail object
     * is viewed as immutable create-destroy type.
     * @param img
     */
    public Thumbnail(Image img) {
        this(USE_COMPUTED_SIZE,USE_COMPUTED_SIZE);
        loadImage(img);
    }

    /** Use to create more specific thumbnail object from the getM go. */
    public Thumbnail (Image img, double size) {
        this(size,size);
        loadImage(img);
    }

    /**
     * Use if you need different size than default thumbnail size and the image
     * is expected to change during life cycle.
     * @param size
     */
    public Thumbnail (double width, double height) {

        root.setMinSize(width,height);
        root.setPrefSize(width,height);
        root.setMaxSize(width,height);
        imageView.getStyleClass().add(image_styleclass);
//        imageView.setManaged(false);
        imageView.setFitHeight(-1);
        imageView.setFitWidth(-1);
        img_border.setMouseTransparent(true);
        img_border.setManaged(false);

//        root.setSnapToPixel(false);
//        img_border.setSnapToPixel(false);

        // update ratios
        ratioTHUMB.bind(root.widthProperty().divide(root.heightProperty()));
        imageView.imageProperty().addListener((o,ov,nv) ->
            ratioIMG.set(nv==null ? 1 : nv.getWidth()/nv.getHeight())
        );

        // initialize values
//        imageView.setCache(false);
        setSmooth(true);
        setPreserveRatio(true);
        setBorderToImage(false);
        setBorderVisible(true);
        setBackgroundVisible(true);
        setDragEnabled(true);
        setContextMenuOn(true);
        root.addEventFilter(MOUSE_ENTERED, e -> animationPlay());
        root.addEventFilter(MOUSE_EXITED, e -> animationPause());
    }


    public final ObservableList<String> getStyleClass() {
        return root.getStyleClass();
    }

/******************************************************************************/

    @Override
    public void loadImage(Image img) {
        imagefile=null;
        setImgA(img);
    }
    private File imagefile = null;
    @Override
    public void loadImage(File img) {
        imagefile = img;
        Point2D size = calculateImageLoadSize(root);
        Image c = getCached(img, size.getX(), size.getY());
        Image i = c!=null ? c : Util.loadImage(img, size.getX(), size.getY());
        setImgA(i);
    }

    public void loadImage(Cover img) {
        imagefile = img==null ? null : img.getFile();
        if(img==null) {
            setImgA(null);
        } else {
            Point2D size = calculateImageLoadSize(root);
            Image i = img.getImage(size.getX(), size.getY());
            setImgA(i);
        }
    }

    private long loadId = 0;    // prevents wastful set image operatins
    private static HashMap<String,Image> IMG_CACHE = new HashMap<>();   // caches images

    public static Image getCached(String url, double w, double h) {
        Image ci = url==null ? null : IMG_CACHE.get(url);
        return ci!=null && (ci.getWidth()>=w || ci.getHeight()>=h) ? ci : null;
    }

    public static Image getCached(File file, double w, double h) {
        String url = null;
        try {
            url = file.toURI().toURL().toString();
        } catch (Exception e) {}
        return getCached(url, w, h);
    }

    // set asynchronously
    private void setImgA(Image i) {
        loadId++;   // load next image
        final long id = loadId; // expected id (must match when load finishes)
        if(i==null) {
            setImg(null,id);
        } else {
            if(i.getProgress()==1) {
                setImg(i,id);
            } else {
                i.progressProperty().addListener((o,ov,nv) -> {
                    if(nv.doubleValue()==1)
                        setImg(i,id);
                });
            }
        }
    }

    // set synchronously
    private void setImg(Image i, long id) {
        // cache
        if(i!=null && cache_images) {
            Image ci = IMG_CACHE.get(i.impl_getUrl());
            if(ci==null || ci.getHeight()*ci.getWidth()<i.getHeight()*i.getWidth())
                IMG_CACHE.put(i.impl_getUrl(), i);
        }

        // ignore outdated loadings
        if(id!=loadId) return;

        imageView.setImage(i);
        if(i!=null) {
            maxIMGW.set(i.getWidth()*maxScaleFactor);
            maxIMGH.set(i.getHeight()*maxScaleFactor);
        }
//        if(borderToImage)
            root.layout();

        // animation
        if(i!=null) {
            Object animWrapper = getFieldValue(i, Object.class, "animation");
            animation = animWrapper==null ? null : getFieldValue(animWrapper, Timeline.class, "timeline");
//            animationPause();
        }
    }

/****************************************** ANIMATION *********************************************/

    Timeline animation = null;

    public boolean isAnimated() {
        return animation!=null;
    }

    public void animationPlay() {
        if(animation!=null) animation.play();
    }

    public void animationPause() {
        if(animation!=null) animation.pause();
    }

/**************************************************************************************************/

    /** File representing the displayed image or null if no image displayed or not a file. */
    @Override
    public File getFile() {
        // Since we delay image loading or something, image.get() can be null, in that case we fall
        // back to imagefile
        String url = image.get()==null ? null : image.get().impl_getUrl();
        try {
            return url==null ? imagefile : new File(URI.create(url));
        } catch(IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public Image getImage() {
        return imageView.getImage();
    }
    public boolean isEmpty() {
        return getImage() == null;
    }
    @Override
    public ImageView getView() {
        return imageView;
    }

    /**
     * Returns root pane. Also image drag gesture source {@see #setDragEnabled(boolean)}.
     * <p>
     * Note that in order for the image to resize prefSize of this pane
     * must be used!
     * <p>
     * {@inheritDoc }
     */
    @Dependency("Must return image drag gesture root")
    @Override
    public Pane getPane() {
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
        borderToImage = val;
        root.layout();
    }

    public void setBorderVisible(boolean val) {
        if(val) img_border.getStyleClass().add(border_styleclass);
        else img_border.getStyleClass().remove(border_styleclass);
    }


    /**
     * Sets visibility of the background. The bgr is visible only when the image
     * size ratio and thumbnail size ratio does not match.
     * Default value is true. Invisible background becomes transparent.
     * Stylizable with css.
     */
    public void setBackgroundVisible(boolean val) {
        if(val) {
            if(!root.getStyleClass().contains(bgr_styleclass))
                root.getStyleClass().add(bgr_styleclass);
        }
        else root.getStyleClass().remove(bgr_styleclass);
    }


    /**
     * Allow image file drag from this thumbnail.
     * <p>
     * Dragging is done with left button and only possible if this thumbnail
     * has file set. The file will be put into dragboard, use Dataformat.FILES
     * to retrieve it.
     * <p>
     * The gesture source can be obtained by {@link #getPane()}
     * <p>
     * Default true.
     */
    public void setDragEnabled(boolean val) {
        if(val) {
            dragHandler = buildDH();
            root.addEventHandler(DRAG_DETECTED, dragHandler);
        } else {
            root.removeEventHandler(DRAG_DETECTED, dragHandler);
            dragHandler = null;
        }
    }

    private EventHandler<MouseEvent> dragHandler;
    private EventHandler<MouseEvent> buildDH() {
        return e -> {
            if(e.getButton()==PRIMARY && getFile()!=null) {
//                TransferMode t = e.isShiftDown() ? TransferMode.MOVE : e.isAltDown() ? TransferMode.
                Dragboard db = root.startDragAndDrop(TransferMode.ANY);
                // set drag image
                if(getImage()!=null) db.setDragView(getImage());
                // set content
                HashMap<DataFormat,Object> c = new HashMap();
                c.put(FILES, singletonList(getFile()));
                db.setContent(c);
                e.consume();
            }
        };
    }


    /**
     * Set whether thumbnail context menu should be used for thumbnail.
     * Default true.
     */
    public void setContextMenuOn(boolean val) {
        if (val) root.addEventHandler(MOUSE_CLICKED,contextMenuHandler);
        else root.removeEventHandler(MOUSE_CLICKED,contextMenuHandler);
    }

/********************************  HOVERING  **********************************/

    /** Duration of the scaling animation effect when transitioning to gover state. */
    public final ObjectProperty<Duration> durationOnHover = new SimpleObjectProperty(this, "durationOnHover", animDur);
    private final Anim hoverAnimation = new Anim(durationOnHover.get(),at -> util.graphics.Util.setScaleXY(root,1+0.05*at));
    private final EventHandler<MouseEvent> hoverHandler = e -> {
            hoverAnimation.dur(durationOnHover.get());
            if(isH())
                hoverAnimation.playFromDir(e.getEventType().equals(MOUSE_ENTERED));
    };
    /** Hover scaling efect on/off. Default false.*/
    public final BooleanProperty hoverable = new SimpleBooleanProperty(this, "hoverable", false) {
        @Override public void set(boolean v) {
            super.set(v);
            if(v) {
                root.addEventFilter(MOUSE_ENTERED, hoverHandler);
                root.addEventFilter(MOUSE_EXITED, hoverHandler);
            } else {
                root.removeEventFilter(MOUSE_ENTERED, hoverHandler);
                root.removeEventFilter(MOUSE_EXITED, hoverHandler);
            }
        }
    };

    private boolean isH() {
        return hoverable.get();
    }


/******************************************************************************/

    /** Image aspect ratio. Image width/height. */
    public final DoubleProperty ratioIMG = new SimpleDoubleProperty(1);
    /** Thumbnail aspect ratio. Root width/height. */
    public final DoubleProperty ratioTHUMB = new SimpleDoubleProperty(1);
    private final DoubleProperty maxIMGW = new SimpleDoubleProperty(Double.MAX_VALUE);
    private final DoubleProperty maxIMGH = new SimpleDoubleProperty(Double.MAX_VALUE);

    public double getRatioPane() {
        return ratioTHUMB.get();
    }
    public double getRatioImg() {
        return ratioIMG.get();
    }
    public DoubleProperty ratioPaneProperty() {
        return ratioTHUMB;
    }
    public DoubleProperty ratioImgProperty() {
        return ratioIMG;
    }

/******************************************************************************/

    private static final SingleR<ImprovedContextMenu<Image>,Thumbnail> img_context_menu = new SingleR<>(
        () -> {
            ImprovedContextMenu<Image> m = new ImprovedContextMenu<>();
            m.getItems().addAll(
                menuItem("Save the image as ...", e -> {
                    FileChooser fc = new FileChooser();
                        fc.getExtensionFilters().addAll(ImageFileFormat.filter());
                        fc.setTitle("Save image as...");
                        fc.setInitialFileName("new_image");
                        fc.setInitialDirectory(APP.DIR_APP);
                    File f = fc.showSaveDialog(APP.windowOwner.getStage());
                    FileUtil.writeImage(m.getValue(), f);
                }),
                menuItem("Copy the image to clipboard", e -> copyToSysClipboard(DataFormat.IMAGE,m.getValue()))
            );
            return m;
        },
        (menu,thumbnail) -> {
            Image i = thumbnail.getImage();
            menu.setValue(i);
            menu.getItems().forEach(m->m.setDisable(i==null));
        }
    );

    private static final SingleR<ImprovedContextMenu<Thumbnail>,Thumbnail> file_context_menu = new SingleR<>(
        () -> {
            ImprovedContextMenu<Thumbnail> m = new ImprovedContextMenu<>();
            m.getItems().addAll(menuItem("Browse location", e ->
                    Environment.browse(m.getValue().getFile())
                ),
                menuItem("Edit the image in editor", e ->
                    Environment.edit(m.getValue().getFile())
                ),
                menuItem("Fulscreen", e -> {
                    Screen s = WindowBase.getScreen(m.getX(),m.getY());

                    Widget c = APP.widgetManager.factories.get("Image").create();
                    Popup p = new Popup();
                    AnchorPane n = new AnchorPane();
                    n.setBackground(new Background(new BackgroundFill(BLACK, CornerRadii.EMPTY, Insets.EMPTY)));
                    n.setPrefSize(s.getBounds().getWidth(),s.getBounds().getHeight());
                    p.setHideOnEscape(true);
                    p.setConsumeAutoHidingEvents(true);
                    Node cn = c.load();
                    n.getChildren().add(cn);
                    setAnchors(cn, 0d);
                    p.getContent().setAll(n);
                    p.show(m.getValue().getPane().getScene().getWindow(), s.getBounds().getMinX(), s.getBounds().getMinY());
                    ((ImageDisplayFeature)c.getController()).showImage(m.getValue().getFile());

                }),
                menuItem("Open image", e ->
                    Environment.open(m.getValue().getFile())
                ),
                menuItem("Delete the image from disc", e ->
                    FileUtil.deleteFile(m.getValue().getFile())
                ),
                menuItem("Save the image as ...", e -> {
                   File of = m.getValue().getFile();
                   FileChooser fc = new FileChooser();
                       fc.getExtensionFilters().addAll(ImageFileFormat.filter());
                       fc.setTitle("Save image as...");
                       fc.setInitialFileName(of.getName());
                       fc.setInitialDirectory(APP.DIR_APP);

                   File nf = fc.showSaveDialog(APP.windowOwner.getStage());
                   if(nf!=null) {
                    try {
                        Files.copy(of.toPath(), nf.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ex) {
                        LOGGER.error("File export failed.",ex);
                    }
                   }
                })
            );
            return m;
        },
        (menu,thumbnail) -> {
            File f = thumbnail.getFile();
            menu.setValue(thumbnail);
            menu.getItems().forEach(i->i.setDisable(f==null));
        }
    );

    private final EventHandler<MouseEvent> contextMenuHandler = e -> {
        if(e.getButton()==SECONDARY) {
            // decide mode (image vs file), build lazily & show where requested
            if (getFile() != null)
                file_context_menu.getM(this).show(root,e);
            else if (getImage() !=null)
                img_context_menu.getM(this).show(root,e);

            e.consume();
        }
    };
}