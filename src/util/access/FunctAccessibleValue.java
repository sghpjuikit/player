/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 *
 * @author Martin Polakovic
 */
public interface FunctAccessibleValue<T> extends ApplicableValue<T> {
    
    @Override
    default T getValue() {
        return getGetter().get();
    }

    @Override
    default void setValue(T val) {
        getSetter().accept(val);
    }
    
    Consumer<T> getSetter();

    Supplier<T> getGetter();

}