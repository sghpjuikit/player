package playerControls;

import java.io.File;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import audio.Item;
import audio.Player;
import audio.playback.PLAYBACK;
import audio.playback.PlaybackState;
import audio.playlist.PlaylistManager;
import audio.playlist.sequence.PlayingSequence.LoopMode;
import audio.tagging.Metadata;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import gui.Gui;
import gui.objects.balancer.Balancer;
import gui.objects.icon.GlowIcon;
import gui.objects.icon.Icon;
import gui.objects.seeker.Seeker;
import layout.widget.Widget;
import layout.widget.controller.FXMLController;
import layout.widget.feature.PlaybackFeature;
import util.Util;
import util.access.V;
import util.conf.IsConfig;
import util.graphics.drag.DragUtil;

import static audio.tagging.Metadata.Field.BITRATE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.*;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FAST_FORWARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PAUSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STOP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_OFF;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.*;
import static util.Util.formatDuration;
import static util.functional.Util.mapRef;
import static util.graphics.drag.DragUtil.installDrag;
import static util.reactive.Util.maintain;

/**
 * Playback Controller class
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
    @FXML Label currTime, totTime, realTime, status;
    @FXML Label titleL, artistL, bitrateL, sampleRateL, channelsL;
    @FXML HBox infoBox, playButtons;
    Seeker seeker = new Seeker();
    Icon f1 = new GlowIcon(ANGLE_DOUBLE_LEFT,25).onClick(PLAYBACK::seekBackward),
         f2    = new GlowIcon(FAST_BACKWARD,25).onClick(PlaylistManager::playPreviousItem),
         f3    = new GlowIcon(PLAY,25).onClick(PLAYBACK::pause_resume),
         f4    = new GlowIcon(FAST_FORWARD,25).onClick(PlaylistManager::playNextItem),
         f5    = new GlowIcon(ANGLE_DOUBLE_RIGHT,25).onClick(PLAYBACK::seekForward),
         f6    = new GlowIcon(STOP,25).onClick(PLAYBACK::stop),
         muteB = new GlowIcon(VOLUME_UP,15).onClick(PLAYBACK::toggleMute),
         addB  = new GlowIcon(PLUS_SQUARE_ALT,10),
         loopB = new GlowIcon(RANDOM,15).onClick((MouseEvent e) -> PLAYBACK.toggleLoopMode(e));
    double lastUpdatedTime = Double.MIN_VALUE; // reduces time update events

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

        // balancer
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
        seeker.setPrefHeight(30);
        AnchorPane.setBottomAnchor(seeker, 0.0);
        AnchorPane.setLeftAnchor(seeker, 0.0);
        AnchorPane.setRightAnchor(seeker, 0.0);
        d(maintain(Gui.snapDistance, seeker.chapSnapDist));

        // icons
        playButtons.getChildren().setAll(f1,f2,f3,f4,f5,f6);
        infoBox.getChildren().add(1, loopB);
        soundGrid.add(muteB, 0, 0);
        addB.tooltip("Add files or folder\n\nUse left for files and right click for directory.");
        addB.setOnMouseClicked(e-> {
            if (e.getButton()==MouseButton.PRIMARY)
                PlaylistManager.chooseFilesToPlay();
            else
                PlaylistManager.chooseFolderToPlay();
        });
        entireArea.getChildren().add(addB);
        AnchorPane.setTopAnchor(addB, 5d);
        AnchorPane.setLeftAnchor(addB, 5d);

        // set gui updating
        d(maintain(ps.duration,     Util::formatDuration, totTime.textProperty()));
        d(maintain(ps.currentTime,  t -> timeChanged()));
        d(maintain(ps.status,       this::statusChanged));
        d(maintain(ps.loopMode,     this::loopModeChanged));
        d(maintain(ps.mute,         v -> muteChanged(v, ps.volume.get())));
        d(maintain(ps.volume,       v -> muteChanged(ps.mute.get(), v.doubleValue())));
        d(PLAYBACK.onSeekDone.addS(() -> lastUpdatedTime = Double.MIN_VALUE));
        d(Player.playingItem.onUpdate(this::playingItemChanged));  // add listener
        playingItemChanged(Player.playingItem.get());              // init value

        // drag & drop
        installDrag(
            entireArea, PLAYLIST_PLUS, "Add to active playlist",
            DragUtil::hasAudio,
            e -> {
                List<Item> items = DragUtil.getAudioItems(e);
                PlaylistManager.use(playDropped ? p -> p.setNplay(items) : p -> p.addItems(items));
            }
        );
    }

    @Override
    public void refresh() { }

    private void playFile(File file) {
         PlaylistManager.use(p -> {
             p.addUri(file.toURI());
             p.playLastItem();
         });
    }

    @FXML
    private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        timeChanged();
    }

    @FXML
    private void consumeMouseEvent(MouseEvent event) {
        event.consume();
    }

    private void playingItemChanged(Metadata nv) {
        lastUpdatedTime = Double.MIN_VALUE;
        titleL.setText(nv.getTitle());
        artistL.setText(nv.getArtist());
        bitrateL.setText(nv.getFieldS(BITRATE, ""));
        sampleRateL.setText(nv.getSampleRate());
        channelsL.setText(nv.getChannels());
        seeker.reloadChapters(nv);
    }

    private void statusChanged(Status newStatus) {
        lastUpdatedTime = Double.MIN_VALUE;
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
        if (loopB.getTooltip()==null) loopB.tooltip("ignoredText"); // lazy init
        loopB.getTooltip().setText(mapRef(looping,
            LoopMode.OFF, LoopMode.PLAYLIST, LoopMode.SONG, LoopMode.RANDOM,
            "Loop mode: off", "Loop mode: loop playlist", "Loop mode: loop song", "Play mode: random")
        );
        loopB.setIcon(mapRef(looping,
            LoopMode.OFF, LoopMode.PLAYLIST, LoopMode.SONG, LoopMode.RANDOM,
            REPEAT_OFF, MaterialDesignIcon.REPEAT, REPEAT_ONCE, RANDOM)
        );
    }

    private void muteChanged(boolean mute, double volume) {
        if (mute) {
            muteB.setIcon(VOLUME_OFF);
        } else {
            muteB.setIcon(volume>0.5 ? VOLUME_UP : VOLUME_DOWN);
        }
    }

    private void timeChanged() {
        double millis = PLAYBACK.getCurrentTime().toMillis();
        if (lastUpdatedTime+1000 <= millis) {
            lastUpdatedTime = millis;
            if (elapsedTime) {
                Duration elapsed = PLAYBACK.getCurrentTime();
                currTime.setText(formatDuration(elapsed));
            } else {
                Duration remaining = PLAYBACK.getRemainingTime();
                currTime.setText("- " + formatDuration(remaining));
            }
            realTime.setText(formatDuration(PLAYBACK.getRealTime()));
        }
    }

}