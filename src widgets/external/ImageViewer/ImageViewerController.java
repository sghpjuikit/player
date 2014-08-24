
package ImageViewer;


import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.Panes.ImageFlowPane;
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
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
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
import javafx.scene.layout.Pane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import utilities.FileUtil;
import utilities.FxTimer;
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
            "    Image left click : Toggles show thumbnails\n" +
            "    Image left click : Opens image context menu\n" +
            "    Thumbnail left click : Show as image\n" +
            "    Thumbnail right click : Opens thumbnail ontext menu\n" +
            "    Drag&Drop audio : Displays images for the first dropped item\n",
    notes = "",
    version = "1.0",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ImageViewerController extends FXMLController {
    
    // gui
    @FXML AnchorPane entireArea;
    @FXML private ScrollPane thumb_root;
    @FXML private TilePane thumb_pane;
    private final Thumbnail thumbnail = new Thumbnail();
    private ImageFlowPane layout;

    // non configurables
    final SimpleObjectProperty<Metadata> data = new SimpleObjectProperty();   // current metadata source
    final SimpleObjectProperty<File> folder = new SimpleObjectProperty(null); // current location source (derived from metadata source)
    final ObservableList<File> images = FXCollections.observableArrayList();
    private boolean image_reading_lock = false;
    private int active_image = -1;
    private FxTimer slideshow;
    
    // auto applied cnfigurables
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public final Accessor<ReadMode> readMode = new Accessor<>(PLAYING, v -> Player.bindObservedMetadata(data,v));
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
    public final Accessor<Double> thumbSize = new Accessor<>(70d, v -> {
        thumb_pane.getChildren().forEach(c -> {
            if(c instanceof Pane)
                Pane.class.cast(c).setPrefSize(v,v);
                });
    });
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
    @IsConfig(name = "Show thumbnails", info = "Show thumbnails.")
    public final Accessor<Boolean> showThumbnails = new Accessor<>(true, this::setImageVisible);
    
    // manually applied configurables
    
    // non applied configurables
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "Thumbnail load time", info = "Delay between thumbnail loading. It is not recommended to load all thumbnails immediatelly after each other")
    public long thumbnailReloadTime = 100l;
    @IsConfig(name = "Show previous content when empty", info = "Keep showing previous content when the new content is empty.")
    public boolean keepContentOnEmpty = true;
    @IsConfig(name = "File search depth", info = "Depth to search to for files in folders.")
    public int folderTreeDepth = 1;
    
    @Override
    public void init() {
        //initialize gui
        thumbnail.setBorderToImage(true);
        layout = new ImageFlowPane(entireArea,thumbnail);
        layout.setGap(5);
        
        entireArea.getChildren().remove(thumb_root);
        layout.setMinContentHeight(150);
        layout.setMinContentWidth(170);
        layout.addChild(thumb_root);
        AnchorPane.setBottomAnchor(thumb_root, 0.0);
        AnchorPane.setTopAnchor(thumb_root, 0.0);
        AnchorPane.setLeftAnchor(thumb_root, 0.0);
        AnchorPane.setRightAnchor(thumb_root, 0.0);
        
        // refresh if source data changed
        data.addListener(metaChange);
        folder.addListener(locationChange);
        
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped( e -> {
            // get first item
            List<Item> items = DragUtil.getAudioItems(e);
            // getMetadata, refresh
            if (!items.isEmpty()) {
                // change mode if desired
                if (changeReadModeOnTransfer) readMode.setNapplyValue(CUSTOM);
                // set data
                data.set(items.get(0).getMetadata());
            }
            // end drag
            e.setDropCompleted(true);
            e.consume();
        });
        
        // consume scroll event to prevent app scroll behavior // optional
        entireArea.setOnScroll(Event::consume);
        
        // show/hide content on cover mouse click
        thumbnail.getPane().setOnMouseClicked( e -> {
            if (e.getButton() == PRIMARY) showThumbnails.toggleNapplyValue();
        });
    }

    @Override
    public void OnClosing() {
        // unbind
        data.unbind();
        data.removeListener(metaChange);
        folder.removeListener(locationChange);
    }
    
