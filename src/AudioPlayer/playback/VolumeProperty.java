/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * Volume Property class.
 * Encapsulates volume property. Value ranges from 0 to 1. Any value outside of 
 * the range will be converted to 0, respectively 1.
 */
public final class VolumeProperty {
    private final DoubleProperty volume = new SimpleDoubleProperty(this,"volume", 0.5);
    private final DoubleProperty interpolated = new SimpleDoubleProperty(this,"interpolated", 0.5);
    
    /**
     * Initializes default value - 0.5.
     */
    public VolumeProperty() {
    }    
    
    /**
     * @param val Takes all values, but converges them into <0;1>.
     */
    public VolumeProperty(double val) {
        volume.set(Volume.volumeValue(val));
    }    
    
    /**
     * @return Value of this property.
     */
    public double get() {
        return volume.get();
    }
    
    /**
     * @param val
     */
    public void set(Volume val) {
        volume.set(val.get());
    }
    
    /**
     * @param val Takes all double values, but converges them into <0;1>.
     */
    public void set(double val) {
        volume.set(Volume.volumeValue(val));
    }
    
    /**
     * @return DoubleProperty volume.
     */
    public DoubleProperty volumeProperty() {
        return volume;
    }    
}
