package PlayerControls;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector.LoopMode;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import GUI.DragUtil;
import GUI.GUI;
import GUI.objects.Balancer.Balancer;
import GUI.objects.FadeButton;
import GUI.objects.Seeker;
import Layout.Widgets.FXMLController;
import Layout.Widgets.Features.PlaybackFeature;
import Layout.Widgets.Widget;
import de.jensd.fx.fontawesome.AwesomeIcon;
import static de.jensd.fx.fontawesome.AwesomeIcon.VOLUME_DOWN;
import static de.jensd.fx.fontawesome.AwesomeIcon.VOLUME_OFF;
import static de.jensd.fx.fontawesome.AwesomeIcon.VOLUME_UP;
import java.io.File;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import org.reactfx.Subscription;
import utilities.Util;
import utilities.access.Accessor;


/**
 * Playback Controller class
 * <p>
 * @author uranium
 */
@Widget.Info(
    name = "Playback Control",
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    howto = "Available actions:\n" +
            "    Control Playback\n" +
            "    Add button left click : Opens file chooser and plays files\n" +
            "    Add button right click: Opens directory chooser and plays files\n" +
            "    Drop audio files : Adds or plays the files\n",
    description = "Controls playback.",
    notes = "",
    version = "0.8",
    year = "2014",
    group = Widget.Group.TAGGER
)
public class PlayerControlsController extends FXMLController implements PlaybackFeature {
    
    @FXML AnchorPane entireArea;
    @FXML BorderPane controlPanel;
    @FXML GridPane soundGrid;
    @FXML Slider volume;
    @FXML Balancer balance;
    Seeker seeker = new Seeker();
    @FXML Label currTime;
    @FXML Label totTime;
    @FXML Label realTime;
    @FXML Label status;
    
    @FXML Label titleL;
    @FXML Label artistL;
    @FXML Label bitrateL;
    @FXML Label sampleRateL;
    @FXML Label channelsL;
    
    @FXML HBox playButtons;
    FadeButton p1 = new FadeButton(AwesomeIcon.ANGLE_DOUBLE_LEFT,25); // BACKWARD is a good choice too
    FadeButton f2 = new FadeButton(AwesomeIcon.FAST_BACKWARD,25);
    FadeButton f3 = new FadeButton(AwesomeIcon.PLAY,25);
    FadeButton f4 = new FadeButton(AwesomeIcon.FAST_FORWARD,25);
    FadeButton f5 = new FadeButton(AwesomeIcon.ANGLE_DOUBLE_RIGHT,25);// FORWARD is a good choice too
    FadeButton f6 = new FadeButton(AwesomeIcon.STOP,25);
    FadeButton muteB = new FadeButton(AwesomeIcon.VOLUME_UP,15);
    FadeButton addB = new FadeButton(AwesomeIcon.PLUS_SQUARE_ALT,10);
    
    @FXML HBox infoBox;
    FadeButton loopB = new FadeButton(AwesomeIcon.RANDOM,14);
    
    // properties
    @IsConfig(name = "Show chapters", info = "Display chapter marks on seeker.")
    public final Accessor<Boolean> showChapters = new Accessor<>(true, seeker::setChaptersVisible);
    @IsConfig(name = "Show info for chapters", info = "Display pop up information for chapter marks on seeker.")
    public final Accessor<Boolean> popupChapters = new Accessor<>(true, seeker::setChaptersShowPopUp);
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
        
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        entireArea.getChildren().add(seeker);
        AnchorPane.setBottomAnchor(seeker, 0.0);
        AnchorPane.setLeftAnchor(seeker, 0.0);
        AnchorPane.setRightAnchor(seeker, 0.0);
        seeker.setChapterSnapDistance(GUI.snapDistance);        
        
        // create play buttons
        p1.setOnMouseClicked(e->rewind());
        f2.setOnMouseClicked(e->previous());
        f3.setOnMouseClicked(e->play_pause());
        f4.setOnMouseClicked(e->next());
        f5.setOnMouseClicked(e->forward());
        f6.setOnMouseClicked(e->stop());
        
        playButtons.getChildren().setAll(p1,f2,f3,f4,f5,f6);
        
        // addButton
        Tooltip.install(addB, new Tooltip("Add files or folder (left/right click)."));
        addB.setOnMouseClicked(e->{
            if(e.getButton()==MouseButton.PRIMARY)
                PlaylistManager.addOrEnqueueFiles(true);
            else
                PlaylistManager.addOrEnqueueFolder(true);
        });
        entireArea.getChildren().add(addB);
        AnchorPane.setTopAnchor(addB, 5d);
        AnchorPane.setLeftAnchor(addB, 5d);
        
