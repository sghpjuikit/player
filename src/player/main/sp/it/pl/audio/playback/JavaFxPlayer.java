package sp.it.pl.audio.playback;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import sp.it.pl.audio.Player;
import sp.it.pl.audio.Song;
import sp.it.util.reactive.Subscription;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import static sp.it.pl.audio.playback.VolumeProperty.linToLog;
import static sp.it.util.async.AsyncKt.runFX;
import static sp.it.util.dev.DebugKt.logger;
import static sp.it.util.reactive.UtilKt.syncC;
import static sp.it.util.reactive.UtilKt.syncTo;

public class JavaFxPlayer implements GeneralPlayer.Play {

	private MediaPlayer player;
	private Subscription d1, d2, d4, d5, d6;

	@Override
	public void play() {
		if (player!=null) player.play();
	}

	@Override
	public void pause() {
		if (player!=null) player.pause();
	}

	@Override
	public void resume() {
		if (player!=null) player.play();
	}

	@Override
	public void seek(Duration duration) {
		if (player!=null) player.seek(duration);
	}

	@Override
	public void stop() {
		if (player!=null) player.stop();
	}

	@Override
	public void createPlayback(Song song, PlaybackState state, Function0<Unit> onOK, Function1<? super Boolean, Unit> onFail) {
		Player.IO_THREAD.execute(() -> {
			Media media;
			try {
				// TODO: Media creation throws MediaException (FileNotFoundException) for files containing some special chars (unicode?)
				//       I think it is the same problem as the ne with vlcPlayer, needs file:///
				//       https://stackoverflow.com/questions/10062270/how-to-target-a-file-a-path-to-it-in-java-javafx
				// If that happens, it can block thread for like half second!, so i execute this not on fx
				media = new Media(song.getUri().toString());
			} catch (MediaException e) {
				logger(JavaFxPlayer.class).error("Media creation error for {}", song.getUri());
				runFX(() ->
					onFail.invoke(false)
				);
				return;
			}
			runFX(() -> {
				player = new MediaPlayer(media);

				player.setStartTime(Duration.ZERO);
				player.setAudioSpectrumInterval(0.01);
				player.setAudioSpectrumNumBands(128);
				// player.setAudioSpectrumThreshold(i) // ? what val is ok?
				player.setAudioSpectrumListener(Player.spectrumListenerDistributor);

				// bind (not read only) values
				d1 = syncC(state.volume, v -> player.setVolume(linToLog(v.doubleValue())));
				d2 = syncTo(state.mute, player.muteProperty());
				d4 = syncTo(state.rate, player.rateProperty());
				player.setOnEndOfMedia(Player.onPlaybackEnd);

				// handle binding of state to player
				// handle seeking when player in invalid statuses (seek when status becomes valid)
				player.statusProperty().addListener(new ChangeListener<>() {
					@Override
					public void changed(ObservableValue<? extends Status> o, Status ov, Status nv) {
						if (nv==PLAYING || nv==PAUSED || nv==STOPPED) {
							// bind (read only) values: new player -> global (manual initialization)
							d5 = syncTo(player.currentTimeProperty(), state.currentTime);
							d6 = syncC(player.statusProperty(), s -> {
								if (!Player.suspension_flag)
									state.status.setValue(s);
							});
							// make one time only
							player.statusProperty().removeListener(this);
						}
					}
				});
				player.statusProperty().addListener((o, ov, nv) -> {
					if (nv==PLAYING || nv==PAUSED) {
						if (Player.startTime!=null) {
							seek(Player.startTime);
							Player.startTime = null;
						}
					}
				});

				Status s = state.status.get();
				if (Player.startTime!=null) {
					if (s==PLAYING) play();
					else if (s==PAUSED) pause();
				}

				onOK.invoke();
			});
		});
	}

	@Override
	public void disposePlayback() {
		if (player==null) return;

		// cut player side effects, do so before disposing
		if (d1!=null) d1.unsubscribe();
		if (d2!=null) d2.unsubscribe();
		if (d4!=null) d4.unsubscribe();
		if (d5!=null) d5.unsubscribe();
		if (d6!=null) d6.unsubscribe();
		player.setAudioSpectrumListener(null); // just in case
		player.setOnEndOfMedia(null); // just in case

		// stop() not necessary, calling dispose stops playback and frees resources
		player.dispose();
		player = null;
	}

	@Override
	public void dispose() {
		disposePlayback();
	}

}