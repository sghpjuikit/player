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
import org.reactfx.Subscription;
import sp.it.pl.audio.Item;
import sp.it.pl.audio.Player;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import static sp.it.pl.audio.playback.VolumeProperty.linToLog;
import static sp.it.pl.util.async.AsyncKt.runFX;
import static sp.it.pl.util.dev.DebugKt.logger;
import static sp.it.pl.util.reactive.Util.maintain;

public class JavaFxPlayer implements GeneralPlayer.Play {

	private MediaPlayer player;
	private Subscription d1, d2, d3, d4, d5, d6;

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
	public void createPlayback(Item item, PlaybackState state, Function0<Unit> onOK, Function0<Unit> onFail) {
		Player.IO_THREAD.execute(() -> {
			Media media;
			try {
				// TODO: Media creation throws MediaException (FileNotFoundException) for files containing some special chars (unicode?)
				// TODO: Use getUri.toAsciiString()?
				// If that happens, it can block thread for like half second!, so i execute this not on fx
				media = new Media(item.getUri().toString());
			} catch (MediaException e) {
				logger(JavaFxPlayer.class).error("Media creation error for {}", item.getUri());
				onFail.invoke();
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
				d1 = maintain(state.volume, v -> linToLog(v.doubleValue()), player.volumeProperty());
				d2 = maintain(state.mute, player.muteProperty());
				d3 = maintain(state.balance, player.balanceProperty());
				d4 = maintain(state.rate, player.rateProperty());
				player.setOnEndOfMedia(Player.onPlaybackEnd);

				// handle binding of state to player
				// handle seeking when player in invalid statuses (seek when status becomes valid)
				player.statusProperty().addListener(new ChangeListener<>() {
					@Override
					public void changed(ObservableValue<? extends Status> o, Status ov, Status nv) {
						if (nv==PLAYING || nv==PAUSED || nv==STOPPED) {
							// bind (read only) values: new player -> global (manual initialization)
							d5 = maintain(player.currentTimeProperty(), state.currentTime);
							d6 = maintain(player.statusProperty(), s -> {
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
		if (d3!=null) d3.unsubscribe();
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
	public void dispose() {}
}