        // loopmode
        loopB.setOnMouseClicked(e->PLAYBACK.toggleLoopMode());
        loopB.setScaleX(1.3); // scale horizontally a bit for non-rectangle icon
        infoBox.getChildren().add(1, loopB);
        
        // volume button
        muteB.setOnMouseClicked(e->cycleMute());
        soundGrid.add(muteB, 0, 0);
     
        
        // set updating + initialize manually
        playingItemMonitoring = Player.playingtem.subscribeToUpdates(this::playingItemChanged);  // add listener
        playingItemChanged(Player.playingtem.get());                  // init value
        
        PLAYBACK.statusProperty().addListener(statusListener);          // add listener
        statusChanged(PLAYBACK.getStatus());                            // init value
        
        PLAYBACK.loopModeProperty().addListener(loopModeListener);      // add listener
        loopModeChanged(PLAYBACK.getLoopMode());                        // init value
                
        PLAYBACK.muteProperty().addListener(muteListener);              // add listener
        muteChanged(PLAYBACK.getMute(), volume.getValue());             // init value
        
        
        PLAYBACK.totalTimeProperty().addListener(totalTimeListener);    // add listener
        PLAYBACK.realTimeProperty().addListener(realTimeListener);      // add listener        
        PLAYBACK.currentTimeProperty().addListener(currTimeListener);   // add listener
        currTimeListener.invalidated(null);                             // init value
        
        ChangeListener<Number> volumeListener = (o,ov,nv) -> 
                muteChanged(PLAYBACK.isMute(), nv.doubleValue());
        volume.valueProperty().addListener(volumeListener);
        
        // support drag transfer
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        // handle drag transfer
        entireArea.setOnDragDropped( e -> {
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
    public void OnClosing() {
        // remove listeners
        playingItemMonitoring.unsubscribe();
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
    
    Subscription playingItemMonitoring;
    private final ChangeListener<Status> statusListener = (o,ov,nv)-> statusChanged(nv);
    private final ChangeListener<LoopMode> loopModeListener = (o,ov,nv)-> loopModeChanged(nv);
    private final ChangeListener<Boolean> muteListener = (o,ov,nv)-> muteChanged(nv, volume.getValue());
    private final InvalidationListener currTimeListener = o -> currentTimeChanged();
    private final InvalidationListener realTimeListener = o -> realTime.setText(Util.formatDuration(PLAYBACK.getRealTime()));
    private final InvalidationListener totalTimeListener = o -> totTime.setText(Util.formatDuration(PLAYBACK.getTotalTime()));       
    
    private void playingItemChanged(Metadata nv) {
        if(nv!=null){
            titleL.setText(nv.getTitle());
            artistL.setText(nv.getArtist());
            bitrateL.setText(nv.getBitrate().toString());
            sampleRateL.setText(nv.getSampleRate());
            channelsL.setText(nv.getChannels());
        }
        seeker.reloadChapters(nv);
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
                f3.setIcon(AwesomeIcon.PAUSE);
            } else {
                f3.setIcon(AwesomeIcon.PLAY);
            }
        }
    }
    private void loopModeChanged(LoopMode new_mode) {
        switch (new_mode) {
            case OFF:       loopB.setIcon(AwesomeIcon.ALIGN_CENTER); // linear
                            Tooltip.install(loopB, new Tooltip("Loop mode: off"));
                            break;
            case PLAYLIST:  loopB.setIcon(AwesomeIcon.REORDER);     // linear playlist
                            Tooltip.install(loopB, new Tooltip("Loop mode: loop playlist"));
                            break;
            case SONG:      loopB.setIcon(AwesomeIcon.REPEAT);      // point
                            Tooltip.install(loopB, new Tooltip("Loop mode: loop song"));
                            break;
            case RANDOM:    loopB.setIcon(AwesomeIcon.RANDOM);      // random
                            Tooltip.install(loopB, new Tooltip("Play mode: random"));
                            break;
        }
    }
    private void muteChanged(boolean mute, double valume) {
        if (mute) {
            muteB.setIcon(VOLUME_OFF);
        } else {
            muteB.setIcon(valume>0.5 ? VOLUME_UP : VOLUME_DOWN);
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