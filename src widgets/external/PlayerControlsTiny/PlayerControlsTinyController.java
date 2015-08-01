package PlayerControlsTiny;


import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.Item;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.feature.PlaybackFeature;
import gui.GUI;
import gui.objects.Seeker;
import gui.objects.icon.Icon;
import util.Util;
import util.access.Accessor;
import util.graphics.drag.DragUtil;

import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.UNKNOWN;
import static util.reactive.Util.maintain;

/** FXMLController for widget. */
@Widget.Info(
    name = "Playback Mini",
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    howto = "Available actions:\n" +
            "    Control Playback\n" +
            "    Drop audio files : Adds or plays the files\n",
    description = "Controls playback. Minimalistic.",
    notes = "",
    version = "1",
    year = "2015",
    group = Widget.Group.PLAYBACK
)
public class PlayerControlsTinyController extends FXMLController implements PlaybackFeature {
    
    @FXML AnchorPane root;
    @FXML BorderPane seekerPane;
    @FXML HBox controlBox;
    @FXML HBox volBox;
    @FXML Slider volume;
    @FXML Label currTime;
    @FXML Label titleL;
    @FXML Label artistL;
    Seeker seeker = new Seeker();
    Icon prevB, playB, stopB, nextB, volB;
    
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
        
        // make volume
        volume.setMin(ps.volume.getMin());
        volume.setMax(ps.volume.getMax());
        volume.setValue(ps.volume.get());
        volume.valueProperty().bindBidirectional(ps.volume);
        d(volume.valueProperty()::unbind);
        
        // make seeker
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        d(seeker::unbindTime);
        d(maintain(GUI.snapDistance, d->d, seeker.chapterSnapDistance));
        seekerPane.setCenter(seeker);
        
        // make icons
        prevB = new Icon(STEP_BACKWARD, 14, null, PlaylistManager::playPreviousItem);
        playB = new Icon(null, 14, null, PLAYBACK::pause_resume);
        stopB = new Icon(STOP, 14, null, PLAYBACK::stop);
        nextB = new Icon(STEP_FORWARD, 14, null, PlaylistManager::playNextItem);
        controlBox.getChildren().addAll(prevB,playB,stopB,nextB);
        volB = new Icon(null, 14, null, PLAYBACK::toggleMute);
        volBox.getChildren().add(0,volB);
        
        // monitor properties and update graphics + initialize
        d(maintain(ps.volume, v -> muteChanged(ps.mute.get(), v.doubleValue())));
        d(maintain(ps.mute, m -> muteChanged(m, ps.volume.get())));
        d(maintain(ps.status, this::statusChanged));
        d(maintain(ps.currentTime,t->currentTimeChanged()));
        d(Player.playingtem.subscribeToUpdates(this::playbackItemChanged));   
        
        // drag & drop
        root.setOnDragOver(DragUtil.audioDragAccepthandler);
        root.setOnDragDropped( e -> {
            if (DragUtil.hasAudio(e.getDragboard())) {
                // get items
                List<Item> items = DragUtil.getAudioItems(e);
                // end drag
                e.setDropCompleted(true);
                e.consume();
                // handle result
                if(playDropped) {
                    PlaylistManager.use(p -> p.setNplay(items));
                } else {
                    PlaylistManager.use(p -> p.addItems(items));
                }
            }
        });
    }
    
    @Override
    public void refresh() { }
    
/******************************************************************************/
        
    @FXML private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        currentTimeChanged();
    }
    
    private void playbackItemChanged(Metadata m) {
        titleL.setText(m.getTitle());
        artistL.setText(m.getArtist());
        seeker.reloadChapters(m);
    }
    
    private void statusChanged(Status status) {
        if (status == null || status == UNKNOWN ) {
            seeker.setDisable(true);
            playB.icon(PLAY);
        } else if (status == PLAYING) {
            seeker.setDisable(false);
            playB.icon(PAUSE);
        } else {
            seeker.setDisable(false);
            playB.icon(PLAY);
        }
    }
    
    private void muteChanged(boolean mute, double vol) {
        volB.icon(mute ? VOLUME_OFF : vol>.5 ? VOLUME_UP : VOLUME_DOWN);
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