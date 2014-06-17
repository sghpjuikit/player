/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.tagging;

/**
 *
 * @author uranium
 */
public final class Playcount {
    
    /**
     * Strategy for incrementing playcount.
     */
    public static enum IncrStrategy {
        ON_START,
        ON_END,
        ON_TIME,
        ON_PERCENT,
        NEVER;
    }
}
