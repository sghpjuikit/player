/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.access;

import java.util.function.Consumer;

/**
 * Simple object wrapper similar to {@link javafx.beans.property.Property}, but
 * simpler and with the ability to apply value change.
 * 
 * @author Plutonium_
 */
public class Accessor<T> implements ApplicableValue<T> {
    
    private T value;
    private Consumer<T> applier;
    
    public Accessor(T val) {
        value = val;
    }
    
    public Accessor(T val, Consumer<T> applier) {
        value = val;
        this.applier = applier;
    }
    
    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public void setValue(T val) {
        value = val;
    }
    
    /** {@inheritDoc} */
    @Override
    public void applyValue() {
        if (applier != null)  applier.accept(value);
    }
    
    /** Sets applier. Applier is a code that applies the value in any way. */
    public void setApplier(Consumer<T> applier) {
        this.applier = applier;
    }
    
    /** Gets applier. Applier is a code that applies the value. It can do anything. */
    public Consumer<T> getApplier() {
        return applier;
    }
    
}
