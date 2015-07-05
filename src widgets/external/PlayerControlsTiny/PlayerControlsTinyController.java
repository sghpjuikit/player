package PlayerControlsTiny;


import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.Playlist;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.tagging.Metadata;
import Configuration.IsConfig;
import Layout.Widgets.FXMLWidget;
import Layout.Widgets.Widget;
import Layout.Widgets.controller.FXMLController;
import Layout.Widgets.feature.PlaybackFeature;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIconName.*;
import gui.GUI;
import gui.objects.Seeker;
import gui.objects.icon.Icon;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.UNKNOWN;
import javafx.util.Duration;
import org.reactfx.Subscription;
import util.Util;
import util.access.Accessor;
import static util.functional.Util.list;
import static util.functional.Util.map;
import util.graphics.drag.DragUtil;
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
    
    // gui
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
    // dpendencies that should be disposed of - listeners, etc
    Subscription d1,d2,d3,d4,d5,d6;
    List<Subscription> ds;
    
    // properties
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
    
        
    public PlayerControlsTinyController(FXMLWidget widget) {
        super(widget);
    }
    
    @Override
    public void init() {
        // make volume
        volume.setMin(PLAYBACK.getVolumeMin());
        volume.setMax(PLAYBACK.getVolumeMax());
        volume.setValue(PLAYBACK.getVolume());
        volume.valueProperty().bindBidirectional(PLAYBACK.volumeProperty());
        // make seeker
        seeker.bindTime(PLAYBACK.totalTimeProperty(), PLAYBACK.currentTimeProperty());
        d6 = maintain(GUI.snapDistance, d->d, seeker.chapterSnapDistance);
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
        d1 = maintain(PLAYBACK.volumeProperty(), v->muteChanged(PLAYBACK.isMute(), v.doubleValue()));
        d2 = maintain(PLAYBACK.muteProperty(), m->muteChanged(m, PLAYBACK.getVolume()));
        d3 = maintain(PLAYBACK.statusProperty(), this::statusChanged);
        d4 = Player.playingtem.subscribeToUpdates(this::playbackItemChanged);   
        d5 = maintain(PLAYBACK.currentTimeProperty(),t->currentTimeChanged());
        
        // audio drag
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
                    PlaylistManager.playPlaylist(new Playlist(map(items, Item::toPlaylist)));
                } else {
                    PlaylistManager.addItems(items);
                }
            }
        });
        
        ds = list(d1,d2,d3,d4,d5,d6);
    }
    
    @Override
    public void refresh() { }

    @Override
    public void onClose() {
        // remove listeners
        ds.forEach(Subscription::unsubscribe);
        seeker.unbindTime();
        volume.valueProperty().unbind();
    }
    
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
            playB.icon.setValue(PLAY);
        } else if (status == PLAYING) {
            seeker.setDisable(false);
            playB.icon.setValue(PAUSE);
        } else {
            seeker.setDisable(false);
            playB.icon.setValue(PLAY);
        }
    }
    private void muteChanged(boolean mute, double vol) {
        volB.icon.setValue(mute ? VOLUME_OFF : vol>.5 ? VOLUME_UP : VOLUME_DOWN);
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