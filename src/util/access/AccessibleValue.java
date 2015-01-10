/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import javafx.beans.value.WritableValue;

/**
 * Extension of {@link WritableValue), that supports {@link SequentialValue}. This
 * is a lightweight interface for objects with an access to a value.
 * <p>
 * Intended for object wrappers.
 * 
 * @author Plutonium_
 */
public interface AccessibleValue<T> extends WritableValue<T>, SequentialValue<T> {
    
    /**
     * Equivalent to calling {@link #setValue()} with {@link #next()};
     */
    public default void setNextValue() {
        setValue(next());
    }
    
    /**
     * Equivalent to calling {@link #setValue()} with {@link #previous()};
     */
    public default void setPreviousValue() {
        setValue(previous());
    }
    
    /**
     * Equivalent to calling {@link #setValue()} with {@link #cycle()};
     */
    public default void setCycledValue() {
        setValue(cycle());
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * Only available for {@link SequentialValue} types for which
     * next value is returned, {@link Boolean}
     * which is equivalent to negation and {@link Enum} which return the next declared
     * enum constant. Otherwise does nothing.
     */
    @Override
    public default T next() {
        T val = getValue();
        if(val instanceof SequentialValue)
            return ((SequentialValue<T>)getValue()).next();
        else if(val instanceof Boolean)
            return (T) SequentialValue.next(Boolean.class.cast(getValue()));
        else if(val instanceof Enum)
            return (T) SequentialValue.next(Enum.class.cast(getValue()));
        else return val;
    }
    
    /**
     * Only available for:
     * <ul>
     * <li> {@link SequentialValue} types for which previous value is returned
     * <li> {@link Boolean} which is equivalent to negation
     * <li> {@link Enum} which return the previous declared enum constant.
     * </ul>
     * Otherwise does nothing.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public default T previous() {
        T val = getValue();
        if(val instanceof SequentialValue)
            return ((SequentialValue<T>)getValue()).previous();
        else if(val instanceof Boolean)
            return (T) SequentialValue.previous(Boolean.class.cast(getValue()));
        else if(val instanceof Enum)
            return (T) SequentialValue.previous(Enum.class.cast(getValue()));
        else return val;
    }
}
