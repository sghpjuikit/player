
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.WindowManager;
import GUI.objects.Seeker;
import Layout.WidgetController;
import Layout.WidgetInfo;
import java.io.File;
import java.net.URI;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import main.App;
import utilities.Util;

/**
 * FXML Controller class
 *
 * @author uranium
 */
@WidgetInfo(name = "Tiny")
public class PlayerControlsTinyController extends WidgetController {
    
    @FXML AnchorPane root;
    @FXML BorderPane seekerPane;
    @FXML ImageView revind;
    @FXML ImageView previous;
    @FXML ImageView play;    
    @FXML ImageView next;
    @FXML ImageView forward;
    @FXML Slider volume;
    Seeker seeker;
    @FXML Label currTime;
    @FXML ImageView mute;
    @FXML Label titleL;
    @FXML Label artistL;
    
    Image pauseImg;
    Image playImg;
    Image loopOFFImg;
    Image loopALLImg;
    Image loopONEImg;
    Image muteOFFImg;
    Image muteONImg;
    
    // properties
    @IsConfig(name = "Show chapters", info = "Display chapter marks on seeker.")
    public boolean showChapters = true;
    @IsConfig(name = "Show info for chapters", info = "Display pop up information for chapter marks on seeker.")
    public boolean popupChapters = true;
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    public boolean elapsedTime = true;
    
    @Override
    public void initialize() {
        
        volume.setMin(PLAYBACK.getVolumeMin());
        volume.setMax(PLAYBACK.getVolumeMax());
        volume.setValue(PLAYBACK.getVolume());
        volume.valueProperty().bindBidirectional(PLAYBACK.volumeProperty());
        
        seeker = new Seeker();
        seeker.prefWidthProperty().bind(seekerPane.widthProperty());
        seekerPane.setCenter(seeker);
        AnchorPane.setBottomAnchor(seeker, 0.0);
        AnchorPane.setLeftAnchor(seeker, 0.0);
        AnchorPane.setRightAnchor(seeker, 0.0);
        
        pauseImg   = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "pause.png").toURI().toString());
        playImg    = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "play.png").toURI().toString());
        loopOFFImg = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "loopOFF.png").toURI().toString());
        loopALLImg = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "loopALL.png").toURI().toString());
        loopONEImg = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "loopONE.png").toURI().toString());
        muteOFFImg = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "muteOFF.png").toURI().toString());
        muteONImg  = new Image(new File(Configuration.WIDGET_FOLDER + File.separator + "muteON.png").toURI().toString());
        
        // set updating + initialize manually
        Player.addOnItemUpdate( metadata -> {
            updateItem(metadata);
            seeker.reloadChapters();
        });
        updateItem(Player.getCurrentMetadata());
        seeker.reloadChapters();
        
        PLAYBACK.muteProperty().addListener( o -> 
                updateMute(PLAYBACK.getMute())
        );
        updateMute(PLAYBACK.getMute()); // initialize value
        
        PLAYBACK.currentTimeProperty().addListener( o -> {
                if (seeker.canUpdate)
                    seeker.updatePosition();
                updateTime();
        });
        
        // support drag transfer
        root.setOnDragOver((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            if(db.hasFiles() || db.hasUrl())
                event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
        root.setOnDragDropped((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            URI uri = null;
            if (db.hasFiles())
                uri = db.getFiles().get(0).toURI();
            else if (db.hasUrl())
                uri = URI.create(db.getUrl());
            if (uri != null)
                playFile(new File(uri));
        });

    }
    
    @Override
    public void refresh() {
        seeker.setChaptersPopUp(popupChapters);
        seeker.setChaptersVisible(showChapters);
    }
    
    private void playFile(File file) {
         PlaylistManager.addUri(file.toURI());
         PlaylistManager.playLastItem();
    }
    @FXML private void play_pause() {
         PLAYBACK.pause_resume();
    }
    
    @FXML private void stop() {
         PLAYBACK.stop();
    }
    
    @FXML private void next() {
        PlaylistManager.playNextItem();
    }
    
    @FXML private void previous() {
        PlaylistManager.playPreviousItem();
    }
    
    @FXML private void forward() {
        PLAYBACK.seekForward();
    }
    
    @FXML private void rewind() {
        PLAYBACK.seekBackward();
    }
    
    @FXML private void cycleLoopMode() {
        PLAYBACK.toggleLoopMode();
    }
    
    @FXML private void cycleMute() {
        PLAYBACK.toggleMute();
    }
    
    @FXML private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        updateTime();
    }
    
    @FXML
    private void consumeMouseEvent(MouseEvent event) {
        event.consume(); // for example to prevent dragging application on some areas
    }
    
    private void updateItem(Metadata m) {
        if(m!=null) {
            titleL.setText(m.getTitle());
            artistL.setText(m.getArtist());
        }
    }
    private void updateMute(boolean new_mode) {
        if (new_mode) {
            mute.setImage(muteOFFImg);
        } else {
            mute.setImage(muteONImg);
        }
    }
    private void updateTime() {
        if (elapsedTime) {
            Duration elapsed = PLAYBACK.getCurrentTime();
            currTime.setText(Util.formatDuration(elapsed));  
        } else {
            if (PLAYBACK.getTotalTime() == null) return;
            Duration remaining = PLAYBACK.getRemainingTime();
            currTime.setText("- " + Util.formatDuration(remaining)); 
        }
    }
    
    
    
    
    @FXML public void toggleMini() {
        WindowManager.toggleMini();
    }
    @FXML public void closeApp() {
        App.getInstance().close();
    }
}