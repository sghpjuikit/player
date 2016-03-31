/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package audio.playback.player;


import javafx.scene.media.MediaPlayer.Status;
import javafx.util.Duration;

import audio.Item;
import audio.Player;
import audio.playback.PLAYBACK;
import audio.playback.RealTimeProperty;
import audio.playlist.PlaylistItem;
import audio.playlist.PlaylistManager;
import audio.tagging.Metadata;
import util.file.AudioFileFormat;
import util.file.AudioFileFormat.Use;
import util.async.Async;

import static audio.playback.PLAYBACK.*;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import static javafx.scene.media.MediaPlayer.Status.STOPPED;
import static util.file.AudioFileFormat.*;
import static util.async.Async.runFX;
import static util.dev.Util.log;

/**
 * Audio player which abstracts away from the implementation. It 'uses' actual players and provides
 * some of the higher level features they may lack.
 *
 * @author yoss
 */
public class GeneralPlayer {

    private final JavaSoundPlayer fp = new JavaSoundPlayer();
    private final JavaFxPlayer dp = new JavaFxPlayer();
//    private final GPlayer gp = new GPlayer();
    private Play p;

    private Play getPlayer(Item i) {
//        return gp;
        AudioFileFormat f = i.getFormat();
        if(!f.isSupported(Use.PLAYBACK)) return null;
        if(f==mp3 || f==wav || f==mp4 || f==m4a) return dp;
        else return fp;
    }

    Item i;
    public final RealTimeProperty realTime = new RealTimeProperty(state.duration, state.currentTime);


    @SuppressWarnings("deprecation")
    synchronized public void play(PlaylistItem item) {
        // Dont recreate player if same song plays again
        // 1) improves performance
        // 2) avoids firing some playback events
        if(p!=null && item.same(i)) {
            seek(Duration.ZERO);
            return;
        }

        i = item;
        if(p!=null) p.dispose();
        p = getPlayer(item);

        // play
        try {

            if(p==null) throw new NoPlayerException();
            p.createPlayback(item, state,
                () -> {
                    realTime.real_seek = state.realTime.get();
                    realTime.curr_sek = Duration.ZERO;
                    p.play();

                    realTime.synchroRealTime_onPlayed();
                    // throw item change event
                    Player.playingtem.itemChanged(item);
                    suspension_flag = false;
                    // fire other events (may rely on the above)
                    PLAYBACK.playbackStartDistributor.run();
                    if(post_activating_1st || !post_activating)
                        // bugfix, unupdated playlist items can get here, but shouldnt!
                        if(item.getTimeMs()>0)
                            PLAYBACK.onTimeHandlers.forEach(t -> t.restart(item.getTime()));
                    post_activating = false;
                    post_activating_1st = false;
                },
                () -> {
                    runFX(() -> {
                        log(GeneralPlayer.class).info("Player {} can not play item {}", p,item);
                        item.playbackerror = true;
                        PlaylistManager.use(p -> p.playNextItem()); // handle within playlist
                    });
                }
            );
        } catch(NoPlayerException e) {
            log(GeneralPlayer.class).info("Player {} can not play item {}", p,item);
            item.playbackerror = true;
            PlaylistManager.use(p -> p.playNextItem()); // handle within playlist
        }
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

        Async.runFX(() -> {
            Player.playingtem.itemChanged(Metadata.EMPTY);
            realTime.synchroRealTime_onStopped();
            PLAYBACK.onTimeHandlers.forEach(t -> t.stop());
            PlaylistManager.playlists.forEach(p ->p.playingI.set(-1));
            PlaylistManager.active = null;
        });
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

    private static class NoPlayerException extends Exception {}
}