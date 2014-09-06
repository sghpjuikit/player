
package ImageViewer;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.objects.Thumbnail;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Widget;
import Layout.Widgets.WidgetInfo;
import PseudoObjects.ReadMode;
import static PseudoObjects.ReadMode.CUSTOM;
import static PseudoObjects.ReadMode.PLAYING;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import static javafx.scene.input.MouseButton.PRIMARY;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import org.reactfx.Subscription;
import utilities.FileUtil;
import utilities.FxTimer;
import utilities.Util;
import utilities.access.Accessor;

/**
 * 
 * @author Plutonium_
 */
@WidgetInfo(
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    name = "Image Viewer",
    description = "Displays images.",
    howto = "Available actions:\n" +
            "    Left click middle: Toggles show thumbnails\n" +
            "    Image left click left side: Previous image\n" +
            "    Image left click right side : Next image\n" +
            "    Image right click : Opens image context menu\n" +
            "    Thumbnail left click : Show as image\n" +
            "    Thumbnail right click : Opens thumbnail context menu\n" +
            "    Drag&Drop audio : Displays images for the first dropped item\n" + 
            "    Drag&Drop image : Copies images into current item's locaton\n",
    notes = "",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ImageViewerController extends FXMLController {
    
    // gui
    @FXML private AnchorPane entireArea;
    @FXML private ScrollPane thumb_root;
    @FXML private TilePane thumb_pane;
    private final Thumbnail thumbnail = new Thumbnail();
    private FadeTransition fIn;
    private FadeTransition fOut;
    
    // state
    private final SimpleObjectProperty<File> folder = new SimpleObjectProperty(null); // current location source (derived from metadata source)
    private final ObservableList<File> images = FXCollections.observableArrayList();
    private final List<Thumbnail> thumbnails = new ArrayList();
    private boolean image_reading_lock = false;
    private int active_image = -1;
    // eager initialized state
    private FxTimer slideshow;
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
    public final Accessor<Double> slideshow_dur = new Accessor<>(15000d, this::slideshowDur);
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public final Accessor<Boolean> slideshow_on = new Accessor<>(true, v -> {
        if (v) slideshowStart(); else slideshowEnd();
    });
    @IsConfig(name = "Show big image", info = "Show thumbnails.")
    public final Accessor<Boolean> showImage = new Accessor<>(true, this::setImageVisible);
    @IsConfig(name = "Show thumbnails", info = "Show thumbnails.")
    public final Accessor<Boolean> showThumbnails = new Accessor<>(true, this::setThumbnailsVisible);
    @IsConfig(name = "Hide thumbnails on mouse exit", info = "Hide thumbnails when mouse leaves the widget area.")
    public final Accessor<Boolean> hideThumbEager = new Accessor<>(true, v -> {
        if (v) entireArea.setOnMouseExited(e -> showThumbnails.setNapplyValue(false));
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
    
    // non applied configurables
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "Thumbnail load time", info = "Delay between thumbnail loading. It is not recommended to load all thumbnails immediatelly one after another")
    public long thumbnailReloadTime = 150l;
    @IsConfig(name = "Show previous content when empty", info = "Keep showing previous content when the new content is empty.")
    public boolean keepContentOnEmpty = true;
    @IsConfig(name = "File search depth", info = "Depth to search to for files in folders.")
    public int folderTreeDepth = 1;
    
    @Override
    public void init() {
        //initialize gui
        entireArea.getChildren().add(thumbnail.getPane());
        thumbnail.setBorderToImage(true);
        thumbnail.getPane().prefWidthProperty().bind(entireArea.widthProperty());
        thumbnail.getPane().prefHeightProperty().bind(entireArea.heightProperty());
        thumbnail.getPane().opacityProperty().bind(Bindings.subtract(1d, thumb_root.opacityProperty().divide(2)));
        
        fIn = new FadeTransition(Duration.millis(500), thumb_root);
        fOut = new FadeTransition(Duration.millis(500), thumb_root);
        fIn.setToValue(1);
        fOut.setToValue(0);
        fOut.setOnFinished(e->thumb_root.setVisible(false));
        
        entireArea.setOnMouseClicked( e -> {
            if(e.getButton()==PRIMARY) {
                double width = entireArea.getWidth();
                if (e.getX() < 0.2*width) prevImage();
                else if(e.getX() > 0.8*width) nextImage();
                else showThumbnails.toggleNapplyValue();
                e.consume();
            }
        });
        
        AnchorPane.setBottomAnchor(thumb_root, 0.0);
        AnchorPane.setTopAnchor(thumb_root, 0.0);
        AnchorPane.setLeftAnchor(thumb_root, 0.0);
        AnchorPane.setRightAnchor(thumb_root, 0.0);
        thumb_root.toFront();
        thumb_root.setPickOnBounds(false);
        thumb_root.setOnMouseClicked(e -> {
            if (e.getButton()==PRIMARY) {
                showThumbnails.toggleNapplyValue();
                e.consume();
            }
        });
        
        // refresh if source data changed
        folder.addListener(locationChange);
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        entireArea.setOnDragOver(DragUtil.imageFileDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped( e -> {
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
            if(folder.get()!=null && DragUtil.hasImage(e.getDragboard())) {
                // grab images
                DragUtil.doWithImageItems(e, files -> {
                    // copy files to displayed item'slocation
                    FileUtil.copyFiles(files, folder.get())
                        // create thumbnails for new files (we avoid refreshign all)
                        .forEach(f->addThumbnail(f));
                });
                // end drag
                e.setDropCompleted(true);
                e.consume();
            }
        });
        
        // consume scroll event to prevent app scroll behavior // optional
        entireArea.setOnScroll(Event::consume);
    }

    @Override
    public void OnClosing() {
        // unbind
        if (dataMonitoring!=null) dataMonitoring.unsubscribe();
        folder.removeListener(locationChange);
    }
    
/********************************* PUBLIC API *********************************/
 
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
        thums_rect.applyValue();
        hideThumbEager.applyValue();
        readThumbnails();
    }

    @Override
    public boolean isEmpty() {
        return folder.get() == null;
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
     }
    
    // using change listener means the event fires only if value really changed
    // which is important here to avoid reading thumbnails for nothing
    private final ChangeListener<File> locationChange = (o,ov,nv) -> readThumbnails();
    Executor executor = Executors.newFixedThreadPool(1, new ThreadFactory(){
                @Override public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                           t.setDaemon(true);
                    return t;
                }
            });
    
    private Timeline thumbTimeline = new Timeline();
    private final Service<Void> thumbReader = new Service() {
        // service recreates task so it can be restarted
        @Override protected Task<Void> createTask() {
            // return newly constructed task
            return new Task<Void>() {
                @Override protected Void call() throws Exception {
                    // discover files
                    List<File> files = FileUtil.getImageFilesRecursive(folder.get(),folderTreeDepth);
                    // create timeline for adding thumbnails
                    thumbTimeline = new Timeline();
                    // add actions - turn file into thumbnail
                    Util.forEachIndexedStream(files, 
                            (i,f) -> new KeyFrame(Duration.millis((1+i)*thumbnailReloadTime), e -> {
                                    // create & load thumbnail on bgr thread
                                    Thumbnail t = createThumbnail(f);
                                    // add to gui on gui thread
                                    Platform.runLater(()->insertThumbnail(t));
                                }))
                            .forEach(thumbTimeline.getKeyFrames()::add);
                    thumbTimeline.play();
                    return null;
                }
            };
        }
    };
    
    private void readThumbnails() {
        if (image_reading_lock) return; 
         // clear old content before adding new
        setImage(-1);   // set empty image
        images.clear();
        thumbnails.clear();
        thumb_pane.getChildren().clear();   
        // ready reading process
        thumbTimeline.stop();
        thumbReader.cancel();
        thumbReader.reset();
        // read if available
        if (folder.get() != null)
            thumbReader.start();
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
//        if (thumbnails.size()==1) setImage(0);
    }
    
    
    
    private void setImage(int index) {
        if (images.isEmpty()) index = -1;
        if (index >= images.size()) index = images.size()-1;
        
        if (index == -1) {
            Image i = null;
            thumbnail.loadImage(i);
            active_image = -1;
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
    
    private void slideshowDur(double v) {
        if(slideshow != null && slideshow_on.getValue())
            slideshow.restart(Duration.millis(v));
    }
    
    private void setImageVisible(boolean v) {
        thumbnail.getPane().setVisible(v);        
    }
    
    private void setThumbnailsVisible(boolean v) {
        if(v) {
            thumb_root.setVisible(true);
            fIn.play();
        } else fOut.play();
    }
}
//
//package ImageViewer;
//
//
//import AudioPlayer.Player;
//import AudioPlayer.playlist.Item;
//import AudioPlayer.tagging.Metadata;
//import Configuration.IsConfig;
//import GUI.DragUtil;
//import GUI.Panes.ImageFlowPane;
//import GUI.objects.Thumbnail;
//import Layout.Widgets.FXMLController;
//import Layout.Widgets.Widget;
//import Layout.Widgets.WidgetInfo;
//import PseudoObjects.ReadMode;
//import static PseudoObjects.ReadMode.CUSTOM;
//import static PseudoObjects.ReadMode.PLAYING;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//import javafx.animation.KeyFrame;
//import javafx.animation.KeyValue;
//import javafx.animation.Timeline;
//import javafx.beans.property.SimpleIntegerProperty;
//import javafx.beans.property.SimpleObjectProperty;
//import javafx.beans.value.ChangeListener;
//import javafx.collections.FXCollections;
//import javafx.collections.ObservableList;
//import javafx.concurrent.Service;
//import javafx.concurrent.Task;
//import javafx.event.Event;
//import javafx.fxml.FXML;
//import javafx.scene.control.ScrollPane;
//import javafx.scene.image.Image;
//import static javafx.scene.input.MouseButton.PRIMARY;
//import javafx.scene.layout.AnchorPane;
//import javafx.scene.layout.Pane;
//import javafx.scene.layout.TilePane;
//import javafx.util.Duration;
//import org.reactfx.Subscription;
//import utilities.FileUtil;
//import utilities.FxTimer;
//import utilities.access.Accessor;
//
///**
// * 
// * @author Plutonium_
// */
//@WidgetInfo(
//    author = "Martin Polakovic",
//    programmer = "Martin Polakovic",
//    name = "Image Viewer",
//    description = "Displays images.",
//    howto = "Available actions:\n" +
//            "    Image left click : Toggles show thumbnails\n" +
//            "    Image left click : Opens image context menu\n" +
//            "    Thumbnail left click : Show as image\n" +
//            "    Thumbnail right click : Opens thumbnail ontext menu\n" +
//            "    Drag&Drop audio : Displays images for the first dropped item\n",
//    notes = "",
//    version = "1.0",
//    year = "2014",
//    group = Widget.Group.OTHER
//)
//public class ImageViewerController extends FXMLController {
//    
//    // gui
//    @FXML AnchorPane entireArea;
//    @FXML private ScrollPane thumb_root;
//    @FXML private TilePane thumb_pane;
//    private final Thumbnail thumbnail = new Thumbnail();
//    private ImageFlowPane layout;
//
//    // state
//    final SimpleObjectProperty<File> folder = new SimpleObjectProperty(null); // current location source (derived from metadata source)
//    final ObservableList<File> images = FXCollections.observableArrayList();
//    private boolean image_reading_lock = false;
//    private int active_image = -1;
//    private FxTimer slideshow;
//    private Subscription dataMonitoring;
//    private Metadata data;
//    
//    // auto applied cnfigurables
//    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
//    public final Accessor<ReadMode> readMode = new Accessor<>(PLAYING, v -> dataMonitoring = Player.bindObservedMetadata(v,dataMonitoring,this::dataChanged));
//    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
//    public final Accessor<Double> thumbSize = new Accessor<>(70d, v -> {
//        thumb_pane.getChildren().forEach(c -> {
//            if(c instanceof Pane)
//                Pane.class.cast(c).setPrefSize(v,v);
//                });
//    });
//    @IsConfig(name = "Thumbnail gap", info = "Spacing between thumbnails")
//    public final Accessor<Double> thumbGap = new Accessor<>(2d, v -> {
//        thumb_pane.setHgap(v);
//        thumb_pane.setVgap(v);
//    });
//    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
//    public final Accessor<Double> slideshow_dur = new Accessor<>(15000d, this::slideshowDur);
//    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
//    public final Accessor<Boolean> slideshow_on = new Accessor<>(true, v -> {
//        if (v) slideshowStart(); else slideshowEnd();
//    });
//    @IsConfig(name = "Show big image", info = "Show thumbnails.")
//    public final Accessor<Boolean> showImage = new Accessor<>(true, this::setImageVisible);
//    @IsConfig(name = "Show thumbnails", info = "Show thumbnails.")
//    public final Accessor<Boolean> showThumbnails = new Accessor<>(true, this::setThumbnailsVisible);
//    
//    // manually applied configurables
//    
//    // non applied configurables
//    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
//    public Boolean changeReadModeOnTransfer = false;
//    @IsConfig(name = "Thumbnail load time", info = "Delay between thumbnail loading. It is not recommended to load all thumbnails immediatelly after each other")
//    public long thumbnailReloadTime = 100l;
//    @IsConfig(name = "Show previous content when empty", info = "Keep showing previous content when the new content is empty.")
//    public boolean keepContentOnEmpty = true;
//    @IsConfig(name = "File search depth", info = "Depth to search to for files in folders.")
//    public int folderTreeDepth = 1;
//    
//    @Override
//    public void init() {
//        //initialize gui
//        thumbnail.setBorderToImage(true);
//        layout = new ImageFlowPane(entireArea,thumbnail);
//        layout.setGap(5);
//        
//        entireArea.getChildren().remove(thumb_root);
//        layout.setMinContentHeight(150);
//        layout.setMinContentWidth(170);
//        layout.addChild(thumb_root);
//        AnchorPane.setBottomAnchor(thumb_root, 0.0);
//        AnchorPane.setTopAnchor(thumb_root, 0.0);
//        AnchorPane.setLeftAnchor(thumb_root, 0.0);
//        AnchorPane.setRightAnchor(thumb_root, 0.0);
//        
//        // refresh if source data changed
//        folder.addListener(locationChange);
//        
//        // accept drag transfer
//        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
//        // handle drag transfers
//        entireArea.setOnDragDropped( e -> {
//            if(DragUtil.hasAudio(e.getDragboard())) {
//                // get first item
//                List<Item> items = DragUtil.getAudioItems(e);
//                // getMetadata, refresh
//                if (!items.isEmpty()) {
//                    // change mode if desired
//                    if (changeReadModeOnTransfer) readMode.setNapplyValue(CUSTOM);
//                    // set data
//                    dataChanged(items.get(0).getMetadata());
//                }
//                // end drag
//                e.setDropCompleted(true);
//                e.consume();
//            }
//        });
//        
//        // consume scroll event to prevent app scroll behavior // optional
//        entireArea.setOnScroll(Event::consume);
//        
//        // show/hide content on cover mouse click
//        thumbnail.getPane().setOnMouseClicked( e -> {
//            if (e.getButton() == PRIMARY) showThumbnails.toggleNapplyValue();
//        });
//    }
//
//    @Override
//    public void OnClosing() {
//        // unbind
//        if (dataMonitoring!=null) dataMonitoring.unsubscribe();
//        folder.removeListener(locationChange);
//    }
//    
///********************************* PUBLIC API *********************************/
// 
//    @Override
//    public void refresh() {
//        image_reading_lock = true; // prevent reading thumbnails twice (see below)
//        // this might change the location, but since we always want to fire
//        // location change, we prevent it here and fire it manually below
//        readMode.applyValue();
//        image_reading_lock = false; // unlock
//        thumbSize.applyValue();
//        thumbGap.applyValue();
//        slideshow_dur.applyValue();
//        slideshow_on.applyValue();
//        showThumbnails.applyValue();
//        readThumbnails();
//    }
//
//    @Override
//    public boolean isEmpty() {
//        return folder.get() == null;
//    }
//    
///****************************** HELPER METHODS ********************************/
//    
//    private void dataChanged(Metadata m) {
//        // remember data
//        data = m;
//        // calculate new location
//        File new_folder = (m==null || !m.isFileBased()) ? null : m.getLocation();
//        // prevent refreshing location if shouldnt
//        if(keepContentOnEmpty && new_folder==null) return;
//        // refresh location
//        folder.set(new_folder);
//     }
//    
//    // using change listener means the event fires only if value really changed
//    // which is important here to avoid reading thumbnails for nothing
//    private final ChangeListener<File> locationChange = (o,ov,nv) -> readThumbnails();
//    
//    private Timeline thumbTimeline = new Timeline();
//    private final Service<Void> thumbReader = new Service() {
//        // service recreates task so it can be restarted
//        @Override protected Task<Void> createTask() {
//            // return newly constructed task
//            return new Task<Void>() {
//                @Override protected Void call() throws Exception {
//                    final List<File> files = FileUtil.getImageFilesRecursive(folder.get(),folderTreeDepth);
//                    SimpleIntegerProperty line = new SimpleIntegerProperty(0);
//                    List<KeyFrame> frames = new ArrayList();
//                    // create zero frame which will not do anything, otherwise
//                    // first file would be left out
//                    frames.add(new KeyFrame(Duration.ZERO, e -> {}, new KeyValue(line,1)));
//                    // add frame per file - each turning file into thumbnail
//                    for(int i=1; i<files.size()+1; i++) {
//                        final int ind = i;
//                        frames.add(new KeyFrame(Duration.millis(i*thumbnailReloadTime), e -> {
//                            File f = files.get(ind-1);
//                            images.add(f);
//                            addThumbnail(f);
//                            if (ind-1== 0 || !layout.hasImage()) setImage(0);
//                        },new KeyValue(line,1)));
//                    }
//                    thumbTimeline = new Timeline(frames.toArray(new KeyFrame[0]));
//                    thumbTimeline.play();
//                    return null;
//                }
//            };
//        }
//    };
//    
//    private void readThumbnails() {
//        if (image_reading_lock) return; 
//         // clear old content before adding new
//        setImage(-1);   // set empty image
//        images.clear();
//        thumb_pane.getChildren().clear();   
//        // ready reading process
//        thumbTimeline.stop();
//        thumbReader.cancel();
//        thumbReader.reset();
//        // read if available
//        if (folder.get() != null)
//            thumbReader.start();
//    }
//    
//    private void addThumbnail(final File image) {
//        Thumbnail t = new Thumbnail(thumbSize.getValue());
//                  t.setBorderToImage(false);
//                  t.setHoverable(true);
//                  t.loadImage(image);
//                  t.getPane().setOnMouseClicked( e -> {
//                      if (e.getButton() == PRIMARY) {
//                          setImage(images.indexOf(image));
//                          e.consume();
//                      }
//                  });
//        thumb_pane.getChildren().add(t.getPane());
//    }
//    
//    private void setImage(int index) {
//        if (images.isEmpty()) index = -1;
//        if (index >= images.size()) index = images.size()-1;
//        
//        if (index == -1) {
//            Image i = null;
//            layout.setImage(i);
//            active_image = -1;
//        } else {
//            layout.setImage(images.get(index));
//            active_image = index;
//        }
//    }
//    
//    private void nextImage() {
//        if (images.size()==1) return;
//        if (images.isEmpty()) {
//            setImage(-1);
//        } else {
//            int index = (active_image > images.size()-2) ? 0 : active_image+1;
//            setImage(index);
//        }
//    }
//    
//    private void slideshowStart() {
//        nextImage();
//        // create if needed
//        if(slideshow==null)
//            slideshow = FxTimer.createPeriodic(Duration.ZERO,this::nextImage);
//        // start up
//        slideshow.restart(Duration.millis(slideshow_dur.getValue()));
//    }
//    
//    private void slideshowEnd() {
//        if (slideshow!=null) slideshow.stop();
//    }
//    
//    private void slideshowDur(double v) {
//        if(slideshow != null && slideshow_on.getValue())
//            slideshow.restart(Duration.millis(v));
//    }
//    
//    private void setImageVisible(boolean v) {
//        layout.setShowImage(v);        
//    }
//    
//    private void setThumbnailsVisible(boolean v) {
//        layout.setShowContent(v);        
//    }
//}