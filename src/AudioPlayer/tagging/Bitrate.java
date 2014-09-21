/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import jdk.nashorn.internal.ir.annotations.Immutable;
import util.Dependency;

/**
 * Simple class for media bit rate represented as int, implementing toString method.
 * 
 * @author uranium
 */
@Immutable
public class Bitrate implements Comparable<Bitrate>{
    
    private final int bitrate;
    
    /**
     * @param value bit rate value in kb per second. Use -1 if not available.
     */
    public Bitrate(int value){
        if(value<-1) throw new IllegalArgumentException("Bitrate value must be -1 or larger");
        bitrate = value;
    }
    
    /** @param value bit rate value in kb per second. Use -1 if not available. */
    public static Bitrate create(int value) {
        return new Bitrate(value);
    }
    
    /** @param value bit rate value in kb per second. Use -1 if not available. */    
    public static Bitrate create(long value) {
        Long l = value;
        return new Bitrate(l.intValue());
    }    
    
    /** @return bit rate value in kb per second. */
    public int getValue() {
        return bitrate;
    }
    
    /**
     * Appends ' kbps' string after value. If no value available it returns "".
     * For example: "320 kbps" or "N/A"
     * @return string representation of the object
     */
    @Override
    @Dependency("Designed to be used in tables and gui.")
    @Dependency("Must be consistent with fromString()")
    public String toString() {
        return bitrate == -1 ? "" : bitrate + " kbps";
    }
    
    @Override
    public int compareTo(Bitrate o) {
        return Long.compare(bitrate, o.bitrate);
    }
    
    @Dependency("Name. Used by String Parser by reflection discovered by method name.")
    @Dependency("Must be consistent with toString()")
    @Dependency("Used in search filters")
    public Bitrate fromString(String s) {
        return new Bitrate(Integer.parseInt(s));
    }
}
