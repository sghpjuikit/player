/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javafx.beans.value.WritableValue;

import util.functional.Operable;

/**
 * {@link WritableValue), with added default methods. A lightweight interface
 * for value wrappers.
 * <p>
 * The value does not have to be wrapped directly within this object, rather
 * this object is a means to access it, hence the more applicable name -
 * accessible vlaue.
 *
 * @param <V> type of accessible value
 * @see SequentialValue
 * @see Operable
 * @author Plutonium_
 */
public interface AccessibleValue<V> extends WritableValue<V>, SequentialValue<V>, Operable<V> {

    /** Sets value to specified. Convenience setter. */
    public default void setVof(WritableValue<V> value) {
        setValue(value.getValue());
    }

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
    public default V next() {
        V val = getValue();
        if(val instanceof SequentialValue)
            return ((SequentialValue<V>)getValue()).next();
        else if(val instanceof Boolean)
            return (V) SequentialValue.next(Boolean.class.cast(getValue()));
        else if(val instanceof Enum)
            return (V) SequentialValue.next(Enum.class.cast(getValue()));
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
    public default V previous() {
        V val = getValue();
        if(val instanceof SequentialValue)
            return ((SequentialValue<V>)getValue()).previous();
        else if(val instanceof Boolean)
            return (V) SequentialValue.previous(Boolean.class.cast(getValue()));
        else if(val instanceof Enum)
            return (V) SequentialValue.previous(Enum.class.cast(getValue()));
        else return val;
    }

/******************************************************************************/

    @Override
    public default V apply(UnaryOperator<V> op) {
        return op.apply(getValue());
    }

    @Override
    public default <R> R apply(Function<V,R> op) {
        return op.apply(getValue());
    }

    @Override
    public default V apply(V e, BinaryOperator<V> op) {
        return op.apply(getValue(), e);
    }

    @Override
    public default void use(Consumer<V> op) {
        op.accept(getValue());
    }

    @Override
    public default V useAnd(Consumer<V> op) {
        V v = getValue();
        op.accept(v);
        return v;
    }



    public default void setValueOf(UnaryOperator<V> op) {
        setValue(op.apply(getValue()));
    }

    public default void setValueOf(V v2, BinaryOperator<V> op) {
        setValue(op.apply(getValue(), v2));
    }


}
