 
package ImageViewer;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.virtual.InfoNode.InfoTask;
import GUI.objects.ItemInfo;
import GUI.objects.PopOver.PopOver;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.ImageDisplayFeature;
import Layout.Widgets.Features.ImagesDisplayFeature;
import Layout.Widgets.Widget;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.CUSTOM;
import static PseudoObjects.ReadMode.PLAYING;
import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import static java.util.Collections.EMPTY_LIST;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.animation.Animation.INDEFINITE;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import static javafx.geometry.Pos.CENTER;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import org.reactfx.Subscription;
import util.Parser.File.FileUtil;
import util.Util;
import util.access.Accessor;
import util.async.Async;
import util.async.FxTimer;
import static util.functional.FunctUtil.forEachIndexedStream;

/**
 * 
 * @author Plutonium_
 */
@Widget.Info(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Image Viewer",
    description = "Displays images.",
    howto = "Available actions:\n" +
            "    Left click middle: Shows/hides thumbnails\n" +
            "    Left click left side: Previous image\n" +
            "    Left click right side : Next image\n" +
            "    Left click bottom : Turns theater mode on/off\n" +
            "    Info pane right click : Shows/hides bacground for info pane\n" +
            "    Image right click : Opens image context menu\n" +
            "    Thumbnail left click : Set as image\n" +
            "    Thumbnail right click : Opens thumbnail context menu\n" +
            "    Drag&Drop audio : Displays images for the first dropped item\n" + 
            "    Drag&Drop image : Copies images into current item's locaton\n",
    notes = "",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ImageViewerController extends FXMLController implements ImageDisplayFeature, ImagesDisplayFeature {
    
    // gui
    @FXML private AnchorPane entireArea;
    @FXML private ScrollPane thumb_root;
    @FXML private TilePane thumb_pane;
    private final Thumbnail thumbnail = new Thumbnail();
    private FadeTransition fIn;
    private FadeTransition fOut;
    private ItemInfo itemPane;
    
    // state
    private final SimpleObjectProperty<File> folder = new SimpleObjectProperty(null); // current location source (derived from metadata source)
    private final ObservableList<File> images = FXCollections.observableArrayList();
    private final List<Thumbnail> thumbnails = new ArrayList();
    private boolean image_reading_lock = false;
    // eager initialized state
    private FxTimer slideshow = new FxTimer(Duration.ZERO,INDEFINITE,this::nextImage);
    private Subscription dataMonitoring;
    private Metadata data;
    
    // auto applied cnfigurables
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public final Accessor<ReadMode> readMode = new Accessor<>(PLAYING, v -> dataMonitoring = Player.bindObservedMetadata(v,dataMonitoring,this::dataChanged));
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
    public final Accessor<Double> thumbSize = new Accessor<>(Thumbnail.default_Thumbnail_Size,v -> thumbnails.forEach(t->t.getPane().setPrefSize(v,v)));
    @IsConfig(name = "Thumbnail gap", info = "Spacing between thumbnails")
    public final Accessor<Double> thumbGap = new Accessor<>(2d, v -> {
        thumb_pane.setHgap(v);
        thumb_pane.setVgap(v);
    });
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public final Accessor<Double> slideshow_dur = new Accessor<>(15000d, slideshow::setTimeoutAndRestart);
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public final Accessor<Boolean> slideshow_on = new Accessor<>(true, v -> {
        if (v) slideshow.restart(); else slideshow.stop();
    });
    @IsConfig(name = "Show big image", info = "Show thumbnails.")
    public final Accessor<Boolean> showImage = new Accessor<>(true, thumbnail.getPane()::setVisible);
    @IsConfig(name = "Show thumbnails", info = "Show thumbnails.")
    public final Accessor<Boolean> showThumbnails = new Accessor<>(true, this::setThumbnailsVisible);
    @IsConfig(name = "Hide thumbnails on mouse exit", info = "Hide thumbnails when mouse leaves the widget area.")
    public final Accessor<Boolean> hideThumbEager = new Accessor<>(true, v -> {
        if (v) entireArea.setOnMouseExited(e -> {
                   // ensure that mouse really exited, as mouse exit event also
                   // happens when mouse enters context menu or app ui drag sets
                   // ui to mouse transparent
                   if(!entireArea.contains(e.getX(), e.getY()))
                       showThumbnails.setNapplyValue(false);
               });
        else entireArea.setOnMouseExited(null);
    });
    @IsConfig(name = "Show thumbnails on mouse enter", info = "Show thumbnails when mouse enters the widget area.")
    public final Accessor<Boolean> showThumbEager = new Accessor<>(false, v -> {
        if (v) entireArea.setOnMouseEntered(e -> showThumbnails.setNapplyValue(true));
        else entireArea.setOnMouseEntered(null);
    });
    @IsConfig(name = "Show thumbnails rectangular", info = "Always frame thumbnails into squares.")
    public final Accessor<Boolean> thums_rect = new Accessor<>(false, v -> thumbnails.forEach(t->{
        t.setBorderToImage(!v);
        t.setBackgroundVisible(v);
    }));
    @IsConfig(name = "Alignment", info = "Preferred image alignment.")
    public final Accessor<Pos> align = new Accessor<>(CENTER, thumbnail::applyAlignment);
    @IsConfig(name = "Theater mode", info = "Turns off slideshow, shows image background to fill the screen, disables image border and displays information about the song.")
    public final Accessor<Boolean> theater_mode = new Accessor<>(false, this::applyTheaterMode);
    
    // non applied configurables
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "Thumbnail load time", info = "Delay between thumbnail loading. It is not recommended to load all thumbnails immediatelly or fast one after another")
    public long thumbnailReloadTime = 200l;
    @IsConfig(name = "Show previous content when empty", info = "Keep showing previous content when the new content is empty.")
    public boolean keepContentOnEmpty = true;
    @IsConfig(name = "File search depth", info = "Depth to search to for files in folders.")
    public int folderTreeDepth = 1;
    @IsConfig(name = "Max amount of thubmnails", info = "Important for directories with lots of images.")
    public int thumbsLimit = 100;
    @IsConfig(
//            editable = false
    )
    private int active_image = -1;
    
    /** {@inheritDoc} */
    @Override
    public void init() {
        try {
            entireArea.getStylesheets().add(getResource("skin.css").toURI().toURL().toExternalForm());
        } catch (MalformedURLException ex) {
            Logger.getLogger(ImageViewerController.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // add thumbnail & span whole area
        entireArea.getChildren().add(thumbnail.getPane());
        thumbnail.setBorderToImage(true);
        thumbnail.getPane().prefWidthProperty().bind(entireArea.widthProperty());
        thumbnail.getPane().prefHeightProperty().bind(entireArea.heightProperty());
        
        fIn = new FadeTransition(Duration.millis(500), thumb_root);
        fOut = new FadeTransition(Duration.millis(500), thumb_root);
        fIn.setToValue(1);
        fOut.setToValue(0);
        fOut.setOnFinished(e->thumb_root.setVisible(false));
        
        entireArea.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                if(e.getY()>0.8*entireArea.getHeight() && e.getX()>0.7*entireArea.getWidth()) {
                    theater_mode.setCycledNapplyValue();
                } else {
                    double width = entireArea.getWidth();
                    if (e.getX() < 0.15*width) prevImage();
                    else if(e.getX() > 0.85*width) nextImage();
                    else showThumbnails.setCycledNapplyValue();
                }
                e.consume();
            }
        });
        
        // position thumbnail scroll pane & make sure it doesnt cover whole area
        Util.setAPAnchors(thumb_root, 0);
        entireArea.heightProperty().addListener((o,ov,nv) -> AnchorPane.setBottomAnchor(thumb_root, nv.doubleValue()*0.3));
        thumb_root.toFront();
        // prevent scrollpane from preventing show thumbnails change
        thumb_root.setOnMouseClicked(e -> {
            if (e.getButton()==PRIMARY) {
                showThumbnails.setCycledNapplyValue();
                e.consume();
            }
        });
        
        // refresh if source data changed
        folder.addListener(locationChange);
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        entireArea.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped(e -> {
            if(DragUtil.hasAudio(e.getDragboard())) {
                // get first item
                List<Item> items = DragUtil.getAudioItems(e);
                // getMetadata, refresh
                if (!items.isEmpty()) {
                    // change mode if desired
                    if (changeReadModeOnTransfer) readMode.setNapplyValue(CUSTOM);
                    // set data
                    dataChanged(items.get(0).getMetadata());
                }
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
            if(folder.get()!=null && DragUtil.hasImage(e.getDragboard())) {System.out.println("received img");
                // grab images
                    InfoTask info = new InfoTask(null, new Label(), new ProgressBar());
                    HBox b = new HBox(8, info.message, info.progressIndicator);
                    PopOver p = new PopOver("Handling images", b);
                    Task t = Async.runAsTask(()->{System.out.println(Platform.isFxApplicationThread() + " dddd");
                        DragUtil.doWithImages(e, files -> {
                            // prevent copying displayed items
                            // the copyng itself prevents copying the files if source and
                            // destination is the same, but we obtained files recursively
                            // which means the location can differ
                            files.removeIf(images::contains);
                            // copy files to displayed item's location
                            FileUtil.copyFiles(files, folder.get())
                                // create thumbnails for new files (we avoid refreshing all)
                                .forEach(f->addThumbnail(f));
                        });
                    });
                    info.bind(t);
                    p.show(entireArea.getScene().getWindow());
                    p.centerOnScreen();
//                DragUtil.doWithImageItems(e, files -> {
//                    // prevent copying displayed items
//                    // the copyng itself prevents copying the files if source and
//                    // destination is the same, but we obtained files recursively
//                    // which means the location can differ
//                    files.removeIf(images::contains);
//                    // copy files to displayed item's location
//                    FileUtil.copyFiles(files, folder.get())
//                        // create thumbnails for new files (we avoid refreshing all)
//                        .forEach(f->addThumbnail(f));
//                });
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        // consume scroll event to prevent app scroll behavior // optional
        entireArea.setOnScroll(Event::consume);
    }
    
    /** {@inheritDoc} */
    @Override
    public void close() {
        // unbind
        if (dataMonitoring!=null) dataMonitoring.unsubscribe();
        folder.removeListener(locationChange);
        // prevent continued thumbnail creation
        stopReadingThumbnails();
        //stop slideshow
        slideshow.stop();
    }
    
/********************************* PUBLIC API *********************************/
 
    /** {@inheritDoc} */
    @Override
    public void refresh() {
        image_reading_lock = true; // prevent reading thumbnails twice (see below)
        // this might change the location, but since we always want to fire
        // location change, we prevent it here and fire it manually below
        readMode.applyValue();
        image_reading_lock = false; // unlock
        thumbSize.applyValue();
        thumbGap.applyValue();
        slideshow_dur.applyValue();
        slideshow_on.applyValue();
        showThumbnails.applyValue();
        hideThumbEager.applyValue();
        thums_rect.applyValue();
        align.applyValue();
        theater_mode.applyValue();
        readThumbnails();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return thumbnails.isEmpty();
//        return folder.get() == null;
    }
    
    /** {@inheritDoc} */
    @Override
    public void showImage(File img_file) {
        if(img_file!=null && img_file.getParentFile()!=null) {
            folder.set(img_file.getParentFile());
            // this resets the slideshow counter
            // notice that we query whether slideshow is running, not whether it is
            // supposed to run, which might not always be the same
            if(slideshow.isRunning()) slideshow.restart();
            active_image = -1;
            thumbnail.loadImage(img_file);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void showImages(List<File> img_files) {
        if(img_files.isEmpty()) return;
        
        showImage(img_files.get(0));
        active_image = 0;
        img_files.forEach(f->addThumbnail(f));
    }
    
/****************************** HELPER METHODS ********************************/
    
    private void dataChanged(Metadata m) {
        // remember data
        data = m;
        // calculate new location
        File new_folder = (m==null || !m.isFileBased()) ? null : m.getLocation();
        // prevent refreshing location if shouldnt
        if(keepContentOnEmpty && new_folder==null) return;
        // refresh location
        folder.set(new_folder);
        if(theater_mode.getValue()) itemPane.setData("", m);
     }
    
    // using change listener means the event fires only if value really changed
    // which is important here to avoid reading thumbnails for nothing
    private final ChangeListener<File> locationChange = (o,ov,nv) -> readThumbnails();
    
    private Timeline thumbTimeline = new Timeline();
    private final Service<Void> thumbReader = new Service() {
        // service recreates task so it can be restarted
        @Override protected Task<Void> createTask() {
            // return newly constructed task
            return new Task<Void>() {
                @Override protected Void call() throws Exception {
                    int ai = active_image;
                    // discover files
                    List<File> files = folder.get()==null ? EMPTY_LIST
                            :FileUtil.getImageFilesRecursive(folder.get(),folderTreeDepth, thumbsLimit);
                    if(files.isEmpty()) {
                        setImage(-1);
                    } else {
                        // create timeline for adding thumbnails
                        thumbTimeline = new Timeline();
                        // add actions - turn file into thumbnail
                        forEachIndexedStream(files, 
                                (i,f) -> new KeyFrame(Duration.millis((1+i)*thumbnailReloadTime), e -> {
                                        // create & load thumbnail on bgr thread
                                        Thumbnail t = createThumbnail(f);
                                        // add to gui
                                        insertThumbnail(t);
                                        // set as image if it should be
                                        // for example when widget loads, the image might be
                                        // restored from deserialized index value
                                        if(i==ai) setImage(ai);
                                    }))
                                .forEach(thumbTimeline.getKeyFrames()::add);
                        thumbTimeline.play();
                    }
                    return null;
                }
            };
        }
    };
    
    private void readThumbnails() {
        if (image_reading_lock) return; 
        // clear old content before adding new
        // setImage(-1) // it is set during thumbnail reading, no need to clear it
        images.clear();
        thumbnails.clear();
        thumb_pane.getChildren().clear();   
        // stop & ready reading process
        stopReadingThumbnails();
        // read if available
        thumbReader.start();
    }
    private void stopReadingThumbnails() {
        thumbTimeline.stop();
        thumbReader.cancel();
        thumbReader.reset();
    }
    
    private void addThumbnail(final File f) {
        insertThumbnail(createThumbnail(f));
    }
    
    private Thumbnail createThumbnail(final File f) {
        // create thumbnail
        Thumbnail t = new Thumbnail(thumbSize.getValue());
                  t.setBorderToImage(!thums_rect.getValue());
                  t.setBackgroundVisible(thums_rect.getValue());
                  t.setHoverable(true);
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
        // if this is first thumbnail display it immediatelly
        // but only if the displayed image isnt one of the thumbnails - isnt located
        // in folder.get() directory
        // avoids image loading + necessary to display custom image, which fires
        // thumbnail refresh and subsequently would change the displayed image
        File displLoc = thumbnail.getFile()==null ? null : thumbnail.getFile().getParentFile();
        File currLoc = folder.get();
        if (thumbnails.size()==1 && currLoc!=null && !currLoc.equals(displLoc))
            setImage(0);
    }    
    
    private void setImage(int index) {
        if (images.isEmpty()) index = -1;
        if (index >= images.size()) index = images.size()-1;
        
        if (index == -1) {
            Image i = null;
            thumbnail.loadImage(i);
            // this is unnecessary because we check the index for validity
            // also unwanted, sometimes this would erase our deserialized index
            //  active_image = -1;
        } else {
            thumbnail.loadImage(images.get(index));
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
        if(slideshow.isRunning()) slideshow.restart();
    }
    
    public void prevImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image < 1) ? images.size()-1 : active_image-1;
            setImage(index);
        }
        if(slideshow.isRunning()) slideshow.restart();
    }
    
    private void setThumbnailsVisible(boolean v) {
        if(v) {
            thumb_root.setVisible(true);
            fIn.play();
        } else fOut.play();
    }
    
    private void applyTheaterMode(boolean v) {
        if(v && itemPane==null) {
            itemPane = new ItemInfo(false);
            entireArea.getChildren().add(itemPane);
            itemPane.toFront();
            AnchorPane.setBottomAnchor(itemPane, 20d);
            AnchorPane.setRightAnchor(itemPane, 20d);
            itemPane.setData("", data);

            itemPane.setOnMouseClicked(ee -> {
                if(ee.getButton()==SECONDARY) {
                    if(itemPane.getStyleClass().isEmpty()) itemPane.getStyleClass().addAll("block");
                    else itemPane.getStyleClass().clear();
                    ee.consume();
                }
            });
        }
        
        slideshow_on.applyValue(v ? false : slideshow_on.getValue());
        thumbnail.setBackgroundVisible(v);
        thumbnail.setBorderVisible(!v);
        if (itemPane!=null) itemPane.setVisible(v);
    }
}