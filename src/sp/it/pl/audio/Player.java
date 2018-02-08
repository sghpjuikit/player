package sp.it.pl.audio;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.AudioSpectrumListener;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import org.reactfx.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sp.it.pl.audio.playback.GeneralPlayer;
import sp.it.pl.audio.playback.PlayTimeHandler;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.playlist.sequence.PlayingSequence;
import sp.it.pl.audio.playlist.sequence.PlayingSequence.LoopMode;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.audio.tagging.MetadataReader;
import sp.it.pl.audio.tagging.MetadataWriter;
import sp.it.pl.layout.widget.controller.io.InOutput;
import sp.it.pl.util.action.IsAction;
import sp.it.pl.util.action.IsActionable;
import sp.it.pl.util.async.executor.EventReducer;
import sp.it.pl.util.async.executor.FxTimer;
import sp.it.pl.util.async.future.Fut;
import sp.it.pl.util.collections.mapset.MapSet;
import sp.it.pl.util.conf.IsConfig;
import sp.it.pl.util.conf.IsConfigurable;
import sp.it.pl.util.math.Portion;
import sp.it.pl.util.reactive.SetƑ;
import sp.it.pl.util.validation.Constraint;
import static java.lang.Double.max;
import static java.lang.Double.min;
import static java.util.concurrent.TimeUnit.MINUTES;
import static javafx.scene.input.MouseButton.PRIMARY;
import static javafx.scene.input.MouseButton.SECONDARY;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.util.Duration.millis;
import static sp.it.pl.audio.playback.PlayTimeHandler.at;
import static sp.it.pl.main.AppUtil.APP;
import static sp.it.pl.util.async.AsyncKt.FX;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.async.AsyncKt.runNew;
import static sp.it.pl.util.async.AsyncKt.threadFactory;
import static sp.it.pl.util.async.executor.EventReducer.toLast;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.system.EnvironmentKt.browse;

@SuppressWarnings("unused")
@IsActionable
@IsConfigurable("Playback")
public class Player {

	private static final Logger LOGGER = LoggerFactory.getLogger(Player.class);

	public static final ExecutorService IO_THREAD = new ThreadPoolExecutor(0, 8, 10, MINUTES, new LinkedBlockingQueue<>(), threadFactory("io-thread", true));
	public static final PlayerState state = PlayerState.deserialize();
	public static final CurrentItem playingItem = new CurrentItem();
	public static final GeneralPlayer player = new GeneralPlayer(state);
	public static final InOutput<Metadata> playing = new InOutput<>(UUID.fromString("876dcdc9-48de-47cd-ab1d-811eb5e95158"), "Playing", Metadata.class);
	public static final InOutput<PlaylistItem> playlistSelected = new InOutput<>(UUID.fromString("ca002c1d-8689-49f6-b1a0-0d0f8ff2e2a8"), "Selected in playlist", PlaylistItem.class);
	public static final InOutput<Metadata> librarySelected = new InOutput<>(UUID.fromString("ba002c1d-2185-49f6-b1a0-0d0f8ff2e2a8"), "Selected in Library", Metadata.class);
	public static final InOutput<Item> anySelected = new InOutput<>(UUID.fromString("1a01ca96-2e60-426e-831d-93b24605595f"), "Selected anywhere", Item.class);

	@IsConfig(name = "Remember playback state", info = "Continue last remembered playback when application starts.")
	public static boolean continuePlaybackOnStart = true;
	@IsConfig(name = "Pause playback on start", info = "Continue last remembered playback paused on application start.")
	public static boolean continuePlaybackPaused = false;
	@IsConfig(name = "Seek time unit", info = "Fixed time unit to jump, when seeking forward/backward.")
	public static Duration seekUnitT = millis(4000);
	@Constraint.MinMax(min = 0, max = 1)
	@IsConfig(name = "Seek fraction", info = "Relative time in fraction of song's length to seek forward/backward by.")
	public static double seekUnitP = 0.05;

