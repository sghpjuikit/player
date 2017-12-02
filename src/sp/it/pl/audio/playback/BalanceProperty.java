/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sp.it.pl.audio.playback;

import javafx.beans.property.SimpleDoubleProperty;
import static java.lang.Math.abs;
import static sp.it.pl.util.Util.clip;

/** 
 * Double property for left-right speaker volume balance, with additional 
 * methods. Makes sure the value is always within valid range.
 */
public final class BalanceProperty extends SimpleDoubleProperty {
    
    public static final double MIN = -1;
    public static final double MAX = 1;
    public static final double AVG = (MAX+MIN)/2;
    public static final double STEP = abs(MAX-MIN)/20;
    
    /** Initializes default value: 0 */
    public BalanceProperty() {
        super(AVG);
    }    
    
    /** Initializes to provided value. */
    public BalanceProperty(double v) {
        super(clip(MIN,v,MAX));
    }

    /** Sets the value. Value outside of minimal-maximal value range will be clipped. */
    @Override
    public void set(double v) {
        super.set(clip(MIN,v,MAX));
    }
    
    /** @return minimum volume value: -1 */
    public double getMin() {
        return MIN;
    }
    
    /** @return average volume value: 0 */
    public double getAverage() {
        return AVG;
    }
    
    /** @return maximum volume value: 1 */
    public double getMax() {
        return MAX;
    }
    
    
    /** Increment value by step. */
    public void left() {
        set(get()+STEP);
    }

    /** Decrement value by step. */
    public void right() {
        set(get()-STEP);
    }
}