/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback.player;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import org.reactfx.Subscription;

import AudioPlayer.Item;
import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import AudioPlayer.playback.PlaybackState;
import util.dev.TODO;

import static javafx.scene.media.MediaPlayer.Status.*;
import static util.async.Async.runFX;
import static util.dev.TODO.Purpose.BUG;
import static util.reactive.Util.maintain;

/**
 *
 * @author Plutonium_
 */
public class JavaFxPlayer implements Play {

    private MediaPlayer player;
    private Subscription d1,d2,d3,d4,d5,d6,d7;

    @Override
    public void play() {
        if(player!=null) player.play();
    }

    @Override
    public void pause() {
        if(player!=null) player.pause();
    }

    @Override
    public void resume() {
        if(player!=null) player.play();
    }

    @Override
    public void seek(Duration duration) {
        if(player!=null) player.seek(duration);
    }

    @Override
    public void stop() {
        if(player!=null) player.stop();
    }

    @Override
    @TODO(purpose = BUG, note = "Media creation throws MediaException (FileNotFoundException) for "
            + "valid files containing some special characters, like á (copypasting in Netbeans "
            + "produces: 'á' (probably Unicode))")
    public void createPlayback(Item item, PlaybackState state, Runnable onOk, Runnable onFail) {

        Player.IO_THREAD.execute(() -> {
            Media media;
            try{
                // bug here, which also
                // blocks thread for like half second!, so i execute this not on fx
                media = new Media(item.getURI().toString());
            }catch(MediaException e) {
                onFail.run();
                return;
            }
            runFX(() -> {
                player = new MediaPlayer(media);

                player.setStartTime(Duration.ZERO);
                player.setAudioSpectrumInterval(0.01);
                player.setAudioSpectrumNumBands(128);
                // player.setAudioSpectrumThreshold(i) // ? what val is ok?
                player.setAudioSpectrumListener(PLAYBACK.spectrumListenerDistributor);

                // bind (not read only) values
                d1 = maintain(state.volume,player.volumeProperty());
                d2 = maintain(state.mute,player.muteProperty());
                d3 = maintain(state.balance,player.balanceProperty());
                d4 = maintain(state.rate,player.rateProperty());
                player.setOnEndOfMedia(PLAYBACK.playbackEndDistributor);

                // handle binding of state to player
                // handle seeking when player in invalid statuses (seek when status becomes valid)
                player.statusProperty().addListener(new ChangeListener<MediaPlayer.Status>() {
                    @Override
                    public void changed(ObservableValue<? extends MediaPlayer.Status> o, MediaPlayer.Status oldV, MediaPlayer.Status newV) {
                        if (newV == PLAYING || newV == PAUSED || newV == STOPPED) {
                            // bind (read only) values: new player -> global (manual initialization)
                            d5 = maintain(player.currentTimeProperty(),state.currentTime);
                            // we completely ignore javafx readings here, instead rely on jaudiotagger
                            // d6 = maintain(player.cycleDurationProperty(),state.duration);
                            // state.duration.set(player.cycleDurationProperty().get());
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
                    if (s == PLAYING) play();
                    else if (s == PAUSED) pause();
                }

                onOk.run();
            });
        });
    }

    @Override
    public void dispose() {
        if(player==null) return;

        // cut player sideffects, do so before disposing
        if(d1!=null) d1.unsubscribe();
        if(d2!=null) d2.unsubscribe();
        if(d3!=null) d3.unsubscribe();
        if(d4!=null) d4.unsubscribe();
        if(d5!=null) d5.unsubscribe();
        if(d6!=null) d6.unsubscribe();
        if(d7!=null) d7.unsubscribe();
        player.setAudioSpectrumListener(null); // just in case
        player.setOnEndOfMedia(null); // just in case

        // stop() not necessary, calling dispose stops playback and frees resources
        player.dispose();
        player = null;
    }
}