package imageViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import sp.it.pl.audio.Song;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.gui.nodeinfo.ItemInfo;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.image.Thumbnail;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.LegacyController;
import sp.it.pl.layout.widget.controller.SimpleController;
import sp.it.pl.layout.widget.controller.io.Input;
import sp.it.pl.layout.widget.controller.io.IsInput;
import sp.it.pl.layout.widget.feature.ImageDisplayFeature;
import sp.it.pl.layout.widget.feature.ImagesDisplayFeature;
import sp.it.pl.util.access.V;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.async.executor.FxTimer;
import sp.it.pl.util.conf.EditMode;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.graphics.Util;
import sp.it.pl.util.graphics.drag.DragUtil;
import sp.it.pl.util.graphics.fxml.ConventionFxmlLoader;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ARROW_RIGHT;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.DETAILS;
import static java.util.stream.Collectors.toList;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.application.Platform.runLater;
import static javafx.css.PseudoClass.getPseudoClass;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_MOVED;
import static javafx.util.Duration.millis;
import static javafx.util.Duration.seconds;
import static sp.it.pl.layout.widget.Widget.Group.OTHER;
import static sp.it.pl.main.AppExtensionsKt.scaleEM;
import static sp.it.pl.main.AppKt.APP;
import static sp.it.pl.main.AppProgressKt.showAppProgress;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.executor.EventReducer.toFirstDelayed;
import static sp.it.pl.util.async.executor.EventReducer.toLast;
import static sp.it.pl.util.async.executor.FxTimer.fxTimer;
import static sp.it.pl.util.file.Util.getCommonRoot;
import static sp.it.pl.util.file.Util.getFilesImage;
import static sp.it.pl.util.file.UtilKt.childOf;
import static sp.it.pl.util.functional.Util.forEachWithI;
import static sp.it.pl.util.functional.Util.listRO;
import static sp.it.pl.util.functional.UtilKt.consumer;
import static sp.it.pl.util.functional.UtilKt.runnable;
import static sp.it.pl.util.graphics.UtilKt.setMinPrefMaxSize;
import static sp.it.pl.util.graphics.drag.DragUtil.installDrag;
import static sp.it.pl.util.reactive.UtilKt.maintain;

@SuppressWarnings({"WeakerAccess", "unused", "FieldCanBeLocal"})
@Widget.Info(
    author = "Martin Polakovic",
    name = "Image Viewer",
    description = "Displays images in directory. Shows main image and thumbnails. "
        + "Looks for images in subfolders.",
    howto = ""
        + "    The widget displays an image and image thumbnails for images in "
        + "specific directory - data source. Main image can change automatically "
        + " (slideshow) or manually by clicking on the thumbnail, or navigating "
        + "to next/previous image.\n"
        + "    User can display image or images in a location by setting the "
        + "file or directory, e.g., by drag & drop. The widget can also follow "
        + "playing or selected songs, displaying images in their parent "
        + "directory.\n"
        + "    The image search is recursive and search depth configurable.\n"
        + "\n"
        + "Available actions:\n"
        + "    Left click: Shows/hides thumbnails\n"
        + "    Left click bottom : Toggles info pane\n"
        + "    Nav icon click : Previous/Next image\n"
        + "    Info pane right click : Shows/hides bacground for info pane\n"
        + "    Image right click : Opens image context menu\n"
        + "    Thumbnail left click : Set as image\n"
        + "    Thumbnail right click : Opens thumbnail context menu\n"
        + "    Drag&Drop audio : Displays images for the first dropped item\n"
        + "    Drag&Drop image : Show images\n",
    version = "1.0",
    year = "2015",
    group = OTHER
)
@LegacyController
public class ImageViewer extends SimpleController implements ImageDisplayFeature, ImagesDisplayFeature {


    @FXML ScrollPane thumb_root;
    @FXML TilePane thumb_pane;
    private final Thumbnail mainImage = new Thumbnail();
    private ItemInfo itemPane;
    private Anim thumbAnim;
    private Anim navAnim;

