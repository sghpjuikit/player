/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.util.function.Consumer;
import java.util.function.Supplier;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 *
 * @author Martin Polakovic
 */
@Immutable
public class FunctAccessor<T> implements FunctAccessibleValue<T> {
    
    private final Consumer<T> setter;
    private final Supplier<T> getter;
    private final Consumer<T> applier;
    
    /**
     * @param setter defines how the password will be set
     * @param getter defines how the password will be accessed
     */
    public FunctAccessor(Consumer<T> setter, Supplier<T> getter) {
        this(setter, getter, null);
    }
    
    /**
     * @param setter defines how the password will be set
     * @param getter defines how the password will be accessed
     */
    public FunctAccessor(Consumer<T> setter, Supplier<T> getter, Consumer<T> applier) {
        this.getter = getter;
        this.setter = setter;
        this.applier = applier;
    }
    
    @Override
    public final Consumer<T> getSetter() {
        return setter;
    }
    
    @Override
    public final Supplier<T> getGetter() {
        return getter;
    }

    @Override
    public final T getValue() {
        return getter.get();
    }

    @Override
    public final void setValue(T value) {
        setter.accept(value);
    }

    @Override
    public void applyValue(T val) {
        if (applier != null) applier.accept(val);
    }
    
    
}