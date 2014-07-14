/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.util.Objects;
import utilities.functional.functor.UnProcedure;

/**
 * {@link Config} wrapper for a standalone object. This is the only implementation
 * of the Config that can be instantiated.
 * <p>
 * The aim is to create custom Configs that are not associated with any field and
 * are not a part of the automated configuration framework. Instead they simply
 * wrap an Object value.
 * <p>
 * The sole use case of this class is for {@link ValueConfigurable}. Normally
 * a {@link Configurable} exports object's properties and attributes by introspection
 * and reflection. Because Configurable provides standard to accessing and configuring
 * itself from outside transparently, sometimes it is desirable to wrap an object
 * into a Config and construct a Configurable around it. This Configurable could
 * aggregate multiple unrelated values and virtually pass them off as a configurable
 * portion of some object which does not exist.
 * <p>
 * In reality ValueConfig simply pretends and only sets and returns its wrapped
 * value.
 * <p>
 * An expected example for the mentioned use case is to pass values into a method
 * or object that takes Configurable as a parameter. This object usually operates
 * with the configs in order to change state of the configured object (in this 
 * case we simply change the values itself - each value represented by one
 * ValueConfig and aggregated by ValueConfigurable). The modification is could
 * be done by the user through GUI.
 * <p>
 * Basically, this class can be used to pass an object somewhere to modify its
 * value and then provide the result.
 * <p>
 * <p>
 * The class is generified to improve type safety. Note, that if aggregated in
 * ValueConfigurable that is not generified, a casting will still be necessary
 * to get exact parameterized type. If this is not done the {@link #getValue()}
 * will return Object instead of V. Avoid casting the wrapped object to V and
 * rather cast the into ValueConfig first.
 * 
 * @param V - type of value - wrapped object
 *
 * @author Plutonium_
 */
public final class ValueConfig<V> extends Config {
    
    private V value;
    private UnProcedure<V> applier;
    
    public ValueConfig(String name, String gui_name, V value, String category, String info, boolean editable, boolean visible, double min, double max, UnProcedure<V> onChange) {
        super(name, gui_name, value, name, info, editable, visible, min, max, null);
        this.value = value;
        this.applier = onChange;
    }
    
    public ValueConfig(String name, V value) {
        super(name, name, value, "", "", true, true, Double.NaN, Double.NaN, null);
        this.value = value;
    }
    
    public ValueConfig(String name, V value, UnProcedure<V> onChange) {
        super(name, name, value, "", "", true, true, Double.NaN, Double.NaN, null);
        this.value = value;
        this.applier = onChange;
    }
    
    public ValueConfig(String name, V value, String info) {
        super(name, name, value, "", info, true, true, Double.NaN, Double.NaN, null);
        this.value = value;
    }
    
    public ValueConfig(String name, V value, String info, UnProcedure<V> onChange) {
        super(name, name, value, "", info, true, true, Double.NaN, Double.NaN, null);
        this.value = value;
        this.applier = onChange;
    }
    
    /** 
     * {@inheritDoc} 
     * Note that if the value changed before, the returned object reference will
     * most likely be entirely new one. Dont store the old object with the expectation
     * it will have changed when setValue will be called. After the change the
     * result can only be obtained by calling this method again while the old
     * results will not match with it anymore.
     */
    @Override
    public V getValue() {
        return value;
    }
    
    /** {@inheritDoc} 
     * Note that if the value changed before, the returned object reference will
     * most likely be entirely new one. Dont store the old object with the expectation
     * it will have changed when setValue will be called. After the change the
     * result can only be obtained by calling getValue again while the old
     * results will not match with it anymore.
     * @param value to set. Must not be null.
     */
    @Override
    public boolean setValue(Object val) {
        Objects.requireNonNull(val);
        value = (V) val;
        return true;
    }
    
    /** 
     * Generic and type safe version of {@link #setValue(java.lang.Object)}. Always
     * use where possible.
     * @param value to set. Must not be null.
     */
    public boolean setValueSafe(V val) {
        Objects.requireNonNull(val);
        value = val;
        return true;
    }

    /** 
     * {@inheritDoc} 
     * Runs the associated runnable to apply the changes of the value or to
     * simply execute some code.
     * <p>
     * Mostly called automatically by the object/framework doing the modification.
     */
    @Override
    public boolean applyValue() {
        if(getApplier()!=null) getApplier().accept(value);
        return true;
    }
    
    /** {@inheritDoc} */
    @Override
    public Class<?> getType() {
        return value.getClass();
    }

    /**
     * @return the applier
     */
    public UnProcedure<V> getApplier() {
        return applier;
    }

    /**
     * Sets applier. The applies takes a parameter which is value of this Config
     * at the time of applier's execution.
     * @param applier Runnable to apply the changes of the value or to
     * simply execute some code when there is intention to apply the value.
     * The runnable is called in {@link #applyValue()} method.
     * <p>
     * For example apply a different css stylesheet to an object or the application.
     * Assuming there is a standalone value as a configuration it needs to be
     * applied
     */
    public void setApplier(UnProcedure<V> applier) {
        this.applier = applier;
    }
    
    
    
}
