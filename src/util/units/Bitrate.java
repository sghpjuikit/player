/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.units;

import static java.lang.Integer.parseInt;
import jdk.nashorn.internal.ir.annotations.Immutable;
import util.dev.Dependency;
import static util.Util.shouldBeEmpty;

/**
 * Simple class for media bit rate. Internally represents the value as int.
 * 
 * @author uranium
 */
@Immutable
public class Bitrate implements Comparable<Bitrate> {
    private static final String UNIT = " kbps";
    private final int bitrate;
    
    /**
     * @param value bit rate value in kb per second. Use -1 if not available.
     */
    public Bitrate(int value){
        if(value<-1) throw new IllegalArgumentException("Bitrate value must be -1 or larger");
        bitrate = value;
    }
    
    /** @return bit rate value in kb per second. */
    public int getValue() {
        return bitrate;
    }
    
    @Override
    public int compareTo(Bitrate o) {
        return Integer.compare(bitrate, o.bitrate);
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
        return bitrate == -1 ? "" : bitrate + UNIT;
    }
    
    @Dependency("Name. Used by String Parser by reflection discovered by method name.")
    @Dependency("Must be consistent with toString()")
    @Dependency("Used in search filters")
    public static Bitrate fromString(String s) {
        if(s.endsWith(UNIT)) s=s.substring(0, s.length()-UNIT.length());
        return new Bitrate(shouldBeEmpty(s) ? -1 : parseInt(s));
    }
}