	public static void initialize() {
		anySelected.i.bind(playlistSelected.o);
		anySelected.i.bind(librarySelected.o);
		playingItem.onUpdate(playing.i::setValue);

		// use jaudiotagger for total time value (fixes incorrect values coming from player classes)
		playingItem.onChange(m -> state.playback.duration.set(m.getLength()));
		// maintain PLAYED_FIRST_TIME & PLAYED_LAST_TIME metadata
		// note: for performance reasons we update AFTER song stops playing, not WHEN it starts
		// as with playcount incrementing, it could disrupt playback, although now we are losing
		// updates on application closing!
		playingItem.onChange((o, n) ->
			MetadataWriter.use(o, w -> {
				w.setPlayedFirstNowIfEmpty();
				w.setPlayedLastNow();
			})
		);

		player.realTime.initialize();
		onPlaybackAt.add(at(new Portion(1), () -> onPlaybackAt.forEach(h -> h.restart(Player.playingItem.get().getLength())))); // TODO: fix possible StackOverflowError
		onPlaybackEnd.add(() -> {
			switch (state.playback.loopMode.get()) {
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

	public static void dispose() {
		player.dispose();
	}

	/** Initialize state from last session */
	public static void loadLastState() {
		if (!continuePlaybackOnStart) return;
		if (PlaylistManager.use(Playlist::getPlaying, null)==null) return;

		if (continuePlaybackPaused)
			state.playback.status.set(Status.PAUSED);

		activate();
	}

	public static class CurrentItem {
		private Metadata val = Metadata.EMPTY;
		private Metadata valNext = Metadata.EMPTY;
		private final FxTimer valNextLoader = new FxTimer(400, 1, () -> preloadNext());
		private final List<BiConsumer<Metadata,Metadata>> changes = new ArrayList<>();
		private final List<BiConsumer<Metadata,Metadata>> updates = new ArrayList<>();

		/**
		 * Returns the playing item and all its information.
		 * <p/>
		 * Note: It is always safe to call this method, even during playing item
		 * change events.
		 */
		public Metadata get() {
			return val;
		}

		private void set(boolean change, Metadata new_metadata) {
			Metadata ov = val;
			Metadata nv = new_metadata;
			val = nv;

			// There is a small problem
			// During tagging it is possible the playback needs to be suspended and activated
			// This unfortunately cascades and fires this method, but suspending/activating
			// should be transparent to playback song change/update events (not when app starts,
			// only when tagging)
			//
			// This can lead to dangerous situations (rarely) for example when tagging suspends
			// playback and calls this method and there is a listener to this which calls tagging
			// this will cause infinite loop!
			//
			// for now we will use flag as dirty solution
			if (suspension_flag) return;

			if (change) changes.forEach(h -> h.accept(ov, nv));
			updates.forEach(h -> h.accept(ov, nv));
		}

		public Subscription onChange(BiConsumer<Metadata,Metadata> bc) {
			changes.add(bc);
			return () -> changes.remove(bc);
		}

		/**
		 * Add behavior to playing item changed event.
		 * <p/>
		 * The event is fired every time playing item changes. This includes
		 * replaying the same item.
		 * <p/>
		 * Use in cases requiring constantly updated information about the playing
		 * item.
		 * <p/>
		 * Note: It is safe to call {@link #get()} method when this even fires.
		 * It has already been updated.
		 */
		public Subscription onChange(Consumer<Metadata> bc) {
			return onChange((o, n) -> bc.accept(n));
		}

		/**
		 * Add behavior to playing item updated event.
		 * <p/>
		 * The event is fired every time playing item changes or even if some of its
		 * metadata is changed such artist or rating. More eager version of change
		 * event.
		 * <p/>
		 * Use in cases requiring not only change updates, but also constantly
		 * (real time) updated information about the playing item, such as when
		 * displaying this information somewhere - for example artist of the
		 * played item.
		 * <p/>
		 * Do not use when only the identity (defined by its URI) of the played
		 * item is required. For example lastFM scrobbling service would not want
		 * to update played item status when the metadata of the item change as it
		 * is not a change in played item - it is still the same item.
		 * <p/>
		 * Note: It is safe to call {@link #get()} method when this even fires.
		 * It has already been updated.
		 */
		public Subscription onUpdate(Consumer<Metadata> bc) {
			return onUpdate((o, n) -> bc.accept(n));
		}

		public Subscription onUpdate(BiConsumer<Metadata,Metadata> bc) {
			updates.add(bc);
			return () -> updates.remove(bc);
		}

		public void update() {
			load(false, val);
		}

		public void update(Metadata m) {
			set(false, m);
		}

		/** Execute when song starts playing. */
		public void itemChanged(Item item) {
			if (item==null) {
				set(true, Metadata.EMPTY);
				log(Player.class).info("Current item metadata set to empty. No item playing.");
			}
			// if same item, still fire change
			else if (val.same(item)) {
				set(true, val);
				log(Player.class).info("Current item metadata reused. Same item playing.");
			}
			// if pre-loaded, set
			else if (valNext.same(item)) {
				set(true, valNext);
				log(Player.class).info("Current item metadata copied from next item metadata cache.");
				// else load
			} else {
				log(Player.class).info("Next item metadata cache copy failed - content does not correspond to correct item. Loading now...");
				load(true, item);
			}

			// wait 400ms, preload metadata for next item
			valNextLoader.start();
		}

		// load metadata, type indicates UPDATE vs CHANGE
		private void load(boolean changeType, Item item) {
			Fut.fut(item)
				.map(Player.IO_THREAD, MetadataReader::readMetadata)
				.use(FX, m -> set(changeType, m.isEmpty() ? item.toMeta() : m));
		}

		private void preloadNext() {
			log(Player.class).info("Pre-loading metadata for next item to play.");

			PlaylistItem next = PlaylistManager.use(Playlist::getNextPlaying, null);
			if (next!=null) {
				Fut.fut(next)
					.map(Player.IO_THREAD, MetadataReader::readMetadata)
					.use(FX, m -> valNext = m);
			}
		}
	}

	/**
	 * Adds songs refreshed event handler. When application updates song metadata it will fire this
	 * event. For example widget displaying song information may update that information, if the
	 * event contains the song.
	 * <p/>
	 * Say, there is a widget with an input, displaying its value and sending it to its output, for
	 * others to listen. And both are of {@link Item} type and updating the contents may be heavy
	 * operation we want to avoid unless necessary.
	 * Always update the output. It will update all inputs (of other widgets) bound to it. However,
	 * there may be widgets whose inputs are not bound, but set manually. In order to not update the
	 * input multiple times (by us and because it is bound) it should be checked whether input is
	 * bound and then handled accordingly.
	 * <p/>
	 * But because the input can be bound to multiple outputs, we must check whether the input is
	 * bound to the particular output it is displaying value of (and would be auto-updated from).
	 * This is potentially complex or impossible (with unfortunate
	 * {@link Object#equals(java.lang.Object) } implementation).
	 * It is possible to use an {@link EventReducer} which will
	 * reduce multiple events into one, in such case always updating input is recommended.
	 */
	public static Subscription onItemRefresh(Consumer<MapSet<URI,Metadata>> handler) {
		refreshHandlers.add(handler);
		return () -> refreshHandlers.remove(handler);
	}

	/** Singleton variant of {@link #refreshItems(java.util.Collection)}. */
	public static void refreshItem(Item i) {
		noØ(i);
		refreshItems(list(i));
	}

	/**
	 * Read metadata from tag of all items and invoke {@link #refreshItemsWith(java.util.List)}.
	 * <p/>
	 * Safe to call from any thread.
	 * <p/>
	 * Use when metadata of the items changed.
	 */
	public static void refreshItems(Collection<? extends Item> is) {
		noØ(is);
		if (is.isEmpty()) return;

		runNew(MetadataReader.buildReadMetadataTask(is, (ok, m) -> {
			if (ok) refreshItemsWith(m);
		}));
	}

	/** Singleton variant of {@link #refreshItemsWith(java.util.List)}. */
	public static void refreshItemWith(Metadata m) {
		noØ(m);
		refreshItemsWith(list(m));
	}

	/** Singleton variant of {@link #refreshItemsWith(java.util.List, boolean)}. */
	public static void refreshItemWith(Metadata m, boolean allowDelay) {
		noØ(m);
		refreshItemsWith(list(m), allowDelay);
	}

	/** Simple version of {@link #refreshItemsWith(java.util.List, boolean) } with false argument. */
	public static void refreshItemsWith(List<Metadata> ms) {
		refreshItemsWith(ms, false);
	}

	/**
	 * Updates application (playlist, library, etc.) with latest metadata. Refreshes the given
	 * data for the whole application.
	 * <p/>
	 * Safe to call from any thread.
	 *
	 * @param ms metadata to refresh
	 * @param allowDelay flag for using delayed refresh to reduce refresh successions to single refresh. Normally false
	 * is used.
	 * <p/>
	 * Use false to refresh immediatelly and true to queue the refresh for future execution (will wait few seconds for
	 * next refresh request and if it comes, will wait again and so on until none will come, which is when all queued
	 * refreshes execute all at once).
	 */
	public static void refreshItemsWith(List<Metadata> ms, boolean allowDelay) {
		noØ(ms);
		if (allowDelay) runFX(() -> red.push(ms));
		else refreshItemsWithNow(ms);
	}

	// processes delayed refreshes - queues them up and invokes refresh after some time
	// use only on fx thread
	private static EventReducer<List<Metadata>> red = toLast(3000, (o, n) -> {
		n.addAll(o);
		return n;
	}, Player::refreshItemsWithNow);

	private static final List<Consumer<MapSet<URI,Metadata>>> refreshHandlers = new ArrayList<>();

	// runs refresh on bgr thread, thread safe
	private static void refreshItemsWithNow(List<Metadata> ms) {
		noØ(ms);
		if (ms.isEmpty()) return;

		// always on br thread
		IO_THREAD.execute(() -> {
			// metadata map hashed with resource identity : O(n^2) -> O(n)
			MapSet<URI,Metadata> mm = new MapSet<>(Metadata::getUri, ms);

			// update library
			APP.db.addItems(ms);

			runFX(() -> {
				// update all playlist items referring to this updated metadata
				PlaylistManager.playlists.stream().flatMap(Playlist::stream).forEach((PlaylistItem p) -> mm.ifHasK(p.getUri(), p::update));
//                PlaylistManager.playlists.forEach(playlist -> playlist.forEach(p -> mm.ifHasK(p.getURI(), p::update)));

				// refresh playing item data
				mm.ifHasE(playingItem.get(), playingItem::update);

				if (playing.i.getValue()!=null) mm.ifHasE(playing.i.getValue(), playing.i::setValue);
				if (playlistSelected.i.getValue()!=null)
					mm.ifHasK(playlistSelected.i.getValue().getUri(), m -> playlistSelected.i.setValue(m.toPlaylist()));
				if (librarySelected.i.getValue()!=null)
					mm.ifHasE(librarySelected.i.getValue(), librarySelected.i::setValue);

				// refresh rest
				refreshHandlers.forEach(h -> h.accept(mm));
			});
		});
	}

	public static void suspend() {
		LOGGER.info("Suspending playback");

		suspension_flag = true;
		player.dispose();
	}

	public static void activate() {
		LOGGER.info("Activating playback");

		post_activating = true;
		Status s = state.playback.status.get();
		if (s==PAUSED || s==PLAYING)
			startTime = state.playback.currentTime.get();
		if (s==PAUSED) {
			// THIS NEEDS TO GET FIXED
			player.play(PlaylistManager.use(Playlist::getPlaying, null));
			runFX(1000, player::pause);
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
		player.pauseResume();
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
		seek(state.playback.duration.get().multiply(at));
		if (state.playback.status.get()==PAUSED) player.pauseResume();
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
	@IsAction(name = "Seek forward", desc = "Seek playback forward by small duration unit.", keys = "ALT+D", repeat = true, global = true)
	public static void seekForwardAbsolute() {
		seek(state.playback.currentTime.get().add(seekUnitT));
	}

	/** Seek forward by small fraction unit. */
	@IsAction(name = "Seek forward (%)", desc = "Seek playback forward by fraction.", keys = "SHIFT+ALT+D", repeat = true, global = true)
	public static void seekForwardRelative() {
		double d = state.playback.currentTime.get().toMillis()/state.playback.duration.get().toMillis() + seekUnitP;
		seek(min(d, 1));
	}

	public static void seekBackward(Seek type) {
		if (type==Seek.ABSOLUTE) seekBackwardAbsolute();
		else seekBackwardRelative();
	}

	/** Seek backward by small duration unit. */
	@IsAction(name = "Seek backward", desc = "Seek playback backward by small duration unit.", keys = "ALT+A", repeat = true, global = true)
	public static void seekBackwardAbsolute() {
		seek(state.playback.currentTime.get().subtract(seekUnitT));
	}

	/** Seek backward by small fraction unit. */
	@IsAction(name = "Seek backward (%)", desc = "Seek playback backward by fraction.", keys = "SHIFT+ALT+A", repeat = true, global = true)
	public static void seekBackwardRelative() {
		double d = state.playback.currentTime.get().toMillis()/state.playback.duration.get().toMillis() - seekUnitP;
		seek(max(d, 0));
	}

	/** Seek forward by specified duration */
	@IsAction(name = "Seek to end", desc = "Seek playback to end.", global = true)
	public static void seekEnd() {
		seek(1);
	}

	/** Increment volume by elementary unit. */
	@IsAction(name = "Volume up", desc = "Increment volume by elementary unit.", keys = "CTRL+SHIFT+2", repeat = true, global = true)
	public static void volumeInc() {
		state.playback.volume.incByStep();
	}

	/** Decrement volume by elementary unit. */
	@IsAction(name = "Volume down", desc = "Decrement volume by elementary unit.", keys = "CTRL+SHIFT+1", repeat = true, global = true)
	public static void volumeDec() {
		state.playback.volume.decByStep();
	}

	/** Increment balance by elementary unit. */
	@IsAction(name = "Balance right", desc = "Shift balance to right by elementary unit.", repeat = true)
	public static void balanceRight() {
		state.playback.balance.leftByStep();
	}

	/** Decrement balance by elementary unit. */
	@IsAction(name = "Balance left", desc = "Shift balance to left by elementary unit.", repeat = true)
	public static void balanceLeft() {
		state.playback.balance.rightByStep();
	}

	public static PlayingSequence.LoopMode getLoopMode() {
		return state.playback.loopMode.get();
	}

	@IsAction(name = "Toggle looping", desc = "Switch between playlist looping mode.", keys = "ALT+L")
	public static void toggleLoopMode() {
		setLoopMode(getLoopMode().next());
	}

	public static void toggleLoopMode(MouseEvent e) {
		if (e.getButton()==PRIMARY) setLoopMode(getLoopMode().next());
		if (e.getButton()==SECONDARY) setLoopMode(getLoopMode().previous());
	}

	public static void setLoopMode(LoopMode mode) {
		state.playback.loopMode.set(mode);
		PlaylistManager.playingItemSelector.setSelector(mode.selector());
	}

	/** Switches between on/off state for mute property. */
	@IsAction(name = "Toggle mute", desc = "Switch mute on/off.", keys = "ALT+M")
	public static void toggleMute() {
		state.playback.mute.set(!state.playback.mute.get());
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
		if (i!=null) browse(i.getUri());
	}

}