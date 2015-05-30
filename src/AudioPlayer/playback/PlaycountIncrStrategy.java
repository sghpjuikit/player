/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.playback;

/**
 * Strategy for incrementing playcount.
 * 
 * @author uranium
 */
public enum PlaycountIncrStrategy {
    /** Increment when song starts playing. */
    ON_START,
    /** Increment when song stops playing naturally. */
    ON_END,
    /** Increment when song is playing for specified time. */
    ON_TIME,
    /** Increment when song is playing for portion of its time. */
    ON_PERCENT,
    /** Increment when song is playing for specified time or portion of its time. */
    ON_TIME_OR_PERCENT,
    /** Increment when song is playing for specified time and portion of its time. */
    ON_TIME_AND_PERCENT,
    /** Never increment. */
    NEVER;
}
