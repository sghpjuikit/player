package audio.playback;

import audio.Item;
import audio.Player;
import audio.playback.player.GeneralPlayer;
import audio.playlist.Playlist;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistManager;
import audio.playlist.sequence.PlayingSequence;
import audio.playlist.sequence.PlayingSequence.LoopMode;
import audio.tagging.MetadataWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.action.IsAction;
import util.action.IsActionable;
import util.conf.Configurable;
import util.conf.IsConfig;
import util.conf.IsConfigurable;
import util.system.Environment;
import util.math.Portion;
import util.reactive.SetƑ;
import util.validation.Constraint;
import static audio.playback.PlayTimeHandler.*;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.util.Duration.millis;
import static util.async.Async.runFX;

/**
 * Provides methods for player.
 */
@IsActionable
@IsConfigurable("Playback")
public final class PLAYBACK implements Configurable {

	private static final Logger LOGGER = LoggerFactory.getLogger(PLAYBACK.class);

	@IsConfig(name = "Remember playback state", info = "Continue last remembered playback when application starts.")
	public static boolean continuePlaybackOnStart = true;
	@IsConfig(name = "Pause playback on start", info = "Continue last remembered playback paused on application start.")
	public static boolean continuePlaybackPaused = false;
	@IsConfig(name = "Seek time unit", info = "Fixed time unit to jump, when seeking forward/backward.")
	public static Duration seekUnitT = millis(4000);
	@Constraint.MinMax(min = 0, max = 1)
	@IsConfig(name = "Seek fraction", info = "Relative time in fraction of song's length to seek forward/backward by.")
	public static double seekUnitP = 0.05;

	public static final PlaybackState state = Player.state.playback;
	private static final GeneralPlayer player = new GeneralPlayer();

	/** Initializes the Playback. */
	public static void initialize() {
		player.realTime.initialize();
		onPlaybackAt.add(at(new Portion(1), () -> onPlaybackAt.forEach(h -> h.restart(Player.playingItem.get().getLength()))));
		onPlaybackEnd.add(() -> {
			switch (state.loopMode.get()) {
				case OFF: stop();
					break;
				case PLAYLIST: PlaylistManager.playNextItem();
					break;
				case SONG: seek(Duration.ZERO);
					break;
				default:
			}
		});
	}

	/** Initialize state from last session */
	public static void loadLastState() {
		if (!continuePlaybackOnStart) return;
		if (PlaylistManager.use(Playlist::getPlaying, null)==null) return;

		if (continuePlaybackPaused)
			state.status.set(Status.PAUSED);

		activate();
	}

	public static void suspend() {
		suspension_flag = true;
		LOGGER.info("Suspending playback");
		state.realTime.set(getRealTime());
		Player.state.serialize();
		player.dispose();
	}

	public static void activate() {
		LOGGER.info("Activating playback");
		post_activating = true;
		Status s = state.status.get();
		if (s==PAUSED || s==PLAYING)
			startTime = state.currentTime.get();
		if (s==PAUSED) {
			// THIS NEEDS TO GET FIXED
			player.play(PlaylistManager.use(Playlist::getPlaying, null));
			util.async.Async.runFX(1000, player::pause);
		}
		if (s==PLAYING) {
			player.play(PlaylistManager.use(Playlist::getPlaying, null));
			// suspension_flag = false; // set inside player.play();
			runFX(200, () -> suspension_flag = false); // just in case som condition prevents resetting flag
		} else {
			suspension_flag = false;
		}
	}

	public static Duration startTime = null;
	// this prevents onTime handlers to reset after playback activation
	// the suspension-activation should undergo as if it never happen
	public static boolean post_activating = false;
	// this negates the above when app starts and playback is activated 1st time
	public static boolean post_activating_1st = true;
	// this prevents update/change playing song events on suspension/activating, it is important (see where it is used)
	public static boolean suspension_flag = false;

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Starts player of item.
	 * <p/>
	 * It is safe to assume that application will have updated currently played
	 * item after this method is invoked. The same is not guaranteed for cached
	 * metadata of this item.
	 * <p/>
	 * Immediately after method is invoked, real time and current time are 0 and
	 * all current song related information are updated and can be assumed to be
	 * correctly initialized.
	 * <p/>
	 * Invocation of this method fires playbackStart event.
	 *
	 * @param item to play
	 */
	public static void play(PlaylistItem item) {
		player.play(item);
	}

	/** Resumes player, if file is being played. Otherwise does nothing. */
	@IsAction(name = "Resume", desc = "Resumes playback, if file is being played.", global = true)
	public static void resume() {
		player.resume();
	}

	/** Pauses player, if already paused, does nothing. */
	@IsAction(name = "Pause", desc = "Pauses playback, if file is being played.", global = true)
	public static void pause() {
		player.pause();
	}

	/** Pauses/resumes player, if file is being played. Otherwise does nothing. */
	@IsAction(name = "Pause/resume", desc = "Pauses/resumes playback, if file is being played.", keys = "ALT+S", global = true)
	public static void pause_resume() {
		player.pause_resume();
	}