    private final SimpleObjectProperty<File> folder = new SimpleObjectProperty<>(null);
    private final List<File> images = new ArrayList<>();
    private final List<Thumbnail> thumbnails = new ArrayList<>();
    private FxTimer slideshow = fxTimer(Duration.ZERO,INDEFINITE, runnable(this::nextImage));
    private Metadata data = Metadata.EMPTY;

    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
    public final V<Double> thumbSize = new V<>(70d).initAttachC(v -> thumbnails.forEach(t-> setMinPrefMaxSize(t.getPane(), v, v)));
    @IsConfig(name = "Thumbnail gap", info = "Spacing between thumbnails")
    public final V<Double> thumbGap = new V<>(2d);
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public final V<Duration> slideshow_dur = new V<>(seconds(15)).initSyncC(slideshow::setTimeoutAndRestart);
    @IsConfig(name = "Slideshow", info = "Turn slideshow on/off.")
    public final V<Boolean> slideshow_on = new V<>(true).initSyncC(slideshow::setRunning);
    @IsConfig(name = "Show big image", info = "Show thumbnails.")
    public final V<Boolean> showImage = new V<>(true).initAttachC(mainImage.getPane()::setVisible);
    @IsConfig(name = "Show thumbnails", info = "Show thumbnails.")
    public final V<Boolean> showThumbnails = new V<>(true);
    @IsConfig(name = "Hide thumbnails on mouse exit", info = "Hide thumbnails when mouse leaves the widget area.")
    public final V<Boolean> hideThumbEager = new V<>(true);
    @IsConfig(name = "Show thumbnails on mouse enter", info = "Show thumbnails when mouse enters the widget area.")
    public final V<Boolean> showThumbEager = new V<>(false);
    @IsConfig(name = "Show thumbnails rectangular", info = "Always frame thumbnails into squares.")
    public final V<Boolean> thums_rect = new V<>(false);
    @IsConfig(name = "Theater mode", info = "Turns off slideshow, shows image background to fill the screen, disables image border and displays information about the song.")
    public final V<Boolean> theater_mode = new V<>(false);

    @IsConfig(name = "Forbid no content", info = "Ignores empty directories and does not change displayed images if there is nothing to show.")
    public boolean keepContentOnEmpty = true;
    @IsConfig(name = "File search depth", info = "Depth to search for files in folders. 1 for current folder only.")
    public int folderTreeDepth = 2;
    @IsConfig(name = "Max number of thumbnails", info = "Important for directories with lots of images.")
    public int thumbsLimit = 50;
    @IsConfig(name = "Displayed image", editable = EditMode.APP)
    private int active_image = -1;

    private Input<Song> inputLocationOf = inputs.create("Location of", Song.class, this::dataChanged);

