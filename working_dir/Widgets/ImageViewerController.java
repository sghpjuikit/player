
import AudioPlayer.Player;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.SimpleItem;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.Panes.ImageFlowPane;
import GUI.objects.Thumbnail;
import Layout.WidgetController;
import utilities.functional.functor.Procedure;
import PseudoObjects.ReadMode;
import java.io.File;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.TilePane;
import javafx.util.Duration;
import utilities.FileUtil;
import utilities.Timer;


public class ImageViewerController extends WidgetController {
    @FXML AnchorPane entireArea;
    @FXML private ScrollPane thumb_root;
    @FXML private TilePane thumb_pane;

    //global variables
    SimpleObjectProperty<Metadata> meta = new SimpleObjectProperty<>();
    File folder = null;
    ObservableList<File> images = FXCollections.observableArrayList();
    ImageFlowPane layout;
    Thread image_reader;
    
    private int active_image = -1;
    private final Procedure nextImage = () -> {
        if (!images.isEmpty()) {
            int index = (active_image > images.size()-2) ? 0 : active_image+1;
            setImage(index);
        }
    };
    Timer slideshow = new Timer("Image slideshow", Duration.millis(15000), nextImage, true);
    
    // properties
    @IsConfig(name = "Read Mode", info = "Source of data for the widget.")
    public ReadMode readMode = ReadMode.CUSTOM;
    @IsConfig(name = "Read mode change on drag", info = "Change read mode to CUSTOM when data are arbitrary added to widget.")
    public Boolean changeReadModeOnTransfer = false;
    @IsConfig(name = "Thumbnail size", info = "Size of the thumbnails.")
    public double thumbSize = 50.0;
    @IsConfig(name = "Thumbnail gap", info = "Spacing between thumbnails")
    public double thumbGap = 2.0;
    @IsConfig(name = "Thumbnail load time", info = "Delay between thumbnail loading. It is not recommended to load all thumbnails immediatelly after each other")
    public long thumbnailReloadTime = 200l;
    @IsConfig(name = "File search depth", info = "Depth to search to for files in folders.")
    public int folderTreeDepth = 1;
    @IsConfig(name = "Slideshow reload time", info = "Time between picture change.")
    public long slideshow_dur = 15000l;
    @IsConfig(name = "Slideshow", info = "Turn sldideshow on/off.")
    public boolean slideshow_on = true;
    
    @Override
    public void initialize() {
        //initialize gui
        layout = new ImageFlowPane(entireArea, new Thumbnail());
        layout.setPadding(5);
        
        entireArea.getChildren().remove(thumb_root);
        layout.minContentHeightProperty().bind(thumb_root.heightProperty());
        layout.minContentWidthProperty().bind(thumb_root.widthProperty());
        layout.addChild(thumb_root);
        AnchorPane.setBottomAnchor(thumb_root, 0.0);
        AnchorPane.setTopAnchor(thumb_root, 0.0);
        AnchorPane.setLeftAnchor(thumb_root, 0.0);
        AnchorPane.setRightAnchor(thumb_root, 0.0);
        
        // refresh folder if metadata source data changed
        meta.addListener((ObservableValue<? extends Metadata> ov, Metadata t, Metadata t1) -> {
            refresh();
        });
        // populate thumbnails on list change
        images.addListener((Change<? extends File> change) -> {
            while(change.next()){
                if(change.wasAdded()) {
                    for(File f: change.getAddedSubList()) {
                        addThumbnail(f);
                        if (images.indexOf(f) == 0 || !layout.hasImage())
                            setImage(0);
                    }
                } else {
                    thumb_pane.getChildren().clear();
                    for(File f: images) {
                        addThumbnail(f);
                        if (images.indexOf(f) == 0 || !layout.hasImage())
                            setImage(0);
                    }
                }
            }
        });
        
        // accept drag transfer
        entireArea.setOnDragOver((DragEvent t) -> {
            Dragboard db = t.getDragboard();
            if (db.hasFiles() || db.hasContent(DragUtil.playlist)) {
                t.acceptTransferModes(TransferMode.ANY);
            }
            t.consume();
        });
        // handle drag transfers
        entireArea.setOnDragDropped((DragEvent t) -> {
            Dragboard db = t.getDragboard();
            Item item = null;
            // get first item
            if (db.hasFiles()) {
                item = FileUtil.getAudioFiles(db.getFiles(), 1)
                        .stream().limit(1).map(SimpleItem::new).findAny().get();
            } else if (db.hasContent(DragUtil.playlist)) {
                Playlist pl = DragUtil.getPlaylist(db);
                item = pl.getItem(0);
            } else if (db.hasContent(DragUtil.items)) {
                List<Item> pl = DragUtil.getItems(db);
                item = pl.get(0);
            }
            // getMetadata, refresh
            if (item != null) {
                if (changeReadModeOnTransfer)
                    readMode = ReadMode.CUSTOM;
                Player.bindObservedMetadata(meta, readMode);
                
                folder = item.getFolder();
                readThumbnails();
            }
            // end drag
            t.consume();
        });
    }
    
    @Override
    public void refresh() {
        Player.bindObservedMetadata(meta, readMode);
        if (meta.get() == null) return;
        if (!meta.get().getFolder().equals(folder)) {
            folder = meta.get().getFolder();
            readThumbnails();
        }
        thumb_pane.setHgap(thumbGap);
        thumb_pane.setVgap(thumbGap);
        slideshow.setPeriod(slideshow_dur);
        if (slideshow_on)
            slideshowStart();
        else
            slideshowEnd();
    }
    
/********************************** THUMBNAILS ********************************/
    
    private void readThumbnails() {
        if (folder == null) return;
        
        // clear old before adding new
        images.clear();
        thumb_pane.getChildren().clear();
        // run separate image_reader to get images
        final Task<Void> task = new Task<Void>() {
            @Override protected Void call() throws Exception {
                for(final File f: FileUtil.getImageFilesRecursive(folder,folderTreeDepth)) {
                    Thread.sleep(thumbnailReloadTime);
                    if (image_reader.isInterrupted()) return null;
                    Platform.runLater(() -> images.add(f));
                }
                return null;
            }
        };
        if (image_reader != null) image_reader.interrupt(); // this handled very dangerous problem very conveniently
        image_reader = new Thread(task);
        image_reader.setDaemon(true);
        image_reader.start();
    }
    
    private void addThumbnail(final File image) {
        Thumbnail t = new Thumbnail(thumbSize);
                  t.setBorderToImage(false);
                  t.setHoverable(true);
                  t.loadImage(image);
                  t.getPane().setOnMouseClicked((MouseEvent event) -> {
                      if (event.getButton() == MouseButton.PRIMARY)
                          setImage(images.indexOf(image));
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
    
    public void slideshowStart() {
        slideshow.start();
    }
    public void slideshowEnd() {
        slideshow.stop();
    }
    public boolean slideshowRunning() {
        return slideshow.isRunning();
    }
}