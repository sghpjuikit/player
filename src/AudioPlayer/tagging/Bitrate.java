/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

/**
 * Simple wrapper class for bitrate int, implementing toString method.
 * This class is immutable.
 * Use -1 value if bitrate not available. It avoids null values of this
 * object and there is also implemented toString method for this scenario.
 */
public class Bitrate {
    private int bitrate;
    
    /**
     * Constructor.
     * @param value 
     */
    public Bitrate(int value){
        bitrate = value;
    }
    
    /**
     * Constructor.
     * @param value
     * Use -1 if bitrate not available.
     */    
    public Bitrate(long value){
        Long l = new Long(value);
        bitrate = l.intValue();
    }
    
    /**
     * Convenience static method for creating Bitrate objects.
     * @param value
     * Use -1 if bitrate not available.
     * @return 
     */
    public static Bitrate create(int value) {
        return new Bitrate(value);
    }
    
    /**
     * Convenience static method for creating Bitrate objects.
     * @param value
     * Use -1 if bitrate not available.
     * @return 
     */    
    public static Bitrate create(long value) {
        return new Bitrate(value);
    }    
    
    /**
     * @return value 
     */
    public int getValue() {
        return bitrate;
    }
    
    /**
     * Appends ' kbps' string after value. If no value available it returns "not
     * available" string.
     * For example: 320kbps.
     * @return string representation of the object
     */
    @Override
    public String toString() {
        if ( bitrate == -1) {
            return "not available";
        }
        return bitrate + " kbps";
    }
}
