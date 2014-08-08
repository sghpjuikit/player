/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Configuration.Config.ConfigBase;
import java.util.function.Supplier;
import javafx.util.Callback;

/**
 * Functional implementation of {@link Config} that doesnt store nor wrap the
 * value, instead contains the getter and setter which call the code that
 * provides the actual value. This can be thought of some kind of intermediary.
 * See {@link Accessor} which this config implements.
 * <p>
 * Use when wrapping the value is not desired, rather it is defined by a means
 * of accessing it.
 *
 * @author Plutonium_
 */
public class AccessorConfig<T> extends ConfigBase<T> implements Accessor<T> {
    
    private final Callback<T,Boolean> setter;
    private final Supplier<T> getter;
    
    /**
     * @param setter defines how the value will be set
     * @param getter defines how the value will be accessed
     */
    public AccessorConfig(String name, String gui_name, Callback<T,Boolean> setter,
            Supplier<T> getter, String category, String info, boolean editable, double min, double max) {
        super(name, gui_name, getter.get(), name, info, editable, min, max);
        this.getter = getter;
        this.setter = setter;
    }
    
    /**
     * @param setter defines how the value will be set
     * @param getter defines how the value will be accessed
     */
    public AccessorConfig(String name, Callback<T,Boolean> setter, Supplier<T> getter) {
        super(name, name, getter.get(), "", "", true, Double.NaN, Double.NaN);
        this.getter = getter;
        this.setter = setter;
    }
    
    /**
     * @param setter defines how the value will be set
     * @param getter defines how the value will be accessed
     */
    public AccessorConfig(String name, Callback<T,Boolean> setter, Supplier<T> getter, String info) {
        super(name, name, getter.get(), "", info, true, Double.NaN, Double.NaN);
        this.getter = getter;
        this.setter = setter;
    }
    
    
    /** {@inheritDoc} */
    @Override
    public Callback<T, Boolean> getSetter() {
        return setter;
    }
    
    /** {@inheritDoc} */
    @Override
    public Supplier<T> getGetter() {
        return getter;
    }
    
    
    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return getter.get();
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean setValue(T val) {
        return setter.call(val);
    }
    
    /** {@inheritDoc} */
    @Override
    public boolean applyValue() {
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public Class getType() {
        return getValue().getClass();
    }
    
}
