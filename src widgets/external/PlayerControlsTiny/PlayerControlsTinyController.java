package PlayerControlsTiny;


import AudioPlayer.ItemChangeEvent.ItemChangeHandler;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.objects.Seeker;
import GUI.WindowManager;
import Layout.Widgets.FXMLController;
import Layout.Widgets.WidgetInfo;
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
import javafx.util.Duration;
import main.App;
import utilities.FileUtil;
import utilities.Util;

/**
 * Playback Controller class
 * <p>
 * @author uranium
 */
@WidgetInfo(name = "Tiny")
public class PlayerControlsTinyController extends FXMLController {
    
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
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    public boolean playDropped = false;
    
    @Override
    public void init() {
        
        volume.setMin(PLAYBACK.getVolumeMin());
        volume.setMax(PLAYBACK.getVolumeMax());
        volume.setValue(PLAYBACK.getVolume());
        volume.valueProperty().bindBidirectional(PLAYBACK.volumeProperty());
        
        seeker = new Seeker();
        seeker.prefWidthProperty().bind(seekerPane.widthProperty());
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        seekerPane.setCenter(seeker);
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
        playbackItemChanged(Player.getCurrentMetadata());               // init value
        
        PLAYBACK.muteProperty().addListener(muteListener);              // add listener
        muteChanged(PLAYBACK.getMute());                                // init value
        
        PLAYBACK.currentTimeProperty().addListener(currTimeListener);   // add listener
        currTimeListener.invalidated(null);                             // init value
        
        // support drag transfer
        root.setOnDragOver( e -> {
            Dragboard db = e.getDragboard();
            if ((db.hasFiles() && FileUtil.hasAudioFiles(db.getFiles())) ||
                    db.hasUrl() || 
                        db.hasContent(DragUtil.playlist)) {
                e.acceptTransferModes(TransferMode.ANY);
                e.consume();
            }
        });
        root.setOnDragDropped( e -> {
            // get items
            Dragboard db = e.getDragboard();
            Playlist p = new Playlist();
            if (db.hasFiles())
                p.addFiles(db.getFiles());
            if (db.hasUrl())
                p.addUrl(db.getUrl());
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
    
    ItemChangeHandler<Metadata> playbackItemChanged = (oldV,newV)-> playbackItemChanged(newV);
    ChangeListener<Boolean> muteListener = (o,oldV,newV)-> muteChanged(newV);
    InvalidationListener currTimeListener = o -> currentTimeChanged();
    
    
    private void playbackItemChanged(Metadata m) {
        if(m!=null) {
            titleL.setText(m.getTitle());
            artistL.setText(m.getArtist());
        }
        seeker.reloadChapters(m);
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
    
    
    @FXML public void toggleMini() {
        WindowManager.toggleMini();
    }
    @FXML public void closeApp() {
        App.getWindow().close();
    }
}