/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package PseudoObjects;

import javafx.util.Duration;
import utilities.Util;

/**
 * Duration with overriden toString method, where it is formatted into
 * minutes:seconds format.* Example: 00:00.
 */
public class FormattedDuration extends Duration {
    private static final long serialVersionUID = 11L;
    
    /* Constructor. Initializes to 0.  */
    public FormattedDuration() {
        super(0);
    }
    
    /* Constructor. Initializes to specified value in milliseconds.  */
    public FormattedDuration(double value) {
        super(value);
    }
    
    /** @return formatted string representation of the duration */
    @Override
    public String toString () {
        return Util.formatDuration(this);
    } 
}
