package PlayerControls;

import AudioPlayer.ItemChangeEvent.ItemChangeHandler;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector.LoopMode;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.objects.Balancer.Balancer;
import GUI.objects.Seeker;
import Layout.Widgets.FXMLController;
import java.io.File;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import utilities.FileUtil;
import utilities.Util;

/**
 * Playback Controller class
 * <p>
 * @author uranium
 */
public class PlayerControlsController extends FXMLController {
    @FXML ImageView openFile;
    @FXML AnchorPane entireArea;
    @FXML BorderPane controlPanel;
    @FXML ImageView revind;
    @FXML ImageView previous;
    @FXML ImageView play;    
    @FXML ImageView next;
    @FXML ImageView forward;
    @FXML GridPane soundGrid;
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
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    public boolean playDropped = false;
    
    @Override
    public void init() {
        // create balancer
        balance = new Balancer();
        soundGrid.add(balance, 1, 1);
        balance.setPrefSize(50,20);
        
        balance.setMax(PLAYBACK.getBalanceMax());
        balance.setMin(PLAYBACK.getBalanceMin());
        balance.balanceProperty().bindBidirectional(PLAYBACK.balanceProperty());
        
        volume.setMin(PLAYBACK.getVolumeMin());
        volume.setMax(PLAYBACK.getVolumeMax());
        volume.setValue(PLAYBACK.getVolume());
        volume.valueProperty().bindBidirectional(PLAYBACK.volumeProperty());
        
        seeker = new Seeker();
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        entireArea.getChildren().add(seeker);
        AnchorPane.setBottomAnchor(seeker, 0.0);
        AnchorPane.setLeftAnchor(seeker, 0.0);
        AnchorPane.setRightAnchor(seeker, 0.0);
        
        // load resources
        pauseImg   = new Image(getResource("pause.png").toURI().toString());
        playImg    = new Image(getResource("play.png").toURI().toString());
        loopOFFImg = new Image(getResource("loopOFF.png").toURI().toString());
        loopALLImg = new Image(getResource("loopALL.png").toURI().toString());
        loopONEImg = new Image(getResource("loopONE.png").toURI().toString());
        muteOFFImg = new Image(getResource("muteOFF.png").toURI().toString());
        muteONImg  = new Image(getResource("muteON.png").toURI().toString());
        
        // set updating + initialize manually
        Player.addOnItemUpdate(playbackItemChanged);                    // add listener
        playingItemChanged(Player.getCurrentMetadata());                // init value
        
        PLAYBACK.statusProperty().addListener(statusListener);          // add listener
        statusChanged(PLAYBACK.getStatus());                            // init value
        
        PLAYBACK.loopModeProperty().addListener(loopModeListener);      // add listener
        loopModeChanged(PLAYBACK.getLoopMode());                        // init value
                
        PLAYBACK.muteProperty().addListener(muteListener);              // add listener
        muteChanged(PLAYBACK.getMute());                                // init value
        
        PLAYBACK.totalTimeProperty().addListener(totalTimeListener);    // add listener
        PLAYBACK.realTimeProperty().addListener(realTimeListener);      // add listener        
        PLAYBACK.currentTimeProperty().addListener(currTimeListener);   // add listener
        currTimeListener.invalidated(null);                             // init value
        
        // support drag transfer
        entireArea.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if ((db.hasFiles() && FileUtil.hasAudioFiles(db.getFiles())) ||
                    db.hasUrl() || 
                        db.hasContent(DragUtil.playlist)) {
                e.acceptTransferModes(TransferMode.ANY);
                e.consume();
            }
        });
        entireArea.setOnDragDropped( e -> {
            // get items
            Dragboard db = e.getDragboard();
            Playlist p = new Playlist();
            if (db.hasFiles())
                p.addFiles(db.getFiles());
            if (db.hasUrl())
                p.addUrl(db.getUrl());
            if (db.hasContent(DragUtil.playlist))
                p.addItems(DragUtil.getPlaylist(db).getItems());
            
            // handle items
            if(playDropped) PlaylistManager.playPlaylist(p);
            else PlaylistManager.addPlaylist(p);
            // end drag
            e.setDropCompleted(true);
            e.consume();
        });
        
    }
    
    @Override
    public void refresh() {
        seeker.setChaptersShowPopUp(popupChapters);
        seeker.setChaptersVisible(showChapters);
    }

    @Override
    public void OnClosing() {
        // remove listeners
        Player.remOnItemUpdate(playbackItemChanged);                    
        PLAYBACK.statusProperty().removeListener(statusListener);       
        PLAYBACK.loopModeProperty().removeListener(loopModeListener);   
        PLAYBACK.muteProperty().removeListener(muteListener);           
        PLAYBACK.totalTimeProperty().removeListener(totalTimeListener); 
        PLAYBACK.realTimeProperty().removeListener(realTimeListener);           
        PLAYBACK.currentTimeProperty().removeListener(currTimeListener);
        // unbind
        balance.balanceProperty().unbind();
        volume.valueProperty().unbind();
        seeker.unbindTime();
    }
    
/******************************************************************************/
    
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
        currentTimeChanged();
    }
    
    @FXML private void consumeMouseEvent(MouseEvent event) {
        event.consume(); // for example to prevent dragging application on some areas
    }
    
/******************************************************************************/
    
    private final ItemChangeHandler<Metadata> playbackItemChanged = (oldV,newV)-> playingItemChanged(newV);
    private final ChangeListener<Status> statusListener = (o,oldV,newV)-> statusChanged(newV);
    private final ChangeListener<LoopMode> loopModeListener = (o,oldV,newV)-> loopModeChanged(newV);
    private final ChangeListener<Boolean> muteListener = (o,oldV,newV)-> muteChanged(newV);
    private final InvalidationListener currTimeListener = o -> currentTimeChanged();
    private final InvalidationListener realTimeListener = o -> realTime.setText(Util.formatDuration(PLAYBACK.getRealTime()));
    private final InvalidationListener totalTimeListener = o -> totTime.setText(Util.formatDuration(PLAYBACK.getTotalTime()));       
    
    private void playingItemChanged(Metadata m) {
        if(m!=null){
            titleL.setText(m.getTitle());
            artistL.setText(m.getArtist());
            bitrateL.setText(m.getBitrate().toString());
            sampleRateL.setText(m.getSampleRate());
            channelsL.setText(m.getChannels());
        }
        seeker.reloadChapters(m);
    }
    private void statusChanged(Status newStatus) {
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
    private void loopModeChanged(LoopMode new_mode) {
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
    private void muteChanged(boolean val) {
        if (val) {
            mute.setImage(muteOFFImg);
        } else {
            mute.setImage(muteONImg);
        }
    }
    private void currentTimeChanged() {
        // update label
        if (elapsedTime) {
            Duration elapsed = PLAYBACK.getCurrentTime();
            currTime.setText(Util.formatDuration(elapsed));  
        } else {
            if (PLAYBACK.getTotalTime() == null) return;
            Duration remaining = PLAYBACK.getRemainingTime();
            currTime.setText("- " + Util.formatDuration(remaining)); 
        }
    }
    
}