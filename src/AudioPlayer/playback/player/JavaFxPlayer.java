/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback.player;

import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.Item;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.PAUSED;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import javafx.util.Duration;
import org.reactfx.Subscription;
import unused.Log;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class JavaFxPlayer implements Play {
    
    public MediaPlayer player;
    Subscription d1,d2,d3,d4,d5,d6,d7;
    
    @Override
    public void play() {
        player.play();
    }

    @Override
    public void pause() {
        player.pause();
    }

    @Override
    public void resume() {
        player.play();
    }

    @Override
    public void seek(Duration duration) {
        player.seek(duration);
    }

    @Override
    public void stop() {
        player.stop();
    }
    
    @Override
    public void createPlayback(Item item, PlaybackState state) {
        Media media;
        try {
            media = new Media(item.getURI().toString());
        } catch (MediaException e) {
            Log.err(e.getLocalizedMessage());
            // this should be actually handled
            return;
        }

        player = new MediaPlayer(media);

        player.setAudioSpectrumInterval(0.1);
        player.setAudioSpectrumNumBands(64);
        // player.setAudioSpectrumThreshold(i) // ? what val is ok?

        // bind (not read only) values
        d1 = maintain(state.volume,player.volumeProperty());
        d2 = maintain(state.mute,player.muteProperty());
        d3 = maintain(state.balance,player.balanceProperty());
        d4 = maintain(state.rate,player.rateProperty());

        // register listener/event distributors
        player.setAudioSpectrumListener(PLAYBACK.spectrumListenerDistributor);
        player.setOnEndOfMedia(PLAYBACK.playbackEndDistributor);

        // handle binding of state to player
        // handle seeking when player in invalid statuses (seek when status becomes valid)
        player.statusProperty().addListener(new ChangeListener<MediaPlayer.Status>() {
            @Override
            public void changed(ObservableValue<? extends MediaPlayer.Status> o, MediaPlayer.Status oldV, MediaPlayer.Status newV) {
                if (newV == PLAYING || newV == PAUSED || newV == STOPPED) {
                    // bind (read only) values: new player -> global (manual initialization)
                    d5 = maintain(player.currentTimeProperty(),state.currentTime);
                    d6 = maintain(player.cycleDurationProperty(),state.duration);
                    d7 = maintain(player.statusProperty(),state.status);
                    // make one time only
                    player.statusProperty().removeListener(this);
                }
            }
        });
        player.statusProperty().addListener((o,ov,nv) -> {
            if (nv == PLAYING || nv == PAUSED ) {
                if (PLAYBACK.startTime!=null) {
                    seek(PLAYBACK.startTime);
                    PLAYBACK.startTime = null;
                }
            }
        });

        Status s = state.status.get();
        if(PLAYBACK.startTime!=null) {
            if (s == PLAYING) player.play();
            else if (s == PAUSED) player.pause();
        }
    }

    @Override
    public void dispose() {
        // render subsequent calls void
        if(player==null) return;
        // cut player sideffects, do so before disposing
        if(d1!=null) d1.unsubscribe();
        if(d2!=null) d2.unsubscribe();
        if(d3!=null) d3.unsubscribe();
        if(d4!=null) d4.unsubscribe();
        if(d5!=null) d5.unsubscribe();
        if(d6!=null) d6.unsubscribe();
        if(d7!=null) d7.unsubscribe();
        // stop() not necessary, wouldnt even work since these calls are 
        // asynchronous, calling dispose stops playback and frees resources
        player.dispose();
        player = null;
    }
}