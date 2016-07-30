package audio.playback.player;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import audio.Item;
import audio.Player;
import audio.playback.PLAYBACK;
import audio.playback.PlaybackState;
import audio.playback.player.xtrememp.audio.AudioPlayer;
import audio.playback.player.xtrememp.audio.PlaybackEvent;
import audio.playback.player.xtrememp.audio.PlaybackListener;
import audio.playback.player.xtrememp.audio.PlayerException;

import static javafx.scene.media.MediaPlayer.Status.*;
import static javafx.util.Duration.millis;
import static util.async.Async.runFX;
import static util.async.Async.runLater;

/**
 *
 * @author Martin Polakovic
 */
public class JavaSoundPlayer implements GeneralPlayer.Play {

    private final AudioPlayer p = new AudioPlayer();
    private double seeked = 0;

    public JavaSoundPlayer() {
        p.addPlaybackListener(new PlaybackListener() {

            @Override public void playbackBuffering(PlaybackEvent pe) {}

            @Override public void playbackOpened(PlaybackEvent pe) {
                Duration d = millis(p.getDuration()/1000);
                runLater(()->{
                    PLAYBACK.state.duration.set(d);
                });
            }

            @Override public void playbackEndOfMedia(PlaybackEvent pe) {
                runLater(()->{
                    PLAYBACK.onPlaybackEnd.run();
                });
            }
            long l = 0;
            @Override public void playbackProgress(PlaybackEvent pe) {
                l++;
                if (l%2==0)  {// lets throttle the events a bit
                    Duration d = millis(seeked+pe.getPosition()/1000);
                    runLater(()->{
                        PLAYBACK.state.currentTime.set(d);
                    });
                }
            }

            // state changes, impl detail is whether we update playback status
            // here (when takes effect) or below (when it is invoked)

            @Override public void playbackPlaying(PlaybackEvent pe) {
                if (PLAYBACK.startTime!=null) {
                    seek(PLAYBACK.startTime);
                    PLAYBACK.startTime = null;
                }
            }

            @Override public void playbackPaused(PlaybackEvent pe) {
                    if (PLAYBACK.startTime!=null) {
                        seek(PLAYBACK.startTime);
                        PLAYBACK.startTime = null;
                    }
            }

            @Override public void playbackStopped(PlaybackEvent pe) {}
        });
    }

    @Override
    public void createPlayback(Item item, PlaybackState state, Runnable onOk, Runnable onFail) {
        Player.IO_THREAD.execute( () -> {
            try {
                p.open(item.getFile());
                p.setVolume(state.volume.get());
                p.setMute(state.mute.get());
                p.setBalance(state.balance.get());
                runFX(() -> {
                    try {
                        state.volume.addListener((o,ov,nv) -> p.setVolume(nv.doubleValue()));
                        state.mute.addListener((o,ov,nv) -> p.setMute(nv));
                        state.balance.addListener((o,ov,nv) -> p.setBalance(nv.doubleValue()));


                        Status s = state.status.get();
                        if (PLAYBACK.startTime!=null) {
                            if (s == PLAYING) p.play();
                            else if (s == PAUSED) p.pause();
                        }

                        onOk.run();
                    } catch (PlayerException ex) {
                        Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
            } catch (PlayerException ex) {
                Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
                onFail.run();
            }
        });
    }

    @Override
    public void dispose() {
        p.stop();
    }

    @Override
    public void play() {
        seeked = 0;
        Player.IO_THREAD.execute( () -> {
            try {
                p.play();
            } catch (PlayerException ex) {
                Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        PLAYBACK.state.status.set(PLAYING);
    }

    @Override
    public void pause() {
        p.pause();
        PLAYBACK.state.status.set(PAUSED);
    }

    @Override
    public void resume() {
        Player.IO_THREAD.execute( () -> {
            try {
                p.play();
            } catch (PlayerException ex) {
                ex.printStackTrace();
                Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        PLAYBACK.state.status.set(PLAYING);
    }

    @Override
    public void seek(Duration duration) {
        if (duration==null) return;
        // this player's seeking requires us to know the new
        // starting position to calculate new current position (see the listener
        // in constructor)
        seeked = duration.toMillis();
        p.seek(duration);
    }

    @Override
    public void stop() {
        p.stop();
        PLAYBACK.state.status.set(STOPPED);
    }
}