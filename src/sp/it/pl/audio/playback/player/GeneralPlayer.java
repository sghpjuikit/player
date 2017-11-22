package sp.it.pl.audio.playback.player;

import java.util.Objects;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.playback.PLAYBACK;
import sp.it.pl.audio.playback.PlayTimeHandler;
import sp.it.pl.audio.playback.PlaybackState;
import sp.it.pl.audio.playback.RealTimeProperty;
import sp.it.pl.audio.playlist.Playlist;
import sp.it.pl.audio.playlist.PlaylistItem;
import sp.it.pl.audio.playlist.PlaylistManager;
import sp.it.pl.audio.tagging.Metadata;
import sp.it.pl.util.animation.Anim;
import sp.it.pl.util.file.AudioFileFormat;
import static java.lang.Math.pow;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import static javafx.util.Duration.ZERO;
import static javafx.util.Duration.millis;
import static sp.it.pl.audio.playback.PLAYBACK.post_activating;
import static sp.it.pl.audio.playback.PLAYBACK.post_activating_1st;
import static sp.it.pl.audio.playback.PLAYBACK.state;
import static sp.it.pl.audio.playback.PLAYBACK.suspension_flag;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.dev.Util.log;
import static sp.it.pl.util.file.AudioFileFormat.Use;
import static sp.it.pl.util.file.AudioFileFormat.m4a;
import static sp.it.pl.util.file.AudioFileFormat.mp3;
import static sp.it.pl.util.file.AudioFileFormat.mp4;
import static sp.it.pl.util.file.AudioFileFormat.wav;
import static sp.it.pl.util.reactive.Util.installSingletonListener;

/**
 * Audio player which abstracts away from the implementation. It 'uses' actual players and provides
 * some of the higher level features they may lack.
 */
public class GeneralPlayer {

	private final JavaSoundPlayer fp = new JavaSoundPlayer();
	private final JavaFxPlayer dp = new JavaFxPlayer();
	//    private final GPlayer gp = new GPlayer();
	private Play p;

	private Play getPlayer(Item i) {
//        return gp;
		AudioFileFormat f = i.getFormat();
		if (!f.isSupported(Use.PLAYBACK)) return null;
		if (f==mp3 || f==wav || f==mp4 || f==m4a) return dp;
		else return fp;
	}

	Item i;
	public final RealTimeProperty realTime = new RealTimeProperty(state.duration, state.currentTime);

	@SuppressWarnings("deprecation")
	synchronized public void play(PlaylistItem item) {
		// Do not recreate player if same song plays again
		// 1) improves performance
		// 2) avoids firing some playback events
		if (p!=null && item.same(i)) {
			seek(ZERO);
			return;
		}

		i = item;
		if (p!=null) p.dispose();
		p = getPlayer(item);

		try {
			if (p==null) throw new NoPlayerException();
			p.createPlayback(item, state,
					() -> {
						realTime.real_seek = state.realTime.get();
						realTime.curr_sek = ZERO;
						p.play();

						realTime.synchroRealTime_onPlayed();
						// throw item change event
						Player.playingItem.itemChanged(item);
						suspension_flag = false;
						// fire other events (may rely on the above)
						PLAYBACK.onPlaybackStart.run();
						if (post_activating_1st || !post_activating)
							// bug fix, not updated playlist items can get here, but should not!
							if (item.getTimeMs()>0)
								PLAYBACK.onPlaybackAt.forEach(t -> t.restart(item.getTime()));
						post_activating = false;
						post_activating_1st = false;
					},
					() -> runFX(() -> {
						log(GeneralPlayer.class).info("Player {} can not play item {}", p, item);
						item.playbackError = true;
						PlaylistManager.use(Playlist::playNextItem); // handle within playlist
					}));
		} catch (NoPlayerException e) {
			log(GeneralPlayer.class).info("Player {} can not play item {}", p, item);
			item.playbackError = true;
			PlaylistManager.use(Playlist::playNextItem); // handle within playlist
		}
	}

	public void resume() {
		if (p==null) return;
		p.resume();
		PLAYBACK.onPlaybackAt.forEach(PlayTimeHandler::unpause);
	}

	public void pause() {
		if (p==null) return;
		p.pause();
		PLAYBACK.onPlaybackAt.forEach(PlayTimeHandler::pause);
	}

	public void pause_resume() {
		if (p==null) return;
		if (state.status.get()==PLAYING) pause();
		else resume();
	}

	public void stop() {
		if (p==null) return;
		p.stop();

		runFX(() -> {
			Player.playingItem.itemChanged(Metadata.EMPTY);
			realTime.synchroRealTime_onStopped();
			PLAYBACK.onPlaybackAt.forEach(PlayTimeHandler::stop);
			PlaylistManager.playlists.forEach(p -> p.updatePlayingItem(-1));
			PlaylistManager.active = null;
		});
	}

	public void seek(Duration duration) {
		if (p==null) return;

		Status s = state.status.get();

		if (s==STOPPED) {
			pause();
		}

		final double currentVolume = state.volume.get();
		if (seekDone) lastValidVolume = currentVolume;
		else { if (volumeAnim!=null) volumeAnim.pause(); }
		seekDone = false;
		new Anim(millis(150), x -> state.volume.set(currentVolume*pow(1 - x, 2)))
				.then(() -> {
					doSeek(duration);
					volumeAnim = new Anim(millis(150), x -> state.volume.set(lastValidVolume*pow(x, 2)))
							.then(() -> seekDone = true);
					volumeAnim.playOpen();
				})
				.playOpen();
	}

	private void doSeek(Duration duration) {
		realTime.synchroRealTime_onPreSeeked();
		installSingletonListener(state.currentTime, Objects::nonNull, v -> PLAYBACK.onSeekDone.run());
		p.seek(duration);
		realTime.synchroRealTime_onPostSeeked(duration);
	}

	private boolean seekDone = true;
	private double lastValidVolume = -1;
	private Anim volumeAnim;

	/**
	 * Closes player. Releases resources. Prevents double playback.
	 */
	public void dispose() {
		if (p==null) return;
		p.dispose();
		p = null;
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
		void dispose();
	}

	private static class NoPlayerException extends Exception {}
}