package AudioPlayer.playback.player;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playback.player.xtrememp.audio.AudioPlayer;
import AudioPlayer.playback.player.xtrememp.audio.PlaybackEvent;
import AudioPlayer.playback.player.xtrememp.audio.PlaybackListener;
import AudioPlayer.playback.player.xtrememp.audio.PlayerException;
import AudioPlayer.playlist.Item;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import javafx.util.Duration;
import static javafx.util.Duration.millis;
import org.reactfx.Subscription;
import static util.async.Async.runLater;

/**
 *
 * @author Plutonium_
 */
public class JavaSoundPlayer implements Play {
    
    public final AudioPlayer p = new AudioPlayer();
    private double seeked = 0;
    private Subscription d1,d2,d3;
    
    public JavaSoundPlayer() {
        p.addPlaybackListener(new PlaybackListener() {

            @Override public void playbackBuffering(PlaybackEvent pe) {
            }

            @Override public void playbackOpened(PlaybackEvent pe) {
                Duration d = millis(p.getDuration()/1000);
                runLater(()->{
                    PLAYBACK.state.duration.set(d);
                });
            }

            @Override public void playbackEndOfMedia(PlaybackEvent pe) {
                runLater(()->{
                    PLAYBACK.playbackEndDistributor.run();
                });
            }
            long l = 0;
            @Override public void playbackProgress(PlaybackEvent pe) {
                l++;
                if(l%2==0)  // lets throttle the events a bit
                    runLater(()->{
                        Duration d = millis(seeked+pe.getPosition()/1000);
                        PLAYBACK.state.currentTime.set(d);
                    });
            }
            
            // state changes, impl detail is whether we update playback status
            // here (after change happened) or below (when it is invoked)
            
            @Override public void playbackPlaying(PlaybackEvent pe) {
            }

            @Override public void playbackPaused(PlaybackEvent pe) {
            }

            @Override public void playbackStopped(PlaybackEvent pe) {
            }
        });
    }

    
    @Override
    public void createPlayback(Item item, PlaybackState state){
        try {
            p.open(item.getFile());
            p.setVolume(PLAYBACK.volumeProperty().get());
            p.setMute(PLAYBACK.state.mute.get());
            p.setBalance(PLAYBACK.state.balance.get());
            PLAYBACK.volumeProperty().addListener((o,ov,nv) -> p.setVolume(nv.doubleValue()));
            PLAYBACK.muteProperty().addListener((o,ov,nv) -> p.setMute(nv));
            PLAYBACK.balanceProperty().addListener((o,ov,nv) -> p.setBalance(nv.doubleValue()));
        } catch (PlayerException ex) {
            Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public void dispose() {
        p.stop();
    }

    @Override
    public void play() {
        seeked = 0;
        try {
            p.play();
        } catch (PlayerException ex) {
            Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PLAYBACK.state.status.set(PLAYING);
    }

    @Override
    public void pause() {
        p.pause();
        PLAYBACK.state.status.set(PAUSED);
    }

    @Override
    public void resume() {
        try {
            p.play();
        } catch (PlayerException ex) {
            Logger.getLogger(JavaSoundPlayer.class.getName()).log(Level.SEVERE, null, ex);
        }
        PLAYBACK.state.status.set(PLAYING);
    }

    @Override
    public void seek(Duration duration) {
        // seeking is implemented in such a way that we need to know the new
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