
package AudioPlayer.playback;

/**
 * Custom wrapper of double representing player's volume property. Value ranges
 * from 0 to 1. Any value outside of the range will be converted to 0,
 * respectively 1.
 */
final class Volume {
    private double value;
    private static final double min = 0;
    private static final double max = 1;
    
    /**
     * Constructor.
     */
    public Volume() {
    }
    
    /**
     * Constructor.
     */    
    private Volume(double val) {
        set(val);
    }
    
    /**
     * Getter.
     * @return Value.
     */
    public double get() {
        return value;
    }
    
    /**
     * Setter.
     * @param val Takes all double values, but converges them into <0;1>.
     */
    public void set(double val) {
        value = volumeValue(val);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    /**
     * Creates volume from string. More formally parses specified string to double
     * and returns new Volume object initialized to parsed value fit inside volume
     * bounds specifications.
     * @param str
     * @return 
     */
    public static Volume create(String str) {
        return new Volume(Double.parseDouble(str));
    }
    /**
     * Creates volume initialized to specified double value fit inside volume
     * bounds specifications.
     * @param val
     * @return 
     */
    public static Volume create(double val) {
        return new Volume(val);
    }
    
    /**
     * minimal volume value. 
     * @return 0.
     */
    public static double min() {
        return min;
    }
    
    /**
     * maximal volume value.
     * @return 1.
     */
    public static double max() {
        return max;
    }
    
    /**
     * Shrinks double value so it fits the range set for Volume
     * @param val
     * @return 
     */
    public static double volumeValue(double val) {
        if (val > max)
            return max;
        else if (val < min)
            return min;
        else
            return val;        
    }
    
    
}
