/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package audio.playback.player;

import javafx.util.Duration;

import audio.Item;
import audio.playback.PlaybackState;

/**
 *
 * @author Martin Polakovic
 */
public interface Play {

    void play();
    void pause();
    void resume();
    void seek(Duration duration);
    void stop();
    void createPlayback(Item item, PlaybackState state, Runnable onOK, Runnable onFail);
    /**
     * Stops playback if any and disposes of the player resources.
     */
    void dispose();
}
