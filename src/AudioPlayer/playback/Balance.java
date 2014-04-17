/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

/**
 * Custom wrapper of double representing player's balance property. Value ranges
 * from 0 to 1. Any value outside of the range will be converted to 0,
 * respectively 1.
 */
final class Balance {
    private double value;
    private static final double min = -1;
    private static final double max = 1;
    
    /**
     * Constructor.
     */
    public Balance() {
    }
    
    /**
     * Constructor.
     */    
    private Balance(double val) {
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
     * @param val Takes all double values, but converges them into <-1;1>.
     */
    public void set(double val) {
        value = balanceValue(val);
    }
    
    @Override
    public String toString() {
        return String.valueOf(value);
    }
    
    /**
     * Creates balance from string. More formally parses specified string to double
     * and returns new Balance object initialized to parsed value fit inside balance
     * bounds specifications.
     * @param str
     * @return 
     */
    public static Balance create(String str) {
        return new Balance(Double.parseDouble(str));
    }
    /**
     * Creates balance initialized to specified double value fit inside balance
     * bounds specifications.
     * @param val
     * @return 
     */
    public static Balance create(double val) {
        return new Balance(val);
    }
    
    /**
     * minimal balance value. 
     * @return 0.
     */
    public static double min() {
        return min;
    }
    
    /**
     * maximal balance value.
     * @return 1.
     */
    public static double max() {
        return max;
    }
    
    /**
     * Shrinks double value so it fits the range set for Balance
     * @param val
     * @return balance value
     */
    public static double balanceValue(double val) {
        if (val > max)
            return max;
        else if (val < min)
            return min;
        else
            return val;        
    }
}
