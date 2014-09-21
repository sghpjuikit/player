package PlayerControlsTiny;


import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.Seeker;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.PlaybackFeature;
import Layout.Widgets.Widget;
import java.io.File;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.UNKNOWN;
import javafx.util.Duration;
import org.reactfx.Subscription;
import util.Util;
import util.access.Accessor;

/**
 * Playback Controller class
 * <p>
 * @author uranium
 */
@Widget.Info(name = "Tiny")
public class PlayerControlsTinyController extends FXMLController implements PlaybackFeature {
    
    @FXML AnchorPane root;
    @FXML BorderPane seekerPane;
    @FXML ImageView revind;
    @FXML ImageView previous;
    @FXML ImageView play;    
    @FXML ImageView next;
    @FXML ImageView forward;
    @FXML Slider volume;
    Seeker seeker = new Seeker();
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
    public final Accessor<Boolean> showChapters = new Accessor<>(true, seeker::setChaptersVisible);
    @IsConfig(name = "Show info for chapters", info = "Display pop up information for chapter marks on seeker.")
    public final Accessor<Boolean> popupChapters = new Accessor<>(true, seeker::setChaptersShowPopUp);
    @IsConfig(name = "Snap seeker to chapters on drag", info = "Enable snapping to chapters during dragging.")
    public final Accessor<Boolean> snapToChap = new Accessor<>(true, seeker::setSnapToChapters);
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    public boolean elapsedTime = true;
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    public boolean playDropped = false;
    
    @Override
    public void init() {
        
        volume.setMin(PLAYBACK.getVolumeMin());
        volume.setMax(PLAYBACK.getVolumeMax());
        volume.setValue(PLAYBACK.getVolume());
        volume.valueProperty().bindBidirectional(PLAYBACK.volumeProperty());
        
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        seeker.setChapterSnapDistance(GUI.snapDistance);
        seekerPane.setCenter(seeker);
//        BorderPane.setAlignment(seeker, Pos.CENTER);
        
        // load resources
        pauseImg   = new Image(getResource("pause.png").toURI().toString());
        playImg    = new Image(getResource("play.png").toURI().toString());
        loopOFFImg = new Image(getResource("loopOFF.png").toURI().toString());
        loopALLImg = new Image(getResource("loopALL.png").toURI().toString());
        loopONEImg = new Image(getResource("loopONE.png").toURI().toString());
        muteOFFImg = new Image(getResource("muteOFF.png").toURI().toString());
        muteONImg  = new Image(getResource("muteON.png").toURI().toString());
        
        
        // set updating + initialize manually
        playingItemMonitoring = Player.playingtem.subscribeToUpdates(this::playbackItemChanged);  // add listener
        playbackItemChanged(Player.playingtem.get());            // init value
        
        PLAYBACK.statusProperty().addListener(statusListener);          // add listener
        statusChanged(PLAYBACK.getStatus());                            // init value
        
        PLAYBACK.muteProperty().addListener(muteListener);              // add listener
        muteChanged(PLAYBACK.getMute());                                // init value
        
        PLAYBACK.currentTimeProperty().addListener(currTimeListener);   // add listener
        currTimeListener.invalidated(null);                             // init value
        
        // support drag transfer
        root.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle drag transfer
        root.setOnDragDropped( e -> {
            if (DragUtil.hasAudio(e.getDragboard())) {
                // get items
                List<Item> items = DragUtil.getAudioItems(e);
                // end drag
                e.setDropCompleted(true);
                e.consume();
                // handle result
                if(playDropped) {
                    PlaylistManager.playPlaylist(new Playlist(
                            items.stream().map(Item::getURI), true));
                } else {
                    PlaylistManager.addItems(items);
                }
            }
        });
    }
    
    @Override
    public void refresh() { }

    @Override
    public void close() {
        // remove listeners
        playingItemMonitoring.unsubscribe();
        PLAYBACK.statusProperty().removeListener(statusListener);       
        PLAYBACK.muteProperty().removeListener(muteListener);
        PLAYBACK.currentTimeProperty().removeListener(currTimeListener);
        seeker.unbindTime();
        volume.valueProperty().unbind();
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
    
    @FXML
    private void consumeMouseEvent(MouseEvent event) {
        event.consume(); // for example to prevent dragging application on some areas
    }

/******************************************************************************/
    
    Subscription playingItemMonitoring;
    private final ChangeListener<Status> statusListener = (o,ov,nv)-> statusChanged(nv);
    private final ChangeListener<Boolean> muteListener = (o,ov,nv)-> muteChanged(nv);
    private final InvalidationListener currTimeListener = o -> currentTimeChanged();
    
    
    private void playbackItemChanged(Metadata nv) {
        if(nv!=null) {
            titleL.setText(nv.getTitle());
            artistL.setText(nv.getArtist());
        }
        seeker.reloadChapters(nv);
    }
    private void statusChanged(Status status) {
        if (status == null || status == UNKNOWN ) {
            seeker.setDisable(true);
            play.setImage(playImg);
        } else if (status == PLAYING) {
            seeker.setDisable(false);
            play.setImage(pauseImg);
        } else {
            seeker.setDisable(false);
            play.setImage(playImg);
        }
    }
    private void muteChanged(boolean new_mode) {
        if (new_mode) {
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