    public ImageViewer(Widget widget) {
        super(widget);
        root.setPrefSize(scaleEM(400), scaleEM(400));
        root.getStylesheets().add(childOf(getLocation(), "skin.css").toURI().toASCIIString());

        new ConventionFxmlLoader(root, this).loadNoEx();

        inputLocationOf.bind(Player.playing.o);

        // main image
        mainImage.setBorderVisible(true);
        mainImage.setBorderToImage(true);
        root.getChildren().add(mainImage.getPane());

        thumb_pane.getStyleClass().add("thumbnail-pane");
        thumb_pane.setTileAlignment(Pos.TOP_LEFT);
        thumb_pane.hgapProperty().bind(thumbGap);
        thumb_pane.vgapProperty().bind(thumbGap);
        root.getChildren().add(thumb_pane);

        // image navigation
        Icon nextB = new Icon(ARROW_RIGHT, 18, "Next image", this::nextImage);
             nextB.setMouseTransparent(true);
        Pane nextP = new StackPane(nextB);
             nextP.setOnMouseClicked(nextB.getOnMouseClicked());
             nextP.getStyleClass().setAll("nav-pane");
             nextP.prefWidthProperty().bind(root.widthProperty().divide(10));
             nextP.setMinWidth(20);
             nextP.setMaxWidth(50);
             nextP.visibleProperty().bind(nextP.opacityProperty().isNotEqualTo(0));
        Icon prevB = new Icon(ARROW_LEFT, 18, "Previous image", this::prevImage);
             prevB.setMouseTransparent(true);
        Pane prevP = new StackPane(prevB);
             prevP.setOnMouseClicked(prevB.getOnMouseClicked());
             prevP.getStyleClass().setAll("nav-pane");
             prevP.prefWidthProperty().bind(root.widthProperty().divide(10));
             prevP.setMinWidth(20);
             prevP.setMaxWidth(50);
             prevP.visibleProperty().bind(prevP.opacityProperty().isNotEqualTo(0));
        root.getChildren().add(prevP);
        StackPane.setAlignment(prevP, Pos.CENTER_LEFT);
        root.getChildren().add(nextP);
        StackPane.setAlignment(nextP, Pos.CENTER_RIGHT);

        navAnim = new Anim(millis(300), p -> {
            prevP.setOpacity(p);
            nextP.setOpacity(p);
            prevB.setTranslateX(+40*(p-1));
            nextB.setTranslateX(-40*(p-1));
        }).applyNow();
        FxTimer navHideDelayed = fxTimer(seconds(2.0), 1, runnable(() -> { if (!nextP.isHover() && !prevP.isHover()) navAnim.playClose(); }));
        EventReducer<Object> navInactive = toLast(1000, it -> { if (!nextP.isHover() && !prevP.isHover()) navAnim.playClose(); });
        EventReducer<Object> navActive = toFirstDelayed(400, it -> navAnim.playOpen());
        maintain(showThumbnails, v -> navAnim.playClose());
        root.addEventFilter(MOUSE_EXITED, e -> navInactive.push(e));
        root.addEventFilter(MOUSE_MOVED, e -> {
            if (!showThumbnails.getValue()) {
                navActive.push(e);
                if (!nextP.isHover() && !prevP.isHover())
                    navInactive.push(e);
            }
        });

        // thumb anim
        thumbAnim = new Anim(millis(500), thumb_root::setOpacity);
        thumb_root.visibleProperty().bind(thumb_root.opacityProperty().isNotEqualTo(0));
        thumb_root.toFront();

        // thumbnails
        root.getChildren().add(thumb_root);
        root.setOnMouseClicked( e -> {
            if (e.getButton()==SECONDARY && showThumbnails.getValue()) {
                showThumbnails.setCycledValue();
                e.consume();
            }
            if (e.getButton()==PRIMARY) {
                if (e.getY()>0.8*root.getHeight() && e.getX()>0.7*root.getWidth()) {
                    theater_mode.setCycledValue();
                } else {
                    showThumbnails.setCycledValue();
                }
                e.consume();
            }
        });
        // prevent scrollpane from preventing show thumbnails change
        thumb_root.setOnMouseClicked(e -> {
            //if (e.getButton()==PRIMARY) {
                showThumbnails.setCycledValue();
                e.consume();
            //}
        });

        // slideshow on hold during user activity
        root.addEventFilter(MOUSE_ENTERED, e -> {
            if (slideshow_on.getValue()) slideshow.pause();
        });
        root.addEventFilter(MOUSE_EXITED, e -> {
            if (slideshow_on.getValue()) slideshow.unpause();
        });

        // refresh if source data changed
        ChangeListener<File> locationChange = (o,ov,nv) -> readThumbnails();
        folder.addListener(locationChange);
        onClose.plusAssign(() -> folder.removeListener(locationChange));

        // drag&drop
        installDrag(
            root, DETAILS,"Display",
            e -> DragUtil.hasImage(e) || DragUtil.hasAudio(e) || DragUtil.hasFiles(e),
            e -> e.getGestureSource()==mainImage.getPane(),
            e -> {
                if (e.getDragboard().hasFiles()) {
                    dataChanged(getCommonRoot(e.getDragboard().getFiles()));
                    return;
                }
                if (DragUtil.hasAudio(e)) {
                    // get first item
                    List<Song> songs = DragUtil.getAudioItems(e);
                    if (!songs.isEmpty()) dataChanged(songs.get(0));
                } else
                if (DragUtil.hasImage(e)) {
                    showAppProgress(
                        DragUtil.getImages(e).useBy(FX, this::showImages),
                        widget.custom_name.getValue() + "Loading images"
                    );
                }
            }
        );

        // forbid app scrolling when thumbnails are visible
        thumb_root.setOnScroll(Event::consume);

        showThumbnails.maintain(v -> {
            thumbAnim.playFromDir(v);
            if (v) navAnim.playClose();
        });

        // show/hide thumbnails eager
        root.addEventHandler(MOUSE_EXITED, e -> {
            if (hideThumbEager.getValue()) {
                double x = e.getX(), y = e.getY();
                if (!root.contains(x,y)) // make sure mouse really is out
                    showThumbnails.setValue(false);
            }
        });
        root.addEventHandler(MOUSE_ENTERED, e -> {
            if (showThumbEager.getValue()) {
                    showThumbnails.setValue(true);
            }
        });

        thums_rect.maintain(v ->
            thumbnails.forEach(t -> {
                t.setBorderToImage(!v);
                t.setBackgroundVisible(v);
            })
        );
        theater_mode.maintain(this::applyTheaterMode);
        readThumbnails();

        onClose.plusAssign(thumb_reader::stop);
        onClose.plusAssign(slideshow::stop);
    }

