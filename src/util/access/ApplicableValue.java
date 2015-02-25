/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.util.function.Consumer;

/**
 *
 * @author Plutonium_
 */
public interface ApplicableValue<T> extends AccessibleValue<T> {
    
    /**
     * Similar to {@link #applyValue()}, but instead of value of this accessor,
     * provided value is used
     * <p>
     * This method is a setter like {@link #setValue(java.lang.Object)}, but the
     * value is not set, only still applied. So the immediate effect is the same,
     * but there is still value to fall back on later.
     * <p>
     * Useful for internal application within the object, where the value should
     * change, but when queried (getValue)) from outside, this should not be
     * reflected.
     * 
     * @param val 
     */
    public void applyValue(T val);
    
    /**
     * Applies contained value using the applier.
     * Equivalent to {@code applyValue(getValue()); }.
     */
    public default void applyValue() {
        applyValue(getValue());
    }
    
    /**
     * Applies contained value using provided applier. 
     * Equivalent to calling {@code applier.accept(getValue()); }.
     */
    public default void setNapplyValue(Consumer<T> applier) {
        applier.accept(getValue());
    }
    
    /**
     * Sets value and applies using the applier.
     * Equivalent to {@code setValue(val); applyValue(); }.
     */
    public default void setNapplyValue(T val) {
        setValue(val);
        applyValue();
    }
    
    /**
     * Equivalent to calling {@link #setNextValue()} and then {@link #applyValue()}
     * subsequently.
     */
    public default void setNextNapplyValue() {
        setNextValue();
        applyValue();
    }
    /**
     * Equivalent to calling {@link #setPreviousValue()} and then {@link #applyValue()}
     * subsequently.
     */
    public default void setPreviousNapplyValue() {
        setPreviousValue();
        applyValue();
    }
    /**
     * Equivalent to calling {@link #setCycledValue())} and then {@link #applyValue()}
     * subsequently.
     */
    public default void setCycledNapplyValue() {
        setCycledValue();
        applyValue();
    }
    
    
}
