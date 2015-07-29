package PlayerControls;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.ItemSelection.PlayingItemSelector.LoopMode;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import static AudioPlayer.tagging.Metadata.Field.BITRATE;
import Configuration.IsConfig;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.feature.PlaybackFeature;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import gui.GUI;
import gui.objects.Balancer.Balancer;
import gui.objects.Seeker;
import gui.objects.icon.GlowIcon;
import gui.objects.icon.Icon;
import java.io.File;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import util.Util;
import static util.Util.formatDuration;
import util.access.Accessor;
import static util.functional.Util.map;
import util.graphics.drag.DragUtil;
import static util.reactive.Util.*;


/**
 * Playback Controller class
 * <p>
 * @author uranium
 */
@Widget.Info(
    name = "Playback",
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
    group = Widget.Group.PLAYBACK
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
    
    @FXML HBox infoBox;
    
    @FXML HBox playButtons;
    Icon p1 = new GlowIcon(ANGLE_DOUBLE_LEFT,25);
    Icon f2 = new GlowIcon(FAST_BACKWARD,25);
    Icon f3 = new GlowIcon(PLAY,25);
    Icon f4 = new GlowIcon(FAST_FORWARD,25);
    Icon f5 = new GlowIcon(ANGLE_DOUBLE_RIGHT,25);
    Icon f6 = new GlowIcon(STOP,25);
    Icon muteB = new GlowIcon(VOLUME_UP,15);
    Icon addB = new GlowIcon(PLUS_SQUARE_ALT,10);
    Icon loopB = new GlowIcon(RANDOM,14);
    
    @IsConfig(name = "Show chapters", info = "Display chapter marks on seeker.")
    public final Accessor<Boolean> showChapters = new Accessor<>(true, seeker::setChaptersVisible);
    @IsConfig(name = "Show info for chapters", info = "Display pop up information for chapter marks on seeker.")
    public final Accessor<Boolean> popupChapters = new Accessor<>(true, seeker::setChaptersShowPopUp);
    @IsConfig(name = "Snap seeker to chapters on drag", info = "Enable snapping to chapters during dragging.")
    public final Accessor<Boolean> snapToChap = new Accessor<>(true, seeker::setSnapToChapters);
    @IsConfig(name = "Show max 1 chapter", info = "Allows only one chapter popup to be visible at any time. Opening new chapter closes all open chapters.")
    public final Accessor<Boolean> singleChapMode = new Accessor<>(true, seeker::setSinglePopupMode);
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    public boolean elapsedTime = true;
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    public boolean playDropped = false;
    

    @Override
    public void init() {
        PlaybackState ps = PLAYBACK.state;
        
        // create balancer
        balance = new Balancer();
        soundGrid.add(balance, 1, 1);
        balance.setPrefSize(50,20);
        
        balance.setMin(ps.balance.getMin());
        balance.setMax(ps.balance.getMax());
        balance.balanceProperty().bindBidirectional(ps.balance);
        d(balance.balanceProperty()::unbind);
        
        volume.setMin(ps.volume.getMin());
        volume.setMax(ps.volume.getMax());
        volume.setValue(ps.volume.get());
        volume.valueProperty().bindBidirectional(ps.volume);
        d(volume.valueProperty()::unbind);
        
        seeker.bindTime(ps.duration, ps.currentTime);
        d(seeker::unbindTime);
        entireArea.getChildren().add(seeker);
        AnchorPane.setBottomAnchor(seeker, 0.0);
        AnchorPane.setLeftAnchor(seeker, 0.0);
        AnchorPane.setRightAnchor(seeker, 0.0);
        d(maintain(GUI.snapDistance, d->d ,seeker.chapterSnapDistance));
        
        // create play buttons
        p1.setOnMouseClicked(e->rewind());
        f2.setOnMouseClicked(e->previous());
        f3.setOnMouseClicked(e->play_pause());
        f4.setOnMouseClicked(e->next());
        f5.setOnMouseClicked(e->forward());
        f6.setOnMouseClicked(e->stop());
        playButtons.getChildren().setAll(p1,f2,f3,f4,f5,f6);
        
        // addButton
        Tooltip.install(addB, new Tooltip("Add files or folder\n\nUse left for files and right click for directory."));
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
        loopB.setOnMouseClicked(this::cycleLoopMode);
        loopB.setScaleX(1.3); // scale horizontally a bit to get non-rectangle icon
        infoBox.getChildren().add(1, loopB);
        
        // volume button
        muteB.setOnMouseClicked(e->cycleMute());
        soundGrid.add(muteB, 0, 0);
     
        // set gui updating
        d(Player.playingtem.subscribeToUpdates(this::playingItemChanged));  // add listener
        playingItemChanged(Player.playingtem.get());                    // init value
        d(maintain(ps.duration, t -> totTime.setText(formatDuration(t))));
        d(maintain(ps.currentTime, t -> timeChanged()));
        d(maintain(ps.status, this::statusChanged));
        d(maintain(ps.loopMode, this::loopModeChanged));
        d(maintain(ps.mute, v -> muteChanged(v, ps.volume.get())));
        d(maintain(ps.volume, v -> muteChanged(ps.mute.get(), v.doubleValue())));
        
        // drag & drop
        entireArea.setOnDragOver(DragUtil.audioDragAccepthandler);
        entireArea.setOnDragDropped( e -> {
            if (DragUtil.hasAudio(e.getDragboard())) {
                // get items
                List<Item> items = DragUtil.getAudioItems(e);
                // end drag
                e.setDropCompleted(true);
                e.consume();
                // handle result
                if(playDropped) {
                    PlaylistManager.playPlaylist(new Playlist(map(items, Item::toPlaylist)));
                } else {
                    PlaylistManager.addItems(items);
                }
            }
        });
    }
    
    @Override
    public void refresh() { }
    
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
    
    private void cycleLoopMode(MouseEvent e) {
        if(e.getButton()==PRIMARY) PLAYBACK.setLoopMode(PLAYBACK.getLoopMode().next());
        if(e.getButton()==SECONDARY) PLAYBACK.setLoopMode(PLAYBACK.getLoopMode().previous());
    }
    
    @FXML private void cycleMute() {
        PLAYBACK.toggleMute();
    }
    
    @FXML private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        timeChanged();
    }

    
    // for example to prevent dragging application on some areas
    @FXML private void consumeMouseEvent(MouseEvent event) {
        event.consume();
    }
    
