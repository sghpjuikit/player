/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package util.units;

import static java.lang.Integer.parseInt;
import jdk.nashorn.internal.ir.annotations.Immutable;
import util.dev.Dependency;
import util.parsing.StringParseStrategy;
import static util.parsing.StringParseStrategy.From.CONSTRUCTOR_STR;
import static util.parsing.StringParseStrategy.To.TO_STRING_METHOD;

/**
 * Simple class for media bit rate. Internally represents the value as int.
 * Unit is kbps.
 * 
 * @author uranium
 */
@Immutable
@StringParseStrategy(
    from = CONSTRUCTOR_STR,
    to = TO_STRING_METHOD,
    ex = { IndexOutOfBoundsException.class, NumberFormatException.class, IllegalArgumentException.class }
)
public class Bitrate implements Comparable<Bitrate> {
    private static final String UNIT = "kbps";
    private final int bitrate;
    
    /**
     * @param value bit rate value in kb per second. Use -1 if not available.
     */
    public Bitrate(int value){
        if(value<-1) throw new IllegalArgumentException("Bitrate value must be -1 or larger");
        bitrate = value;
    }
    
    /***
     * @param s number as string with optional unit 'kbps' appended. White spaces
     * are removed.
     */
    public Bitrate(String s){
        this(val(s));
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
    @Dependency("Designed to be used in tables, filters and gui.")
    public String toString() {
        return bitrate == -1 ? "" : bitrate + " " + UNIT;
    }

    /** @return true if the value is the same. */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        return o instanceof Bitrate && ((Bitrate)o).bitrate==bitrate;
    }

    @Override
    public int hashCode() {
        return 97 * 7 + this.bitrate;
    }
    
    
    
    
    private static int val(String s) throws IndexOutOfBoundsException, NumberFormatException {
        if(s.endsWith(UNIT)) s=s.substring(0, s.length()-UNIT.length());
        s = s.trim();
        return s.isEmpty() ? -1 : parseInt(s);
    }
}
