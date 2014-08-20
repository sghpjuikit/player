/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package utilities.access;

import java.util.function.Consumer;

/**
 *
 * @author Plutonium_
 */
public class Accessor<T> implements AccessibleValue<T> {
    
    private T value;
    private Consumer<T> applier;
    
    public Accessor(T val) {
        value = val;
    }
    
    public Accessor(T val, Consumer<T> applier) {
        value = val;
        this.applier = applier;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setValue(T val) {
        value = val;
        if (applier != null) applier.accept(val);
    }
    
    public void setApplier(Consumer<T> applier) {
        this.applier = applier;
    }
    
}