    public boolean isEmpty() {
        return thumbnails.isEmpty();
    }

    @Override
    public void showImage(File imgFile) {
        if (imgFile !=null && imgFile.getParentFile()!=null) {
            folder.set(imgFile.getParentFile());
            // this resets the slideshow counter
            // notice that we query whether slideshow is running, not whether it is
            // supposed to run, which might not always be the same
            if (slideshow.isRunning()) slideshow.start();
            active_image = -1;
            mainImage.loadImage(imgFile);
        }
    }

    @Override
    public void showImages(Collection<File> imgFiles) {
        if (imgFiles.isEmpty()) return;

        showImage(imgFiles.stream().findFirst().orElse(null));
        active_image = 0;
        imgFiles.forEach(this::addThumbnail);
    }

/****************************** HELPER METHODS ********************************/

    @IsInput("Location of")
    private void dataChanged(Song i) {
        if (i==null) dataChanged(Metadata.EMPTY);
        else APP.db.songToMeta(i, consumer(this::dataChanged));
    }

    private void dataChanged(Metadata m) {
        // remember data
        data = m;
        // calculate new location
        File new_folder = data==null ? null : data.getLocation();
        dataChanged(new_folder);
    }

    @IsInput("Directory")
    private void dataChanged(File new_folder) {
        // prevent refreshing location if should not
        if (keepContentOnEmpty && new_folder==null) return;
        // refresh location
        folder.set(new_folder);
        if (theater_mode.getValue()) itemPane.setValue(data);
    }

    private void run() {navAnim.playClose();}

