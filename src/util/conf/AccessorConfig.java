/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package util.conf;

import java.util.function.Consumer;
import java.util.function.Supplier;

import util.conf.Config.ConfigBase;
import util.access.FunctAccessibleValue;
import util.access.FunctAccessor;

/**
 * Functional implementation of {@link Config} that doesnt store nor wrap the
 * value, instead contains the getter and setter which call the code that
 * provides the actual value. This can be thought of some kind of intermediary.
 * See {@link FunctAccessor} which this config implements.
 * <p>
 * Use when wrapping the value is not desired, rather it is defined by a means
 * of accessing it.
 *
 * @author Plutonium_
 */
public class AccessorConfig<T> extends ConfigBase<T> implements FunctAccessibleValue<T> {

    private final Consumer<T> setter;
    private final Supplier<T> getter;

    /**
     * @param setter defines how the value will be set
     * @param getter defines how the value will be accessed
     */
    public AccessorConfig(Class<T> type, String name, String gui_name, Consumer<T> setter,
            Supplier<T> getter, String category, String info, boolean editable, double min, double max) {
        super(type, name, gui_name, getter.get(), name, info, editable, min, max);
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * @param setter defines how the value will be set
     * @param getter defines how the value will be accessed
     */
    public AccessorConfig(Class<T> type, String name, Consumer<T> setter, Supplier<T> getter) {
        super(type, name, name, getter.get(), "", "", true, Double.NaN, Double.NaN);
        this.getter = getter;
        this.setter = setter;
    }

    /**
     * @param setter defines how the value will be set
     * @param getter defines how the value will be accessed
     */
    public AccessorConfig(Class<T> type, String name, Consumer<T> setter, Supplier<T> getter, String info) {
        super(type, name, name, getter.get(), "", info, true, Double.NaN, Double.NaN);
        this.getter = getter;
        this.setter = setter;
    }

    @Override
    public Consumer<T> getSetter() {
        return setter;
    }

    @Override
    public Supplier<T> getGetter() {
        return getter;
    }

    @Override
    public T getValue() {
        return getter.get();
    }

    @Override
    public void setValue(T val) {
        setter.accept(val);
    }

    @Override
    public void applyValue(T val) {
        // do nothing
    }

}
