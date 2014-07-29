
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
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import utilities.FileUtil;
import utilities.FxTimer;
import utilities.functional.functor.Procedure;

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
            "    Thumbnail click : Select image\n" +
            "    Image click : Toggle no thumbnails mode on/off\n",
    notes = "",
    version = "0.8",
    year = "2014",
    group = Widget.Group.OTHER
)
public class ImageViewerController extends FXMLController {
    @FXML AnchorPane entireArea;
    @FXML private ScrollPane thumb_root;
    @FXML private TilePane thumb_pane;

    //global variables
    final SimpleObjectProperty<Metadata> meta = new SimpleObjectProperty();   // current metadata source
    final SimpleObjectProperty<File> folder = new SimpleObjectProperty(null); // current location source (derived from metadata source)
    final ObservableList<File> images = FXCollections.observableArrayList();
    ImageFlowPane layout;
    Thread image_reader;
    
    // properties
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public ReadMode readMode = ReadMode.PLAYING;
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "Show content when empty", info = "Keep showing previous content when the widget should be empty.")
    public Boolean keepContentOnEmpty = true;
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
    public double thumbSize = 70.0;
    @IsConfig(name = "Thumbnail gap", info = "Spacing between thumbnails")
    public double thumbGap = 2.0;
    @IsConfig(name = "Thumbnail load time", info = "Delay between thumbnail loading. It is not recommended to load all thumbnails immediatelly after each other")
    public long thumbnailReloadTime = 100l;
    @IsConfig(name = "File search depth", info = "Depth to search to for files in folders.")
    public int folderTreeDepth = 1;
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public double slideshow_dur = 15000l;
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public boolean slideshow_on = true;
    
    @Override
    public void init() {
        //initialize gui
        Thumbnail thumbnail = new Thumbnail();
        layout = new ImageFlowPane(entireArea,thumbnail);
        
        entireArea.getChildren().remove(thumb_root);
        layout.setMinContentHeight(150);
        layout.setMinContentWidth(170);
        layout.addChild(thumb_root);
        AnchorPane.setBottomAnchor(thumb_root, 0.0);
        AnchorPane.setTopAnchor(thumb_root, 0.0);
        AnchorPane.setLeftAnchor(thumb_root, 0.0);
        AnchorPane.setRightAnchor(thumb_root, 0.0);
        
        // refresh if source data changed
        meta.addListener(metaChange);
        folder.addListener(locationChange);
        
        
        // accept drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle drag transfers
        entireArea.setOnDragDropped( e -> {
            // get first item
            List<Item> items = DragUtil.getAudioItems(e);
            Item item = items.isEmpty() ? null : items.get(0);
            // getMetadata, refresh
            if (item != null) {
                if (changeReadModeOnTransfer) {
                    readMode = ReadMode.CUSTOM;
                    refresh();  // rebinds read mode, doesnt refresh content in this case
                }
                meta.set(item.toMetadata()); // refresh
            }
            // end drag
            e.setDropCompleted(true);
            e.consume();
        });
        
        // consume scroll event to prevent app scroll behavior // optional
        entireArea.setOnScroll(Event::consume);
        
        // show/hide content on cover mouse click
        thumbnail.getPane().setOnMouseClicked( e -> {
            if (e.getButton() == PRIMARY) layout.toggleShowContent();
        });
    }
    
    @Override
    public void refresh() {
        Player.bindObservedMetadata(meta, readMode);        
        
        thumb_pane.setHgap(thumbGap);
        thumb_pane.setVgap(thumbGap);
        
        if (slideshow_on) slideshowStart();
        else slideshowEnd();
    }

    @Override
    public void OnClosing() {
        // unbind
        meta.unbind();
        meta.removeListener(metaChange);
        folder.removeListener(locationChange);
    }
    
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
            if(nv==null) {
                readThumbnails();
                setImage(-1);   // set empty image
            } else {
                readThumbnails();
            }
        };
    
/********************************** THUMBNAILS ********************************/
    
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
        Thumbnail t = new Thumbnail(thumbSize);
                  t.setBorderToImage(false);
                  t.setHoverable(true);
                  t.loadImage(image);
                  t.getPane().setOnMouseClicked( e -> {
                      if (e.getButton() == PRIMARY) {
                          setImage(images.indexOf(image));
                          slideshowStart();
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
    
/********************************** SLIDESHOW *********************************/
    
    private int active_image = -1;
    FxTimer slideshow;
    
    public void slideshowStart() {
        // create if needed
        if(slideshow==null) {
            Procedure nextImage = () -> {
                if (images.size()==1) return;
                if (!images.isEmpty()) {
                    int index = (active_image > images.size()-2) ? 0 : active_image+1;
                    setImage(index);
                }
            };
            slideshow = FxTimer.createPeriodic(Duration.ZERO,nextImage);
        }
        // start up
        slideshow.restart(Duration.millis(slideshow_dur));
    }
    
    public void slideshowEnd() {
        if (slideshow!=null) slideshow.stop();
    }
}