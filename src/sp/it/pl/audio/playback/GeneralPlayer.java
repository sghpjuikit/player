package sp.it.pl.audio.playback;

import java.util.Objects;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.PlayerState;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.util.animation.Anim;
import static java.lang.Math.pow;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.reactive.Util.installSingletonListener;

/** Audio player which abstracts away from the implementation. */
public class GeneralPlayer {

	private Play p;
	private Item i;
	private final PlayerState state;
	public final RealTimeProperty realTime;

	public GeneralPlayer(PlayerState state) {
		this.state = state;
		this.realTime = new RealTimeProperty(state.playback.duration, state.playback.currentTime);
	}

	synchronized public void play(PlaylistItem item) {
		// Do not recreate player if same song plays again
		// 1) improves performance
		// 2) avoids firing some playback events
		if (p!=null && item.same(i)) {
			seek(ZERO);
			return;
		}

		i = item;
		if (p!=null) p.disposePlayback();
		p = computePlayer(item);
		if (p==null) {
			log(GeneralPlayer.class).info("Player {} can not play item {}", p, item);
			item.playbackError = true;
			PlaylistManager.use(Playlist::playNextItem); // handle within playlist
		} else {
			p.createPlayback(item, state.playback,
					() -> {
						realTime.real_seek = state.playback.realTime.get();
						realTime.curr_sek = ZERO;
						p.play();

						realTime.synchroRealTime_onPlayed();
						// throw item change event
						Player.playingItem.itemChanged(item);
						Player.suspension_flag = false;
						// fire other events (may rely on the above)
						Player.onPlaybackStart.run();
						if (Player.post_activating_1st || !Player.post_activating)
							// bug fix, not updated playlist items can get here, but should not!
							if (item.getTimeMs()>0)
								Player.onPlaybackAt.forEach(t -> t.restart(item.getTime()));
						Player.post_activating = false;
						Player.post_activating_1st = false;
					},
					() -> runFX(() -> {
						log(GeneralPlayer.class).info("Player {} can not play item {}", p, item);
						item.playbackError = true;
						PlaylistManager.use(Playlist::playNextItem); // handle within playlist
					}));
		}
	}

	private Play computePlayer(Item i) {
		return new VlcPlayer();
	}

	public void resume() {
		if (p==null) return;
		p.resume();
		Player.onPlaybackAt.forEach(PlayTimeHandler::unpause);
	}

	public void pause() {
		if (p==null) return;
		p.pause();
		Player.onPlaybackAt.forEach(PlayTimeHandler::pause);
	}

	public void pause_resume() {
		if (p==null) return;
		if (state.playback.status.get()==PLAYING) pause();
		else resume();
	}

	public void stop() {
		if (p==null) return;
		p.stop();

		runFX(() -> {
			Player.playingItem.itemChanged(Metadata.EMPTY);
			realTime.synchroRealTime_onStopped();
			Player.onPlaybackAt.forEach(PlayTimeHandler::stop);
			PlaylistManager.playlists.forEach(p -> p.updatePlayingItem(-1));
			PlaylistManager.active = null;
		});
	}

	public void seek(Duration duration) {
		if (p==null) return;

		Status s = state.playback.status.get();

		if (s==STOPPED) {
			pause();
		}

		doSeek(duration);

		// TODO: enable volume fading on seek
		if (true) return;
		final double currentVolume = state.playback.volume.get();
		if (seekDone) lastValidVolume = currentVolume;
		else { if (volumeAnim!=null) volumeAnim.pause(); }
		seekDone = false;
		new Anim(millis(150), x -> state.playback.volume.set(currentVolume*pow(1 - x, 2)))
				.then(() -> {
					doSeek(duration);
					volumeAnim = new Anim(millis(150), x -> state.playback.volume.set(lastValidVolume*pow(x, 2)))
							.then(() -> seekDone = true);
					volumeAnim.playOpen();
				})
				.playOpen();
	}

	private void doSeek(Duration duration) {
		realTime.synchroRealTime_onPreSeeked();
		state.playback.currentTime.set(duration);    // allow next doSeek() target correct value even if this has not finished
		installSingletonListener(state.playback.currentTime, Objects::nonNull, v -> Player.onSeekDone.run());
		p.seek(duration);
		realTime.synchroRealTime_onPostSeeked(duration);
	}

	private boolean seekDone = true;
	private double lastValidVolume = -1;
	private Anim volumeAnim;

	public void dispose() {
		if (p!=null) {
			p.disposePlayback();
			p.dispose();
			p = null;
		}
	}

	public interface Play {
		void play();

		void pause();

		void resume();

		void seek(Duration duration);

		void stop();

		void createPlayback(Item item, PlaybackState state, Runnable onOK, Runnable onFail);

		/**
		 * Stops playback if any and disposes of the player resources.
		 */
		void disposePlayback();

		void dispose();
	}

}