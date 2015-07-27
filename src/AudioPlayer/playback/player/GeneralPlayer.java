/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback.player;


import AudioPlayer.Player;
import AudioPlayer.playback.PLAYBACK;
import static AudioPlayer.playback.PLAYBACK.state;
import AudioPlayer.playback.RealTimeProperty;
import AudioPlayer.playlist.Item;
import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.playlist.PlaylistManager;
import javafx.scene.media.MediaPlayer.Status;
import static javafx.scene.media.MediaPlayer.Status.*;
import javafx.util.Duration;
import util.File.AudioFileFormat;
import util.File.AudioFileFormat.Use;
import static util.File.AudioFileFormat.m4a;
import static util.File.AudioFileFormat.mp3;
import static util.File.AudioFileFormat.mp4;
import static util.File.AudioFileFormat.wav;

/**
 *
 * @author yoss
 */
public class GeneralPlayer {

    private final JavaSoundPlayer fp = new JavaSoundPlayer();
    private final JavaFxPlayer dp = new JavaFxPlayer();
    private Play p;

    private Play getPlayer(Item i) {
        AudioFileFormat f = i.getFormat();
        if(!f.isSupported(Use.PLAYBACK)) return null;
        if(f==mp3 || f==wav || f==mp4 || f==m4a) return dp;
        else return fp;
    }
    
    Item i;
    public final RealTimeProperty realTime = new RealTimeProperty(PLAYBACK.state.duration, PLAYBACK.state.currentTime);
    
    
    public void play(PlaylistItem item) {
        // prevent recreating the whole thing if same song plays again
        if(p!=null && item.same(i)) {
            seek(Duration.ZERO);
            return;
        }
        i = item;
        
        // dispose of previous player
        if(p!=null) p.dispose();
        // get new player
        p = getPlayer(item);
        
        // handle unsupported
        if (p==null) { 
            PlaylistManager.playItem(item); // handle within playlist
            return;
        }
        
        // play
        p.createPlayback(item, state);
        realTime.real_seek = state.realTime.get();
        realTime.curr_sek = Duration.ZERO;
        p.play();
        
        realTime.synchroRealTime_onPlayed();
        // throw item change event
        Player.playingtem.itemChanged(item);
        PlaylistManager.setPlayingItem(item);
        // fire other events (may rely on the above)
        PLAYBACK.playbackStartDistributor.run();
        PLAYBACK.onTimeHandlers.forEach(t -> t.restart(item.getTime()));
    }
    
    public void resume() {
        if(p==null) return;
        
        p.resume();
        
        PLAYBACK.onTimeHandlers.forEach(t -> t.unpause());
    }
    
    public void pause() {
        if(p==null) return;
        
        p.pause();
        
        PLAYBACK.onTimeHandlers.forEach(t -> t.pause());
    }
    
    public void pause_resume() {
        if(p==null) return;
        
        if(state.status.get()==PLAYING) pause();
        else resume();
    }
    
    public void stop() {
        if(p==null) return;
        p.stop();
        
        realTime.synchroRealTime_onStopped();
        PLAYBACK.onTimeHandlers.forEach(t -> t.stop());
    }
    
    public void seek(Duration duration) {
        if(p==null) return;
        
        Status s = state.status.get();
        
        if (s == STOPPED) {
            pause();
        }
        
        realTime.synchroRealTime_onPreSeeked();
        p.seek(duration);
        realTime.synchroRealTime_onPostSeeked(duration);
    }
   
    /**
     * Closes player.
     * Prevents 'double player'.
     */
    public void dispose() {
        if(p==null) return;
        p.dispose();
        p=null;
    }
}