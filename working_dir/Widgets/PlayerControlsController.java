
import AudioPlayer.Player;
import AudioPlayer.playback.LoopMode;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.Configuration;
import Configuration.IsConfig;
import GUI.objects.Balancer.Balancer;
import GUI.objects.Seeker;
import Layout.WidgetController;
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
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import utilities.Util;

/**
 * FXML Controller class
 *
 * @author uranium
 */
public class PlayerControlsController extends WidgetController {
    @FXML ImageView openFile;
    @FXML AnchorPane entireArea;
    @FXML BorderPane controlPanel;
    @FXML ImageView revind;
    @FXML ImageView previous;
    @FXML ImageView play;    
    @FXML ImageView next;
    @FXML ImageView forward;
    @FXML Slider volume;
    @FXML Balancer balance;
    Seeker seeker;
    @FXML Label currTime;
    @FXML Label totTime;
    @FXML Label realTime;
    @FXML Label status;
    @FXML ImageView loopMode;
    @FXML ImageView mute;
    
    @FXML Label titleL;
    @FXML Label artistL;
    @FXML Label bitrateL;
    @FXML Label sampleRateL;
    @FXML Label channelsL;
    
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
        balance.setMax(PLAYBACK.getBalanceMax());
        balance.setMin(PLAYBACK.getBalanceMin());
        balance.balanceProperty().bindBidirectional(PLAYBACK.balanceProperty());
        
        volume.setMin(PLAYBACK.getVolumeMin());
        volume.setMax(PLAYBACK.getVolumeMax());
        volume.setValue(PLAYBACK.getVolume());
        volume.valueProperty().bindBidirectional(PLAYBACK.volumeProperty());
        
        seeker = new Seeker();
        entireArea.getChildren().add(seeker);
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
        
        PLAYBACK.statusProperty().addListener( o -> 
            updateStatus(PLAYBACK.getStatus())
        );
        updateStatus(PLAYBACK.getStatus());
        
        PLAYBACK.loopModeProperty().addListener( o -> 
            updateLoopMode(PLAYBACK.getLoopMode())
        );
        updateLoopMode(PLAYBACK.getLoopMode());
                
        PLAYBACK.totalTimeProperty().addListener( o -> 
            totTime.setText(Util.formatDuration(PLAYBACK.getTotalTime()))
        );
        
        PLAYBACK.realTimeProperty().addListener( o -> 
            realTime.setText(Util.formatDuration(PLAYBACK.getRealTime()))
        );
        
        PLAYBACK.currentTimeProperty().addListener( o -> {
            if (seeker.canUpdate)
                seeker.updatePosition();
            updateTime();
        });

        PLAYBACK.muteProperty().addListener( o -> 
                updateMute(PLAYBACK.getMute())
        );
        updateMute(PLAYBACK.getMute());
        
        // support drag transfer
        entireArea.setOnDragOver((DragEvent event) -> {
            Dragboard db = event.getDragboard();
            if(db.hasFiles() || db.hasUrl())
                event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        });
        entireArea.setOnDragDropped((DragEvent event) -> {
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
    @FXML
    private void play_pause() {
         PLAYBACK.pause_resume();
    }
    
    @FXML
    private void stop() {
         PLAYBACK.stop();
    }
    
    @FXML
    private void next() {
        PlaylistManager.playNextItem();
    }
    
    @FXML
    private void previous() {
        PlaylistManager.playPreviousItem();
    }
    
    @FXML
    private void forward() {
        PLAYBACK.seekForward();
    }
    
    @FXML
    private void rewind() {
        PLAYBACK.seekBackward();
    }
    
    @FXML
    private void cycleLoopMode() {
        PLAYBACK.toggleLoopMode();
    }
    
    @FXML
    private void cycleMute() {
        PLAYBACK.toggleMute();
    }
    
    @FXML
    private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        updateTime();
    }
    
    @FXML
    private void consumeMouseEvent(MouseEvent event) {
        event.consume(); // for example to prevent dragging application on some areas
    }
    
    private void updateItem(Metadata m) {
        if(m!=null){
            titleL.setText(m.getTitle());
            artistL.setText(m.getArtist());
            bitrateL.setText(m.getBitrate().toString());
            sampleRateL.setText(m.getSampleRate());
            channelsL.setText(m.getChannels());
        }
    }
    private void updateStatus(Status newStatus) {
        if (newStatus == null || newStatus == Status.UNKNOWN ) {
            controlPanel.setDisable(true);
            status.setText("Buffering");
            seeker.setDisable(true);
        } else {
            controlPanel.setDisable(false);
            seeker.setDisable(false);
            status.setText(newStatus.toString()); 

            if (newStatus == Status.PLAYING) {
                play.setImage(pauseImg);
            } else {
                play.setImage(playImg);
            }
        }
    }
    private void updateLoopMode(LoopMode new_mode) {
        switch (new_mode) {
            case OFF:       loopMode.setImage(loopOFFImg);
                            loopMode.setOpacity(0.3);
                            break;
            case PLAYLIST:  loopMode.setImage(loopALLImg);
                            loopMode.setOpacity(0.6);
                            break;
            case SONG:      loopMode.setImage(loopONEImg);
                            loopMode.setOpacity(0.6);            
                            break;
            default:
        }
    }
    private void updateMute(boolean val) {
        if (val) {
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
            if (PLAYBACK.getTotalTime() == null) {return;}
            Duration remaining = PLAYBACK.getRemainingTime();
            currTime.setText("- " + Util.formatDuration(remaining)); 
        }
    }
    
}