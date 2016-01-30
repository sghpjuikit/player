package PlayerControls;

import java.io.File;
import java.util.List;

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

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.PlaylistManager;
import AudioPlayer.playlist.sequence.PlayingSequence.LoopMode;
import AudioPlayer.tagging.Metadata;
import util.conf.IsConfig;
import Layout.widget.Widget;
import Layout.widget.controller.FXMLController;
import Layout.widget.feature.PlaybackFeature;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import gui.GUI;
import gui.objects.Balancer.Balancer;
import gui.objects.seeker.Seeker;
import gui.objects.icon.GlowIcon;
import gui.objects.icon.Icon;
import util.Util;
import util.access.V;
import util.graphics.drag.DragUtil;

import static AudioPlayer.tagging.Metadata.Field.BITRATE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.REPEAT_OFF;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.REPEAT_ONCE;
import static util.Util.formatDuration;
import static util.graphics.drag.DragUtil.installDrag;
import static util.reactive.Util.maintain;


/**
 * Playback Controller class
 * <p>
 * @author uranium
 */
@Widget.Info(
    name = "Playback",
    author = "Martin Polakovic",
    programmer = "Martin Polakovic",
    howto = "Playback actions:\n"
          + "    Control Playback\n"
          + "    Drop audio files : Adds or plays the files\n"
          + "    Left click : Seek - move playback to seeked position\n"
          + "    Mouse drag : Seek (on release)\n"
          + "    Right click : Cancel seek\n"
          + "    Add button left click : Opens file chooser and plays files\n"
          + "    Add button right click: Opens directory chooser and plays files\n"
          + "    Drop audio files : Adds or plays the files\n"
          + "\nChapter actions:\n"
          + "    Right click : Create chapter\n"
          + "    Right click chapter : Open chapter\n"
          + "    Mouse hover chapter (optional) : Open chapter\n",
    description = "Playback control widget.",
    notes = "",
    version = "0.8",
    year = "2014",
    group = Widget.Group.PLAYBACK
)
public class PlayerControls extends FXMLController implements PlaybackFeature {

    @FXML AnchorPane entireArea;
    @FXML BorderPane controlPanel;
    @FXML GridPane soundGrid;
    @FXML Slider volume;
    @FXML Balancer balance;
    Seeker seeker = new Seeker();
    @FXML Label currTime, totTime, realTime, status;
    @FXML Label titleL, artistL, bitrateL, sampleRateL, channelsL;
    @FXML HBox infoBox;
    @FXML HBox playButtons;
    Icon p1    = new GlowIcon(ANGLE_DOUBLE_LEFT,25);
    Icon f2    = new GlowIcon(FAST_BACKWARD,25);
    Icon f3    = new GlowIcon(PLAY,25);
    Icon f4    = new GlowIcon(FAST_FORWARD,25);
    Icon f5    = new GlowIcon(ANGLE_DOUBLE_RIGHT,25);
    Icon f6    = new GlowIcon(STOP,25);
    Icon muteB = new GlowIcon(VOLUME_UP,15);
    Icon addB  = new GlowIcon(PLUS_SQUARE_ALT,10);
    Icon loopB = new GlowIcon(RANDOM,15);

    @IsConfig(name = "Show chapters", info = "Display chapter marks on seeker.")
    public final V<Boolean> showChapters = new V<>(true, seeker::setChaptersVisible);
    @IsConfig(name = "Open chapters", info = "Display pop up information for chapter marks on seeker.")
    public final V<Boolean> popupChapters = new V<>(true, seeker::setChaptersShowPopUp);
    @IsConfig(name = "Snap seeker to chapters on drag", info = "Enable snapping to chapters during dragging.")
    public final V<Boolean> snapToChap = new V<>(true, seeker::setSnapToChapters);
    @IsConfig(name = "Open max 1 chapter", info = "Allows only one chapter open. Opening chapter closes all open chapters.")
    public final V<Boolean> singleChapMode = new V<>(true, seeker::setSinglePopupMode);
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

        d(seeker.bindTime(ps.duration, ps.currentTime));
        entireArea.getChildren().add(seeker);
        AnchorPane.setBottomAnchor(seeker, 0.0);
        AnchorPane.setLeftAnchor(seeker, 0.0);
        AnchorPane.setRightAnchor(seeker, 0.0);
        d(maintain(GUI.snapDistance, d->d ,seeker.chapSnapDist));

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
        addB.setOnMouseClicked(e-> {
            if(e.getButton()==MouseButton.PRIMARY)
                PlaylistManager.chooseFilesToPlay();
            else
                PlaylistManager.chooseFolderToPlay();
        });
        entireArea.getChildren().add(addB);
        AnchorPane.setTopAnchor(addB, 5d);
        AnchorPane.setLeftAnchor(addB, 5d);

        // loopmode
        loopB.setOnMouseClicked(PLAYBACK::toggleLoopMode);
        infoBox.getChildren().add(1, loopB);

        // volume button
        muteB.setOnMouseClicked(e -> PLAYBACK.toggleMute());
        soundGrid.add(muteB, 0, 0);

        // set gui updating
        d(Player.playingtem.onUpdate(this::playingItemChanged));  // add listener
        playingItemChanged(Player.playingtem.get());              // init value
        d(maintain(ps.duration, t -> formatDuration(t), totTime.textProperty()));
        d(maintain(ps.currentTime, t -> timeChanged()));
        d(maintain(ps.status, this::statusChanged));
        d(maintain(ps.loopMode, this::loopModeChanged));
        d(maintain(ps.mute, v -> muteChanged(v, ps.volume.get())));
        d(maintain(ps.volume, v -> muteChanged(ps.mute.get(), v.doubleValue())));

        // drag & drop
        installDrag(
            entireArea, PLAYLIST_PLUS, "Add to active playlist",
            DragUtil::hasAudio,
            e -> {
                List<Item> items = DragUtil.getAudioItems(e);
                PlaylistManager.use(p -> {
                    if(!playDropped) p.addItems(items);
                    else p.setNplay(items);
                });
            }
        );
    }

    @Override
    public void refresh() { }

/******************************************************************************/

    private void playFile(File file) {
         PlaylistManager.use(p -> {
             p.addUri(file.toURI());
             p.playLastItem();
         });
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

    @FXML private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        timeChanged();
    }


    // for example to prevent dragging application on some areas
    @FXML private void consumeMouseEvent(MouseEvent event) {
        event.consume();
    }

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

    private void loopModeChanged(LoopMode looping) {
        switch (looping) {
            case OFF:       loopB.setIcon(REPEAT_OFF);
                            Tooltip.install(loopB, new Tooltip("Loop mode: off"));
                            break;
            case PLAYLIST:  loopB.setIcon(MaterialDesignIcon.REPEAT);
                            Tooltip.install(loopB, new Tooltip("Loop mode: loop playlist"));
                            break;
            case SONG:      loopB.setIcon(REPEAT_ONCE);
                            Tooltip.install(loopB, new Tooltip("Loop mode: loop song"));
                            break;
            case RANDOM:    loopB.setIcon(RANDOM);
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