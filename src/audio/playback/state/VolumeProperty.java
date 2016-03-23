/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package audio.playback.state;

import javafx.beans.property.SimpleDoubleProperty;

import static java.lang.Math.abs;
import static util.Util.clip;

/** 
 * Double property for volume, with additional methods.  Makes sure the value 
 * is always within valid range.
 */
public final class VolumeProperty extends SimpleDoubleProperty {
    
    public static final double MIN = 0;
    public static final double MAX = 1;
    public static final double AVG = (MAX+MIN)/2;
    public static final double STEP = abs(MAX-MIN)/20;
    
    /** Initializes default value: 0.5 */
    public VolumeProperty() {
        super(AVG);
    }    
    
    /** Initializes to provided value. */
    public VolumeProperty(double v) {
        super(clip(MIN,v,MAX));
    }

    /** Sets the value. Value outside of minimal-maximal value range will be clipped. */
    @Override
    public void set(double v) {
        super.set(clip(MIN,v,MAX));
    }
    
    /** @return minimum volume value: 0 */
    public double getMin() {
        return MIN;
    }
    
    /** @return average volume value: 0.5 */
    public double getAverage() {
        return AVG;
    }
    
    /** @return maximum volume value: 1 */
    public double getMax() {
        return MAX;
    }
    
    /** Increment value by step. */
    public void inc() {
        set(get()+STEP);
    }

    /** Decrement value by step. */
    public void dec() {
        set(get()-STEP);
    }
    
}
