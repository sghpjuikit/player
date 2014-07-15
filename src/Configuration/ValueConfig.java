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
 * 
 * @param V - type of value - wrapped object
 *
 * @author Plutonium_
 */
public final class ValueConfig<V> extends Config<V> {
    
    private V value;
    private UnProcedure<V> applier;
    
    public ValueConfig(String name, String gui_name, V value, String category, String info, boolean editable, boolean visible, double min, double max, UnProcedure<V> onChange) {
        super(name, gui_name, value, name, info, editable, visible, min, max);
        this.value = value;
        this.applier = onChange;
    }
    
    public ValueConfig(String name, V value) {
        super(name, name, value, "", "", true, true, Double.NaN, Double.NaN);
        this.value = value;
    }
    
    public ValueConfig(String name, V value, UnProcedure<V> onChange) {
        super(name, name, value, "", "", true, true, Double.NaN, Double.NaN);
        this.value = value;
        this.applier = onChange;
    }
    
    public ValueConfig(String name, V value, String info) {
        super(name, name, value, "", info, true, true, Double.NaN, Double.NaN);
        this.value = value;
    }
    
    public ValueConfig(String name, V value, String info, UnProcedure<V> onChange) {
        super(name, name, value, "", info, true, true, Double.NaN, Double.NaN);
        this.value = value;
        this.applier = onChange;
    }
    
    /** 
     * {@inheritDoc} 
     * <p>
     * Note that if the value changed see {@link #setValue(java.lang.Object)},
     * the returned object reference will most likely be entirely new one. 
     * Dont store the old object in order to avoid using this getter and directly
     * access the value with the expectation it will have changed. What is changing
     * is the object itself not its value (if you wish to only change the value
     * use {@link PropertyConfig}). After the change the result can only be 
     * obtained by calling this method and the old results will not == equal 
     * with it anymore.
     * 
     * @return the wrapped value. Never null. The wrapped value must no be
     * null.
     */
    @Override
    public V getValue() {
        return value;
    }
    
    /** {@inheritDoc} 
     * <p>
     * Note that if the value changed see {@link #setValue(java.lang.Object)},
     * the returned object reference will most likely be entirely new one. 
     * Dont store the old object in order to avoid using this getter and directly
     * access the value with the expectation it will have changed. What is changing
     * is the object itself not its value (if you wish to only change the value
     * use {@link PropertyConfig}). After the change the result can only be 
     * obtained by calling this method and the old results will not == equal 
     * with it anymore.
     * 
     * @throws NullPointerException if param null. The wrapped value must no be
     * null.
     */
    @Override
    public boolean setValue(V val) {
        Objects.requireNonNull(val);
        value = (V) val;
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
    public Class<V> getType() {
        return (Class<V>) value.getClass();
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

    /**
     * Equivalent to this==o
     * @param o
     * @return 
     */
    @Override
    public boolean equals(Object o) {
        return this==o;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        return hash;
    }
    
    
    
}
