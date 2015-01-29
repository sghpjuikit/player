/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;

/**
 * Simple object wrapper similar to {@link javafx.beans.property.Property}, but
 * simpler (no binding) and with the ability to apply value change.
 * <p>
 * Does not permit null values.
 * 
 * @author Plutonium_
 */
public class Accessor<T> implements ApplicableValue<T> {
    
    private T value;
    private Consumer<T> applier;
    
    public Accessor(T val) {
        this(val, null);
    }
    
    public Accessor(T val, Consumer<T> applier) {
        setValue(val);
        setApplier(applier);
    }
    
    /** 
     * {@inheritDoc}
     * 
     * @return non null value
     */
    @Override
    public T getValue() {
        requireNonNull(value);
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public final void setValue(T val) {
        requireNonNull(val);
        value = val;
    }
    
    /** {@inheritDoc} */
    @Override
    public void applyValue(T val) {
        requireNonNull(val);
        if (applier != null) applier.accept(val);
    }
    
    /** 
     * Sets applier. Applier is a code that applies the value in any way. 
     * 
     * @param applier or null to disable applying
     */
    public final void setApplier(Consumer<T> applier) {
        this.applier = applier;
    }
    
    /** 
     * Gets applier. Applier is a code that applies the value. It can do anything.
     * Default null.
     * 
     * @return applier or null if none.
     */
    public Consumer<T> getApplier() {
        return applier;
    }
    
}
