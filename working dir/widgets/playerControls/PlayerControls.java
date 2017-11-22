package playerControls;

import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playback.PLAYBACK;
import sp.it.pl.audio.playback.PlaybackState;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode;
import sp.it.pl.audio.tagging.Metadata;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.objects.balancer.Balancer;
import sp.it.pl.gui.objects.icon.GlowIcon;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.seeker.ChapterDisplayActivation;
import sp.it.pl.gui.objects.seeker.ChapterDisplayMode;
import sp.it.pl.gui.objects.seeker.Seeker;
import java.io.File;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.FXMLController;
import sp.it.pl.layout.widget.feature.PlaybackFeature;
import sp.it.pl.util.Util;
import sp.it.pl.util.access.V;
import sp.it.pl.util.conf.IsConfig;
import static sp.it.pl.audio.playback.PLAYBACK.Seek.RELATIVE;
import static sp.it.pl.audio.tagging.Metadata.Field.BITRATE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_LEFT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.ANGLE_DOUBLE_RIGHT;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FAST_BACKWARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.FAST_FORWARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PAUSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.RANDOM;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STOP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_OFF;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_UP;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.REPEAT_OFF;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.REPEAT_ONCE;
import static sp.it.pl.util.Util.formatDuration;
import static sp.it.pl.util.functional.Util.mapRef;
import static sp.it.pl.util.graphics.drag.DragUtil.getAudioItems;
import static sp.it.pl.util.graphics.drag.DragUtil.hasAudio;
import static sp.it.pl.util.graphics.drag.DragUtil.installDrag;
import static sp.it.pl.util.reactive.Util.maintain;

/**
 * Playback Controller class
 */
@Widget.Info(
    name = "Playback",
    author = "Martin Polakovic",
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

	@IsConfig(name = "Seek type", info = "Seek by time (absolute) or fraction of total duration (relative).")
	public final V<PLAYBACK.Seek> seekType = new V<>(RELATIVE);
    @IsConfig(name = "Chapters show", info = "Display chapter marks on seeker.")
    public final V<ChapterDisplayMode> showChapters = seeker.chapterDisplayMode;
    @IsConfig(name = "Chapter open on", info = "Opens chapter also when mouse hovers over them.")
    public final V<ChapterDisplayActivation> showChapOnHover = seeker.chapterDisplayActivation;
    @IsConfig(name = "Snap seeker to chapters", info = "Enable snapping to chapters during dragging.")
    public final V<Boolean> snapToChap = seeker.chapterSnap;
    @IsConfig(name = "Show elapsed time", info = "Show elapsed time instead of remaining.")
    public boolean elapsedTime = true;
    @IsConfig(name = "Play files on drop", info = "Plays the drag and dropped files instead of enqueuing them in playlist.")
    public boolean playDropped = false;

	Icon f1    = new GlowIcon(ANGLE_DOUBLE_LEFT,25).onClick(() -> PLAYBACK.seekBackward(seekType.get())),
		 f2    = new GlowIcon(FAST_BACKWARD,25).onClick(PlaylistManager::playPreviousItem),
		 f3    = new GlowIcon(PLAY,25).onClick(PLAYBACK::pause_resume),
		 f4    = new GlowIcon(FAST_FORWARD,25).onClick(PlaylistManager::playNextItem),
		 f5    = new GlowIcon(ANGLE_DOUBLE_RIGHT,25).onClick(() -> PLAYBACK.seekForward(seekType.get())),
		 f6    = new GlowIcon(STOP,25).onClick(PLAYBACK::stop),
		 muteB = new GlowIcon(VOLUME_UP,15).onClick(PLAYBACK::toggleMute),
		 loopB = new GlowIcon(RANDOM,15).onClick((MouseEvent e) -> PLAYBACK.toggleLoopMode(e));
	double lastUpdatedTime = Double.MIN_VALUE; // reduces time update events

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
        d(maintain(Gui.snapDistance, seeker.chapterSnapDistance));

        // icons
        playButtons.getChildren().setAll(f1,f2,f3,f4,f5,f6);
        infoBox.getChildren().add(1, loopB);
        soundGrid.add(muteB, 0, 0);

        // set gui updating
        d(maintain(ps.duration,     Util::formatDuration, totTime.textProperty()));
        d(maintain(ps.currentTime,  t -> timeChanged()));
        d(maintain(ps.status,       s -> statusChanged(s)));
        d(maintain(ps.loopMode,     l -> loopModeChanged(l)));
        d(maintain(ps.mute,         v -> muteChanged(v, ps.volume.get())));
        d(maintain(ps.volume,       v -> muteChanged(ps.mute.get(), v.doubleValue())));
        d(PLAYBACK.onSeekDone.addS(() -> lastUpdatedTime = Double.MIN_VALUE));
        d(Player.playingItem.onUpdate(this::playingItemChanged));  // add listener
        playingItemChanged(Player.playingItem.get());              // init value

        // drag & drop
        installDrag(
            entireArea, PLAYLIST_PLUS, "Add to active playlist",
            e -> hasAudio(e),
            e -> {
                List<Item> items = getAudioItems(e);
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
                f3.icon(PAUSE);
            } else {
                f3.icon(PLAY);
            }
        }
    }

    private void loopModeChanged(LoopMode looping) {
        if (loopB.getTooltip()==null) loopB.tooltip("ignoredText"); // lazy init
        loopB.getTooltip().setText(mapRef(looping,
            LoopMode.OFF, LoopMode.PLAYLIST, LoopMode.SONG, LoopMode.RANDOM,
            "Loop mode: off", "Loop mode: loop playlist", "Loop mode: loop song", "Play mode: random")
        );
        loopB.icon(mapRef(looping,
            LoopMode.OFF, LoopMode.PLAYLIST, LoopMode.SONG, LoopMode.RANDOM,
            REPEAT_OFF, MaterialDesignIcon.REPEAT, REPEAT_ONCE, RANDOM)
        );
    }

    private void muteChanged(boolean mute, double volume) {
        if (mute) {
            muteB.icon(VOLUME_OFF);
        } else {
            muteB.icon(volume>0.5 ? VOLUME_UP : VOLUME_DOWN);
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