/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

/**
 * Playback Loop mode type variable. Extends Enum. Values are: PLAYLIST, SONG, OFF .
 */
public enum LoopMode {
    PLAYLIST,
    SONG,
    OFF;
    
    /**
     * Returns next enum value from list of all values. It cycles the list if
     * last value is specified. Use to conveniently traverse all values of this
     * enum.
     * @return 
     */
    public LoopMode next() {
        LoopMode vals[] = LoopMode.values();
        int index = (this.ordinal()+1) % vals.length;
        return vals[index];
    }
    /**
     * Returns next enum value from list of all values. It cycles the list if
     * last value is specified. Use to conveniently traverse all values of this
     * enum.
     * @param val
     * @return 
     */
    public static LoopMode next(LoopMode val) {
        LoopMode vals[] = LoopMode.values();
        int index = (val.ordinal()+1) % vals.length;
       
        return vals[index];
    }
}