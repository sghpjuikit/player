/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.access;

import java.util.function.Consumer;

import javafx.beans.property.SimpleObjectProperty;

import static java.util.Objects.requireNonNull;

/**
 * Var/variable - simple object wrapper similar to {@link javafx.beans.property.Property}, but
 * simpler (no binding) and with the ability to apply value change.
 * <p>
 * Does not permit null values.
 * 
 * @author Plutonium_
 */
public class Ѵ<V> extends SimpleObjectProperty<V> implements ApplicableValue<V> {
    
    private Consumer<V> applier;
    
    public Ѵ(V val) {
        setValue(val);
    }
    
    public Ѵ(V val, Consumer<V> applier) {
        this(val);
        setApplier(applier);
    }
    
    public Ѵ(V val, Runnable applier) {
        this(val,v -> applier.run());
    }
    
    /** {@inheritDoc} */
    @Override
    public void applyValue(V val) {
        requireNonNull(val);
        if (applier != null) applier.accept(val);
    }
    
    /** 
     * Sets applier. Applier is a code that applies the value in any way. 
     * 
     * @param applier or null to disable applying
     */
    public final void setApplier(Consumer<V> applier) {
        this.applier = applier;
    }
    
    /** 
     * Gets applier. Applier is a code that applies the value. It can do anything.
     * Default null.
     * 
     * @return applier or null if none.
     */
    public Consumer<V> getApplier() {
        return applier;
    }
    
}