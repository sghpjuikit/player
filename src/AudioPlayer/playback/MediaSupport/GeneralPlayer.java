/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback.MediaSupport;


import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import static AudioPlayer.playback.PLAYBACK.state;
import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playback.RealTimeProperty;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import static javafx.scene.media.MediaPlayer.Status.*;
import javafx.util.Duration;
import util.File.AudioFileFormat.Use;
import util.dev.Log;

/**
 *
 * @author yoss
 */
public class GeneralPlayer {// implements MediaWrap{

    public MediaPlayer playback;
          
    public boolean needs_bind = true; // must be initialized to true
    public boolean needs_seek = false;
    public Duration seekTo;
    public final RealTimeProperty realTime = new RealTimeProperty(PLAYBACK.state.duration, PLAYBACK.state.currentTime);
    
    
    public void play(PlaylistItem item) {
        // properly end previous
        dispose();
        // handle corrupted
        if (item.isCorrupt(Use.PLAYBACK)) { 
            PlaylistManager.playItem(item); // will handle 'corruptness' within playlist
            return;
        }
        // play
        createPlayback(item.getURI().toString(), state);
        playback.play();
        
        realTime.synchroRealTime_onPlayed();
        // first throw item change event
        Player.playingtem.itemChanged(item);
        PlaylistManager.setPlayingItem(item);
        // then start other events (that may rely on the above)
        PLAYBACK.playbackStartDistributor.run();
        PLAYBACK.onTimeHandlers.forEach(t -> t.restart(item.getTime()));
    }
    
    public void resume() {
        if (playback.getMedia() == null) return;
        playback.play();
        PLAYBACK.onTimeHandlers.forEach(t -> t.unpause());
    }
    
    public void pause() {
        if (playback.getMedia() == null) return;
        playback.pause();
        PLAYBACK.onTimeHandlers.forEach(t -> t.pause());
    }
    
    public void pause_resume() {
        if (playback == null || playback.getMedia() == null) return;

        if (playback.getStatus() == PLAYING) pause();
        else resume();
    }
    
    public void stop() {
        if (playback == null) return;
        
        playback.stop();
        realTime.synchroRealTime_onStopped();
        PLAYBACK.onTimeHandlers.forEach(t -> t.stop());
    }
    
    public void seek(Duration duration) {
        if (playback == null) return;
        
        if (playback.getStatus() == STOPPED) {
            pause();
        }
        
        if (playback.getStatus() == PLAYING || playback.getStatus() == PAUSED) {
            realTime.synchroRealTime_onPreSeeked();
            playback.seek(duration);
            realTime.synchroRealTime_onPostSeeked(duration);
            needs_seek = false;
        } else {
            needs_seek = true;
            seekTo = duration;
        }
    }
   
    
    
    public void createPlayback(String resource, PlaybackState state){
        Media media;
        try {
            media = new Media(resource);
        } catch (MediaException e) {
            Log.err(e.getLocalizedMessage());
            stop();
            return;
        }
        
        playback = new MediaPlayer(media);
        
        // debug
        // these two have no effect, unless something is wrong
        // if seeking out of sync with current value (WHICH IT IS!!)
        // setStopTime might stop the song prematurely
        // player.setStartTime(ZERO);
        // player.setStopTime(player.getTotalDuration());
        
        
        playback.setAudioSpectrumInterval(0.1);
        playback.setAudioSpectrumNumBands(64);
//        player.setAudioSpectrumThreshold(i) // ? what val is ok?

        // bind (not read only) values: global -> new player (automatic initialization)
        playback.volumeProperty().bind(state.volume.volumeProperty());
        playback.balanceProperty().bind(state.balance.balanceProperty());
        playback.muteProperty().bind(state.mute);
        playback.rateProperty().bind(state.rate);
        realTime.real_seek = state.realTime.get();
        realTime.curr_sek = Duration.ZERO;
        // register listener/event distributors
        playback.setAudioSpectrumListener(PLAYBACK.spectrumListenerDistributor);
        playback.setOnEndOfMedia(PLAYBACK.playbackEndDistributor);
        
        // handle binding of state to player
        // handle seeking when player in invalid statuses (seek when status becomes valid)
        playback.statusProperty().addListener(new ChangeListener<MediaPlayer.Status>() {
            @Override
            public void changed(ObservableValue<? extends MediaPlayer.Status> o, MediaPlayer.Status oldV, MediaPlayer.Status newV) {
                if (newV == PLAYING || newV == PAUSED || newV == STOPPED) {
                    // bind (read only) values: new player -> global (manual initialization)
                    state.currentTime.bind(playback.currentTimeProperty());
                    state.duration.bind(playback.cycleDurationProperty());
                    state.status.bind(playback.statusProperty());
                    // make one time only
                    playback.statusProperty().removeListener(this);
                }
            }
        });
        playback.statusProperty().addListener(new ChangeListener<MediaPlayer.Status>() {
            @Override
            public void changed(ObservableValue<? extends MediaPlayer.Status> o, MediaPlayer.Status oldV, MediaPlayer.Status newV) {
                if (newV == MediaPlayer.Status.PLAYING || newV == PAUSED ) {
                    if (needs_seek) {
                        seek(seekTo);
                    }
                    // make one time only
                    playback.statusProperty().removeListener(this);
                }
            }
        });
        
        // initialize (read only) values
            //status && duration(auto)(item_length) && current time (auto)(0)
        if (state.status.get() == MediaPlayer.Status.PLAYING) {
            playback.play();
        } else
        if (state.status.get() == MediaPlayer.Status.PAUSED) {
            playback.pause();
        } else {
            playback.stop();
        }
    }
    
    
    
    /**
     * Closes player.
     * Prevents 'double player'.
     */
    public void dispose() {
        if (playback == null) return;
        playback.volumeProperty().unbind();
        playback.balanceProperty().unbind();
        playback.muteProperty().unbind();
        playback.rateProperty().unbind();
        PLAYBACK.state.currentTime.unbind();
        PLAYBACK.state.duration.unbind();
        PLAYBACK.state.status.unbind();
        playback.stop();
        playback.dispose();
        playback = null;
        needs_bind = true;
    }
    
}