	/** Stops player. */
	@IsAction(name = "Stop", desc = "Stops playback.", keys = "ALT+F", global = true)
	public static void stop() {
		player.stop();
	}

	/** Seeks player to position specified by duration parameter. */
	public static void seek(Duration duration) {
		player.seek(duration);
	}

	/** Seeks player to position specified by percent value 0-1. */
	public static void seek(double at) {
		if (at<0 || at>1) throw new IllegalArgumentException("Seek value must be 0-1");
		seek(getTotalTime().multiply(at));
		if (state.status.get()==PAUSED) player.pause_resume();
	}

	/** Seek forward by specified duration */
	@IsAction(name = "Seek to beginning", desc = "Seek playback to beginning.", keys = "ALT+R", global = true)
	public static void seekZero() {
		seek(0);
	}

	public static void seekForward(Seek type) {
		if (type==Seek.ABSOLUTE) seekForwardAbsolute();
		else seekForwardRelative();
	}

	/** Seek forward by small duration unit. */
	@IsAction(name = "Seek forward", desc = "Seek forward playback by small duration unit.", keys = "ALT+D", repeat = true, global = true)
	public static void seekForwardAbsolute() {
		seek(getCurrentTime().add(seekUnitT));
	}

	/** Seek forward by small fraction unit. */
	@IsAction(name = "Seek forward (%)", desc = "Seek forward playback by fraction.", keys = "SHIFT+ALT+D", repeat = true, global = true)
	public static void seekForwardRelative() {
		double d = getCurrentTime().toMillis()/getTotalTime().toMillis() + seekUnitP;
		seek(min(d, 1));
	}

	public static void seekBackward(Seek type) {
		if (type==Seek.ABSOLUTE) seekBackwardAbsolute();
		else seekBackwardRelative();
	}

	/** Seek backward by small duration unit. */
	@IsAction(name = "Seek backward", desc = "Seek backward playback by small duration unit.", keys = "ALT+A", repeat = true, global = true)
	public static void seekBackwardAbsolute() {
		seek(getCurrentTime().subtract(seekUnitT));
	}

	/** Seek backward by small fraction unit. */
	@IsAction(name = "Seek backward (%)", desc = "Seek backward playback by fraction.", keys = "SHIFT+ALT+A", repeat = true, global = true)
	public static void seekBackwardRelative() {
		double d = getCurrentTime().toMillis()/getTotalTime().toMillis() - seekUnitP;
		seek(max(d, 0));
	}

