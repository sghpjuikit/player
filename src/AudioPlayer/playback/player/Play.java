/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback.player;

import AudioPlayer.playback.PlaybackState;
import AudioPlayer.playlist.Item;
import javafx.util.Duration;

/**
 *
 * @author Plutonium_
 */
public interface Play {
    
    void play();
    void pause();
    void resume();
    void seek(Duration duration);
    void stop();
    
    void createPlayback(Item item, PlaybackState state);
    /**
     * Stops playback if any and disposes of the player resources.
     */
    void dispose();
}
