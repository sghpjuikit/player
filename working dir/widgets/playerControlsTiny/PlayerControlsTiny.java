package playerControlsTiny;

import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;
import java.util.List;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playback.PlaybackState;
import sp.it.pl.audio.playback.VolumeProperty;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.gui.Gui;
import sp.it.pl.gui.objects.icon.Icon;
import sp.it.pl.gui.objects.seeker.ChapterDisplayActivation;
import sp.it.pl.gui.objects.seeker.ChapterDisplayMode;
import sp.it.pl.gui.objects.seeker.Seeker;
import sp.it.pl.layout.widget.Widget;
import sp.it.pl.layout.widget.controller.FXMLController;
import sp.it.pl.layout.widget.feature.HorizontalDock;
import sp.it.pl.layout.widget.feature.PlaybackFeature;
import sp.it.pl.util.access.V;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.graphics.drag.DragUtil;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PAUSE;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PLAY;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.RANDOM;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STEP_BACKWARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STEP_FORWARD;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.STOP;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_DOWN;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_OFF;
import static de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.VOLUME_UP;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.PLAYLIST_PLUS;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.REPEAT_OFF;
import static de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon.REPEAT_ONCE;
import static java.lang.Double.max;
import static javafx.animation.Animation.INDEFINITE;
import static javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER;
import static javafx.scene.layout.Priority.ALWAYS;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.UNKNOWN;
import static javafx.util.Duration.seconds;
import static sp.it.pl.util.Util.clip;
import static sp.it.pl.util.Util.formatDuration;
import static sp.it.pl.util.functional.Util.mapRef;
import static sp.it.pl.util.graphics.Util.layStack;
import static sp.it.pl.util.graphics.drag.DragUtil.installDrag;
import static sp.it.pl.util.reactive.Util.maintain;

/** Controller for mini playback controls widget. */
@Widget.Info(
    name = "Playback Mini",
    author = "Martin Polakovic",
    howto = "Playback actions:\n"
          + "    Control Playback\n"
          + "    Drop audio files : Adds or plays the files\n"
          + "    Left click : Seek - move playback to seeked position\n"
          + "    Mouse drag : Seek (on release)\n"
          + "    Right click : Cancel seek\n"
          + "    Drop audio files : Adds or plays the files\n"
          + "\nChapter actions:\n"
          + "    Right click : Create chapter\n"
          + "    Right click chapter : Open chapter\n"
          + "    Mouse hover chapter (optional) : Open chapter\n",
    description = "Minimalistic playback control widget.",
    notes = "",
    version = "1",
    year = "2015",
    group = Widget.Group.PLAYBACK
)
public class PlayerControlsTiny extends FXMLController implements PlaybackFeature, HorizontalDock {

    private static final double ICON_SIZE = 14;

    @FXML AnchorPane root;
    @FXML HBox layout, controlBox, volBox;
    @FXML Slider volume;
    @FXML Label currTime;
    Label scrollLabel = new Label("");
    Seeker seeker = new Seeker();
    Icon prevB = new Icon(STEP_BACKWARD, ICON_SIZE, null, PlaylistManager::playPreviousItem),
         playB = new Icon(null, ICON_SIZE+3, null, Player::pause_resume),
         stopB = new Icon(STOP, ICON_SIZE, null, Player::stop),
         nextB = new Icon(STEP_FORWARD, ICON_SIZE, null, PlaylistManager::playNextItem),
         loopB = new Icon(null, ICON_SIZE, null, (MouseEvent e) -> Player.toggleLoopMode(e)),
         volB  = new Icon(null, ICON_SIZE, null, Player::toggleMute);
    Anim scroller;
    double lastUpdatedTime = Double.MIN_VALUE; // reduces time update events

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

    @Override
    public void init() {
        PlaybackState ps = Player.state.playback;

        // volume
        volume.setMin(ps.volume.getMin());
        volume.setMax(ps.volume.getMax());
        volume.setBlockIncrement(VolumeProperty.STEP);
        volume.setValue(ps.volume.get());
        volume.valueProperty().bindBidirectional(ps.volume);
        d(volume.valueProperty()::unbind);

        // seeker
        d(seeker.bindTime(ps.duration, ps.currentTime));
        d(maintain(Gui.snapDistance, seeker.chapterSnapDistance));
        layout.getChildren().add(2,seeker);
        HBox.setHgrow(seeker, ALWAYS);

        // icons
        controlBox.getChildren().addAll(prevB,playB,stopB,nextB,new Label(),loopB);
        volBox.getChildren().add(0,volB);

        ScrollPane scrollerPane = new ScrollPane(layStack(scrollLabel, Pos.CENTER));
        double scrollWidth = 200;
        scrollerPane.setPrefWidth(scrollWidth);
        scrollerPane.setPannable(false);
        scrollerPane.setFitToHeight(true);
        scrollerPane.setVbarPolicy(NEVER);
        scrollerPane.setHbarPolicy(NEVER);
        ((Pane)currTime.getParent()).getChildren().add(((Pane) currTime.getParent()).getChildren().indexOf(currTime)+1, scrollerPane);
        scroller = new Anim(seconds(5), scrollerPane::setHvalue)
                .intpl(x -> clip(0,x*1.5-0.25,1)); // linear, but waits a bit around 0 and 1
        scroller.setAutoReverse(true);
        scroller.setCycleCount(INDEFINITE);
        scroller.play();
        d(maintain(scrollLabel.widthProperty(), w -> scroller.setRate(50/max(50,w.doubleValue()-scrollWidth))));    // maintain constant speed
        d(scroller::stop);

        // monitor properties and update graphics
        d(maintain(ps.volume, v -> muteChanged(ps.mute.get(), v.doubleValue())));
        d(maintain(ps.mute, m -> muteChanged(m, ps.volume.get())));
        d(maintain(ps.status, this::statusChanged));
        d(maintain(ps.currentTime, t -> currentTimeChanged()));
        d(maintain(ps.loopMode,this::loopModeChanged));
        d(Player.onSeekDone.addS(() -> lastUpdatedTime = Double.MIN_VALUE));
        d(Player.playingItem.onUpdate(this::playbackItemChanged));
        playbackItemChanged(Player.playingItem.get());

        // drag & drop
        installDrag(
            root, PLAYLIST_PLUS, "Add to active playlist",
            DragUtil::hasAudio,
            e -> {
                List<Item> items = DragUtil.getAudioItems(e);
                PlaylistManager.use(playDropped ? p -> p.setNplay(items) : p -> p.addItems(items));
            }
        );
    }

    @Override
    public void refresh() { }

    @FXML
    private void cycleElapsed() {
        elapsedTime = !elapsedTime;
        currentTimeChanged();
    }

    private void playbackItemChanged(Metadata m) {
        lastUpdatedTime = Double.MIN_VALUE;
        seeker.reloadChapters(m);
        scrollLabel.setText(m.getArtistOrEmpty() + " - " + m.getTitleOrEmpty());
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
        double millis = Player.state.playback.currentTime.get().toMillis();
        if (lastUpdatedTime+1000 <= millis) {
            lastUpdatedTime = millis;
            if (elapsedTime) {
                Duration elapsed = Player.state.playback.currentTime.get();
                currTime.setText(formatDuration(elapsed));
            } else {
                if (Player.state.playback.duration.get() == null) return;
                Duration remaining = Player.state.playback.getRemainingTime();
                currTime.setText("- " + formatDuration(remaining));
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
}