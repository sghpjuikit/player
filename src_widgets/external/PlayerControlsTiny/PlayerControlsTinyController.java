package PlayerControlsTiny;


import AudioPlayer.ItemChangeEvent.ItemChangeHandler;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.ItemHolders.Seeker;
import GUI.WindowManager;
import Layout.Widgets.FXMLController;
import Layout.Widgets.WidgetInfo;
import java.io.File;
import java.net.URI;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
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
    
    @Override
    public void init() {
        
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

    @Override
    public void OnClosing() {
        // remove listeners
        Player.remOnItemUpdate(playbackItemChanged);
        PLAYBACK.muteProperty().removeListener(muteListener);
        PLAYBACK.currentTimeProperty().removeListener(currTimeListener);
        
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
    
    
    private void playbackItemChanged(Metadata new_item) {
        if(new_item!=null) {
            titleL.setText(new_item.getTitle());
            artistL.setText(new_item.getArtist());
        }
        seeker.reloadChapters();
    }
    private void muteChanged(boolean new_mode) {
        if (new_mode) {
            mute.setImage(muteOFFImg);
        } else {
            mute.setImage(muteONImg);
        }
    }
    private void currentTimeChanged() {
        // update seeker position
        if (seeker.canUpdate)
            seeker.updatePosition();
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