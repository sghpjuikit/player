/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import jdk.nashorn.internal.ir.annotations.Immutable;
import utilities.Dependency;

/**
 * Simple wrapper class for bitrate represented as int, implementing toString method.
 * This class is immutable.
 * Use -1 value if bitrate not available. It avoids null values of this
 * object and there is also implemented toString method for this scenario.
 * 
 * @author uranium
 */
@Immutable
public class Bitrate implements Comparable<Bitrate>{
    
    private final int bitrate;
    
    /**
     * Constructor.
     * @param value bit rate value in kb per second. Use -1 if not available.
     */
    public Bitrate(int value){
        if(value<-1) throw new IllegalArgumentException("Bitrate value must be -1 or larger");
        bitrate = value;
    }
    
    /**
     * Convenience static method for creating Bitrate objects.
     * @param value
     * @param value bit rate value in kb per second. Use -1 if not available.
     * @return 
     */
    public static Bitrate create(int value) {
        return new Bitrate(value);
    }
    
    /**
     * Convenience static method for creating Bitrate objects.
     * @param value
     * @param value bit rate value in kb per second. Use -1 if not available.
     * @return 
     */    
    public static Bitrate create(long value) {
        Long l = value;
        return new Bitrate(l.intValue());
    }    
    
    /**
     * @return bit rate value in kb per second.
     */
    public int getValue() {
        return bitrate;
    }
    
    /**
     * Appends ' kbps' string after value. If no value available it returns 
     * "N/A" string.
     * For example: "320 kbps" or "N/A"
     * @return string representation of the object
     */
    @Override
    @Dependency("Must be consistent with fromString()")
    public String toString() {
        return bitrate == -1 ? "N/A" : bitrate + " kbps";
    }
    
    @Override
    public int compareTo(Bitrate o) {
        return Long.compare(bitrate, o.bitrate);
    }
    
    @Dependency("Enables convertability to String using Parser by reflection")
    public Bitrate fromString(String s) {
        return new Bitrate(Integer.parseInt(s));
    }
}
