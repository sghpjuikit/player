/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.access;

import utilities.CyclicEnum;

/**
 *
 * @author Plutonium_
 */
public interface ApplicableValue<T> extends AccessibleValue<T> {
    
    /**
     * Applies value if possible, which depends on implementation, but not value
     * itself.
     */
    public abstract void applyValue();
    
    /**
     * Equivalent to calling {@link #setValue()} and {@link #applyValue()}.
     */
    public default void setNapplyValue(T val) {
        setValue(val);
        applyValue();
    }
    
    /**
     * Loops value. Only available for {@link Boolean} and {@link CyclicEnum}
     * otherwise does nothing.
     * <p>
     * For boolean negates the value. For CyclicEnum sets next value.
     */
    public default void toggleValue() {
        T val = getValue();
        if(val instanceof Boolean)
            setValue((T)(Object)!Boolean.class.cast(getValue()));
        else if(val instanceof CyclicEnum)
            setValue((T)CyclicEnum.class.cast(getValue()).next());
    }
    
    /**
     * Equivalent to calling {@link #toggleValue()} and {@link #applyValue()}.
     */
    public default void toggleNapplyValue() {
        toggleValue();
        applyValue();
    }
    
    
}