/******************************************************************************/
    
    private void playingItemChanged(Metadata nv) {
        titleL.setText(nv.getTitle());
        artistL.setText(nv.getArtist());
        bitrateL.setText(nv.getFieldS(BITRATE, ""));
        sampleRateL.setText(nv.getSampleRate());
        channelsL.setText(nv.getChannels());
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
                f3.setIcon(PAUSE);
            } else {
                f3.setIcon(PLAY);
            }
        }
    }
    
    private void loopModeChanged(LoopMode new_mode) {
        switch (new_mode) {
            case OFF:       loopB.setIcon(ALIGN_CENTER); // linear
                            Tooltip.install(loopB, new Tooltip("Loop mode: off"));
                            break;
            case PLAYLIST:  loopB.setIcon(REORDER);     // linear playlist
                            Tooltip.install(loopB, new Tooltip("Loop mode: loop playlist"));
                            break;
            case SONG:      loopB.setIcon(REPEAT);      // point
                            Tooltip.install(loopB, new Tooltip("Loop mode: loop song"));
                            break;
            case RANDOM:    loopB.setIcon(RANDOM);      // random
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

    private void timeChanged() {
        if (elapsedTime) {
            Duration elapsed = PLAYBACK.getCurrentTime();
            currTime.setText(Util.formatDuration(elapsed));  
        } else {
            Duration remaining = PLAYBACK.getRemainingTime();
            currTime.setText("- " + Util.formatDuration(remaining)); 
        }
        realTime.setText(Util.formatDuration(PLAYBACK.getRealTime()));
    }
    
}