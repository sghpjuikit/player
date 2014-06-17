/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package AudioPlayer.playback;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * 
 * Balance Property class.
 * Encapsulates balance property. Value ranges from -1 to 1. Any value outside of the range will be converted to -1, respectively 1.
 */
public class BalanceProperty {
    private final DoubleProperty balance = new SimpleDoubleProperty(this,"balance", 0.0);
    
    /**
     * Constructor.
     * Initializes default value 0.
     */
    public BalanceProperty() {
    }    
    
    /**
     * Constructor.
     * @param val Takes all values, but converges them into <-1;1>.
     */
    public BalanceProperty(double val) {
        balance.set(Balance.balanceValue(val));
    } 
    
    /**
     * @return Value of this property.
     */
    public double get() {
        return balance.get();
    }
    
    /**
     * @param val 
     */
    public void set(Balance val) {
        balance.set(val.get());
    }
    
    /**
     * @param val Takes all double values, but converges them into <-1;1>.
     */
    public void set(double val) {
        balance.set(Balance.balanceValue(val));
    }
    
    /**
     * @return DoubleProperty balance.
     */
    public DoubleProperty balanceProperty() {
        return balance;
    }  
}
