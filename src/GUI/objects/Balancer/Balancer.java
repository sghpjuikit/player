

package GUI.objects.Balancer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;

/**
 *
 * @author uranium
 */
public class Balancer extends Control {

    /***************************************************************************
     * 
     * Constructors
     * 
     **************************************************************************/
    
    /** Creates a default instance with default balance, min, max values. */
    public Balancer() {
        getStyleClass().setAll("balancer");
        setBalance(getBalance()); // refreshes graphics on creation
    }
    /** Creates a default instance with a specified balance value and
     * default min, max values. */
    public Balancer(double balance) {
        this();
        setBalance(balance);
    }
    /**
     * Creates a Balancer instance with specified current, min and max
     * value.
     * @param balance The maximum allowed rating value. <-1,1>
     */
    public Balancer(double balance, double min, double max) {
        this(balance);
        setMax(max);
        setMin(min);
    }

    /***************************************************************************
     * 
     * Overriding public API
     * 
     **************************************************************************/
    
    /** {@inheritDoc} */
    @Override protected Skin<?> createDefaultSkin() {
        return new BalancerSkin(this);
    }

    /** {@inheritDoc} */
    @Override public String getUserAgentStylesheet() {
        return getClass().getResource("balancer.css").toExternalForm();
    }
    
    
    
    /***************************************************************************
     * 
     * Properties
     * 
     **************************************************************************/
    
    // --- Balance
    private DoubleProperty balance = new SimpleDoubleProperty(this, "balance", 0);
    
    /** @return The current balance value. */
    public final DoubleProperty balanceProperty() {
        return balance;
    }
    
    /**  Sets the current balance value. */
    public final void setBalance(double value) {
       double to = value;
       
       if (to > getMax()) to = getMax();
       if (to < getMin()) to = getMin();
       
       if (Math.abs(to)<0.2) to = 0;
       if (to-getMin() <0.2) to = getMin();
       if (getMax()-to <0.2) to = getMax();
        
       balanceProperty().set(to);
    }
    
    /**  Returns the current balance value. */
    public final double getBalance() {
        return balance == null ? 0 : balance.get();
    }
    
    // --- Max
    private DoubleProperty max = new SimpleDoubleProperty(this, "max", 1);
    
    /** The maximum-allowed rating value. */
    public final DoubleProperty maxProperty() {
        return max;
    }
    
    /**  Sets the maximum-allowed rating value. */
    public final void setMax(double value) {
       maxProperty().set(value);
    }
    
    /** Returns the maximum-allowed rating value. */
    public final double getMax() {
        return max.get();
    }
    
    // --- Min
    private DoubleProperty min = new SimpleDoubleProperty(this, "min", -1);
    
    /** The maximum-allowed rating value. */
    public final DoubleProperty minProperty() {
        return min;
    }
    
    /**  Sets the maximum-allowed rating value. */
    public final void setMin(double value) {
       minProperty().set(value);
    }
    
    /** Returns the maximum-allowed rating value. */
    public final double getMin() {
        return min.get();
    }
    
}