    class Exec {
        ExecutorService e = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r);
                   t.setDaemon(true);
                   return t;
        });
        List<Runnable> l = new ArrayList<>();
        boolean executing = false;

        void run(Runnable r) {
            if (!l.isEmpty()) l.add(r);
            else {
                e.execute(() -> {
                    r.run();
                    Platform.runLater(() -> {
                        l.remove(r);
                        if (!l.isEmpty()) run(l.get(0));
                    });
                });
            }
        }

        void stop() {
            l.clear();
        }
    }

    private Exec thumb_reader = new Exec();

    // Make an action/config
    private void readThumbnails() {
        // clear old content before adding new
        // setImage(-1) // it is set during thumbnail reading, no need to clear it
        images.clear();
        thumbnails.clear();
        thumb_pane.getChildren().clear();

        thumb_reader.stop();
        thumb_reader.run(() -> {
            int ai = active_image;
            // discover files
            List<File> files = folder.get()==null
                ? listRO()
                : getFilesImage(folder.get(),folderTreeDepth).limit(thumbsLimit).collect(toList());
            if (files.isEmpty()) {
                Platform.runLater(() -> setImage(-1));
            } else {
                // add actions - turn file into thumbnail
                forEachWithI(files, (i,f) -> {
                    // create & load thumbnail on bgr thread
                    Thumbnail t = createThumbnail(f);
                    // add to gui
                    runLater(() -> insertThumbnail(t));
                    // set as image if it should be
                    // for example when widget loads, the image might be
                    // restored from deserialized index value
                    if (i==ai) runLater(() -> setImage(ai));
                });
            }
        });
    }

    private void addThumbnail(final File f) {
        insertThumbnail(createThumbnail(f));
    }

    private Thumbnail createThumbnail(final File f) {
        // create thumbnail
        Thumbnail t = new Thumbnail(thumbSize.getValue(),thumbSize.getValue());
                  t.setBorderToImage(!thums_rect.getValue());
                  t.setBackgroundVisible(thums_rect.getValue());
                  t.hoverable.set(true);
                  t.loadImage(f);
                  t.getPane().setOnMouseClicked( e -> {
                      if (e.getButton() == PRIMARY) {
                          setImage(images.indexOf(f));
                          e.consume();
                      }
                  });
        return t;
    }

    private void insertThumbnail(Thumbnail t) {
        // store
        thumbnails.add(t);
        images.add(t.getFile());
        thumb_pane.getChildren().add(t.getPane());
        // if this is first thumbnail display it immediately
        // but only if the displayed image is not one of the thumbnails - is not located
        // in folder.get() directory
        // avoids image loading + necessary to display custom image, which fires
        // thumbnail refresh and subsequently would change the displayed image
        File displLoc = mainImage.getFile()==null ? null : mainImage.getFile().getParentFile();
        File currLoc = folder.get();
        if (thumbnails.size()==1 && currLoc!=null && !currLoc.equals(displLoc))
            setImage(0);
    }

    private void setImage(int index) {
        if (images.isEmpty()) index = -1;
        if (index >= images.size()) index = images.size()-1;

        if (index == -1) {
            mainImage.loadImage((Image) null);
            // this is unnecessary because we check the index for validity
            // also unwanted, sometimes this would erase our deserialized index
            //  active_image = -1;
        } else {
            mainImage.loadImage(images.get(index));
            active_image = index;
        }
    }

    public void nextImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image >= images.size()-1) ? 0 : active_image+1;
            setImage(index);
        }
        if (slideshow.isRunning()) slideshow.start();
    }

    public void prevImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image < 1) ? images.size()-1 : active_image-1;
            setImage(index);
        }
        if (slideshow.isRunning()) slideshow.start();
    }

    private void applyShowThumbs(boolean v) {
        thumbAnim.playFromDir(v);
        if (v) navAnim.playClose();
    }

    private void applyTheaterMode(boolean v) {
        root.pseudoClassStateChanged(getPseudoClass("theater"), v);
        if (v && itemPane==null) {
            itemPane = new ItemInfo(false);
            itemPane.setOnMouseClicked(e -> {
                if (e.getButton()==SECONDARY) {
                    if (itemPane.getStyleClass().isEmpty()) itemPane.getStyleClass().addAll("block-alternative");
                    else itemPane.getStyleClass().clear();
                    e.consume();
                }
            });

            AnchorPane itemPaneRoot = Util.layAnchor(itemPane, null, 20.0, 20.0, null);
            itemPaneRoot.setPickOnBounds(false);
            root.getChildren().add(itemPaneRoot);

            itemPane.setValue(data);
        }

        slideshow_on.setValue(v ? false : slideshow_on.getValue());
        mainImage.setBackgroundVisible(v);
        mainImage.setBorderVisible(!v);
        if (itemPane!=null) itemPane.setVisible(v);
    }
}