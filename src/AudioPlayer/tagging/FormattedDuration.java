/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.tagging;

import javafx.util.Duration;
import util.Dependency;
import util.Util;

/**
 * Duration with overriden toString method, where it is formatted into
 * minutes:seconds format.* Example: 00:00.
 */
public class FormattedDuration extends Duration {
    private static final long serialVersionUID = 11L;
    
    /** Constructor. Initializes to 0. */
    public FormattedDuration() {
        super(0);
    }
    
    /** Constructor. Initializes to specified value in milliseconds. */
    public FormattedDuration(double value) {
        super(value);
    }
    
    /** @return formatted string representation of the duration */
    @Dependency("Designed to be used in tables and gui. Should be in xx:xx format")
    @Dependency("Must be consistent with fromString().")
    @Override
    public String toString() {
        return Util.formatDuration(this);
    }
    
    @Dependency("Name. Used by String Parser by reflection discovered by method name.")
    @Dependency("Must be consistent with toStirng()")
    @Dependency("Supports different units to allow convenient search filters.")
    public static FormattedDuration valueOf(String time) {
        int index = -1;
        for (int i=0; i<time.length(); i++) {
            char c = time.charAt(i);
            if (!Character.isDigit(c) && c != '.' && c != '-') {
                index = i;
                break;
            }
        }

        double value = Double.parseDouble(time.substring(0, index));
        
        if (index == -1) 
            return new FormattedDuration(value);
        else {
            String suffix = time.substring(index);
            if ("ms".equals(suffix)) {
                return new FormattedDuration(value);
            } else if ("s".equals(suffix)) {
                return new FormattedDuration(1000*value);
            } else if ("m".equals(suffix)) {
                return new FormattedDuration(60000*value);
            } else if ("h".equals(suffix)) {
                return new FormattedDuration(3600000*value);
            } else {
                // Malformed suffix
                throw new IllegalArgumentException("The time parameter must have a suffix of [ms|s|m|h]");
            }
        }
    }
    
}