	/** Seek forward by specified duration */
	@IsAction(name = "Seek to end", desc = "Seek playback to end.", global = true)
	public static void seekEnd() {
		seek(1);
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/** Increment volume by elementary unit. */
	@IsAction(name = "Volume up", desc = "Increment volume by elementary unit.", keys = "CTRL+SHIFT+2", repeat = true, global = true)
	public static void volumeInc() {
		state.volume.inc();
	}

	/** Decrement volume by elementary unit. */
	@IsAction(name = "Volume down", desc = "Decrement volume by elementary unit.", keys = "CTRL+SHIFT+1", repeat = true, global = true)
	public static void volumeDec() {
		state.volume.dec();
	}

	/** Increment balance by elementary unit. */
	@IsAction(name = "Balance right", desc = "Shift balance to right by elementary unit.", repeat = true)
	public static void balanceRight() {
		state.balance.left();
	}

	/** Decrement balance by elementary unit. */
	@IsAction(name = "Balance left", desc = "Shift balance to left by elementary unit.", repeat = true)
	public static void balanceLeft() {
		state.balance.right();
	}

	public static PlayingSequence.LoopMode getLoopMode() {
		return state.loopMode.get();
	}

	@IsAction(name = "Toggle looping", desc = "Switch between playlist looping mode.", keys = "ALT+L")
	public static void toggleLoopMode() {
		setLoopMode(getLoopMode().next());
	}

	public static void toggleLoopMode(MouseEvent e) {
		if (e.getButton()==PRIMARY) PLAYBACK.setLoopMode(PLAYBACK.getLoopMode().next());
		if (e.getButton()==SECONDARY) PLAYBACK.setLoopMode(PLAYBACK.getLoopMode().previous());
	}

	public static void setLoopMode(LoopMode mode) {
		state.loopMode.set(mode);
		PlaylistManager.playingItemSelector.setSelector(mode.selector());
	}

	public static ObjectProperty<LoopMode> loopModeProperty() {
		return state.loopMode;
	}

	public static MediaPlayer.Status getStatus() {
		return state.status.get();
	}

	public static ObjectProperty<Status> statusProperty() {
		return state.status;
	}

	public static Duration getCurrentTime() {
		return state.currentTime.get();
	}

	public static ObjectProperty<Duration> currentTimeProperty() {
		return state.currentTime;
	}

	public static Duration getRealTime() {
		return player.realTime.get();
	}

	public static RealTimeProperty realTimeProperty() {
		return player.realTime;
	}

	public static Duration getRemainingTime() {
		return getTotalTime().subtract(getCurrentTime());
	}

	public static Duration getTotalTime() {
		return state.duration.get();
	}

	public static ObjectProperty<Duration> totalTimeProperty() {
		return state.duration;
	}

	public static double getPosition() {
		return getCurrentTime().toMillis()/getTotalTime().toMillis();
	}

	public static double getPlayedPercentage() {
		return getRealTime().toMillis()/getTotalTime().toMillis();
	}

	public static double getRate() {
		return state.rate.get();
	}

	public static void setRate(double value) {
		state.rate.set(value);
	}

	public static DoubleProperty rateProperty() {
		return state.rate;
	}

	/** Switches between on/off state for mute property. */
	@IsAction(name = "Toggle mute", desc = "Switch mute on/off.", keys = "ALT+M")
	public static void toggleMute() {
		state.mute.set(!state.mute.get());
	}

	/**
	 * Rates playing item specified by percentage rating.
	 *
	 * @param rating <0,1> representing percentage of the rating, 0 being minimum and 1 maximum possible rating for
	 * current item.
	 */
	public static void rate(double rating) {
		if (PlaylistManager.active==null) return;
		MetadataWriter.useToRate(Player.playingItem.get(), rating);
	}

	/** Rate playing item 0/5. */
	@IsAction(name = "Rate playing 0/5", desc = "Rate currently playing item 0/5.", keys = "ALT+BACK_QUOTE", global = true)
	public static void rate0() {
		rate(0);
	}

	/** Rate playing item 1/5. */
	@IsAction(name = "Rate playing 1/5", desc = "Rate currently playing item 1/5.", keys = "ALT+1", global = true)
	public static void rate1() {
		rate(0.2);
	}

	/** Rate playing item 2/5. */
	@IsAction(name = "Rate playing 2/5", desc = "Rate currently playing item 2/5.", keys = "ALT+2", global = true)
	public static void rate2() {
		rate(0.4);
	}

	/** Rate playing item 3/5. */
	@IsAction(name = "Rate playing 3/5", desc = "Rate currently playing item 3/5.", keys = "ALT+3", global = true)
	public static void rate3() {
		rate(0.6);
	}

	/** Rate playing item 4/5. */
	@IsAction(name = "Rate playing 4/5", desc = "Rate currently playing item 4/5.", keys = "ALT+4", global = true)
	public static void rate4() {
		rate(0.8);
	}

	/** Rate playing item 5/5. */
	@IsAction(name = "Rate playing 5/5", desc = "Rate currently playing item 5/5.", keys = "ALT+5", global = true)
	public static void rate5() {
		rate(1);
	}

	/** Explore current item directory - opens file browser for its location. */
	@IsAction(name = "Explore current item directory", desc = "Explore current item directory.", keys = "ALT+V", global = true)
	public static void openPlayedLocation() {
		if (PlaylistManager.active==null) return;
		Item i = PlaylistManager.use(Playlist::getPlaying, null);
		if (i!=null) Environment.browse(i.getUri());
	}

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Set of actions that execute when song starts playing. Seeking to song start doesn't activate this event.
	 * <p/>
	 * It is not safe to assume that application's information on currently played
	 * item will be updated before this event. Therefore using cached information
	 * can result in misbehavior due to outdated information.
	 */
	public static final SetƑ onPlaybackStart = new SetƑ();

	/**
	 * Set of actions that will execute when song playback seek completes.
	 */
	public static final SetƑ onSeekDone = new SetƑ();

	/**
	 * Set of actions that will execute when song playback ends.
	 * <p/>
	 * It is safe to use in-app cache of currently played item inside
	 * the behavior parameter.
	 */
	public static final SetƑ onPlaybackEnd = new SetƑ();

	/**
	 * Set of time-specific actions that individually execute when song playback reaches point of handler's interest.
	 */
	public static final List<PlayTimeHandler> onPlaybackAt = new ArrayList<>();

/* ------------------------------------------------------------------------------------------------------------------ */

	/**
	 * Playing item spectrum distributor.
	 * Register listeners by adding it into collection. Adding listener multiple times has no
	 * effect.
	 */
	public static final Set<AudioSpectrumListener> spectrumListeners = new HashSet<>();

	/**
	 * Only one spectrum listener is allowed per player (MediaPlayer) object. Here
	 * re-registering and distributing of the event is handled.
	 * <p/>
	 * Playback has main spectrum listener registered only if there is at least one
	 * listener registered in the listener list.
	 */
	public final static AudioSpectrumListener spectrumListenerDistributor = (double d, double d1, float[] floats, float[] floats1) ->
		spectrumListeners.forEach(l -> l.spectrumDataUpdate(d, d1, floats, floats1));

	public enum Seek {
		ABSOLUTE, RELATIVE
	}

}