/********************************* PUBLIC API *********************************/
 
    @Override
    public void refresh() {
        image_reading_lock = true; // prevent reading thumbnails twice (see below)
        readMode.applyValue();
        thumbSize.applyValue();
        thumbGap.applyValue();
        slideshow_dur.applyValue();
        slideshow_on.applyValue();
        showThumbnails.applyValue();
        // we need to fire this too, althout at first sight not necessary, it is
        // the location content might change, we must guarantee that the only
        // means to display the change, user refreshing widget manually' works
        image_reading_lock = false; // unlock
        readThumbnails(); // make sure
    }

    @Override
    public boolean isEmpty() {
        return data.get() == null;
    }
    
/****************************** HELPER METHODS ********************************/
    
    private final ChangeListener<Metadata> metaChange = (o,ov,nv) -> {
            // calculate new location
            File new_folder = (nv==null || !nv.isFileBased()) 
                    ? null
                    : nv.getLocation();
            // prevent refreshing location if shouldnt
            if(keepContentOnEmpty && new_folder==null) return;
            // refresh location
            folder.set(new_folder);
        };
    private final ChangeListener<File> locationChange = (o,ov,nv) -> {
            setImage(-1);   // set empty image
            readThumbnails();
        };
    
    private Timeline thumbTimeline = new Timeline();
    private final Service<Void> thumbReader = new Service() {
        // service recreates task so it can be restarted
        @Override protected Task<Void> createTask() {
            // return newly constructed task
            return new Task<Void>() {
                @Override protected Void call() throws Exception {
                    final List<File> files = FileUtil.getImageFilesRecursive(folder.get(),folderTreeDepth);
                    SimpleIntegerProperty line = new SimpleIntegerProperty(0);
                    List<KeyFrame> frames = new ArrayList();
                    // create zero frame which will not do anything, otherwise
                    // first file would be left out
                    frames.add(new KeyFrame(Duration.ZERO, e -> {}, new KeyValue(line,1)));
                    // add frame per file - each turning file into thumbnail
                    for(int i=1; i<files.size()+1; i++) {
                        final int ind = i;
                        frames.add(new KeyFrame(Duration.millis(i*thumbnailReloadTime), e -> {
                            File f = files.get(ind-1);
                            images.add(f);
                            addThumbnail(f);
                            if (ind-1== 0 || !layout.hasImage()) setImage(0);
                        },new KeyValue(line,1)));
                    }
                    thumbTimeline = new Timeline(frames.toArray(new KeyFrame[0]));
                    thumbTimeline.play();
                    return null;
                }
            };
        }
    };
    
    private void readThumbnails() {
        if (image_reading_lock) return; 
         // clear old content before adding new
        images.clear();
        thumb_pane.getChildren().clear();   
        // ready reading process
        thumbTimeline.stop();
        thumbReader.cancel();
        thumbReader.reset();
        // read if available
        if (folder.get() != null)
            thumbReader.start();
    }
    
    private void addThumbnail(final File image) {
        Thumbnail t = new Thumbnail(thumbSize.getValue());
                  t.setBorderToImage(false);
                  t.setHoverable(true);
                  t.loadImage(image);
                  t.getPane().setOnMouseClicked( e -> {
                      if (e.getButton() == PRIMARY) {
                          setImage(images.indexOf(image));
                          e.consume();
                      }
                  });
        thumb_pane.getChildren().add(t.getPane());
    }
    
    private void setImage(int index) {
        if (images.isEmpty()) index = -1;
        if (index >= images.size()) index = images.size()-1;
        
        if (index == -1) {
            Image i = null;
            layout.setImage(i);
            active_image = -1;
        } else {
            layout.setImage(images.get(index));
            active_image = index;
        }
    }
    
    private void nextImage() {
        if (images.size()==1) return;
        if (images.isEmpty()) {
            setImage(-1);
        } else {
            int index = (active_image > images.size()-2) ? 0 : active_image+1;
            setImage(index);
        }
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
        layout.setShowImage(v);        
    }
}