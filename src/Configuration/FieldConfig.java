/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Configuration.Config.ConfigBase;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Objects;
import util.dev.Log;

/**
 * Class level {@link Config}.
 * <p>
 * Wraps static {@link Field}.
 * <p>
 * Use for class level configurations.
 * 
 * @author Plutonium_
 */
public final class FieldConfig<T> extends ConfigBase<T> {
    
    private final Object instance;
    MethodHandle getter;
    MethodHandle setter;
    MethodHandle applier;
    
    /**
     * @param _name
     * @param c
     * @param category
     * @param instance owner of the field or null if static
     */
    FieldConfig(String _name, IsConfig c, Object instance, String category, MethodHandle getter, MethodHandle setter) {
        super(_name, c, getValueFromMethodHelper(getter, instance), category);
        this.getter = getter;
        this.setter = setter;
        this.instance = instance;
    }
    
    /** {@inheritDoc} */
    @Override
    public T getValue() {
        return getValueFromMethodHelper(getter, instance);
    }
    
    /** {@inheritDoc} */
    @Override
    public void setValue(T val) { 
        try {
            if(instance==null) setter.invokeWithArguments(val);
            else setter.invokeWithArguments(instance,val);
            //Log.deb("Config field: " + getName() + " set to: " + val);
        } catch (Throwable e) {
            Log.err("Config field: " + getName() + " failed to set. Reason: " + e.getMessage());
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void applyValue(T val) {
        if(applier != null) {
            //Log.deb("Applying config: " + getName());
            try {
                int i = applier.type().parameterCount();
                
                if(i==1) applier.invokeWithArguments(val);
                else applier.invoke();
                
                //Log.deb("    Success.");
            } catch (Throwable e) {
                Log.err("    Failed to apply config field: " + getName() + ". Reason: " + e.getMessage());
            }
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public Class getType() {
        return getValue().getClass();
    }
    
    /** 
     * Equals if and only if non null, is Config type and source field is equal.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if (o == null || !(o instanceof FieldConfig)) return false;
        
        FieldConfig c = (FieldConfig)o;
        return setter.equals(c.setter) && getter.equals(c.getter) &&
               applier.equals(c.applier); 
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Objects.hashCode(this.applier);
        hash = 23 * hash + Objects.hashCode(this.getter);
        hash = 23 * hash + Objects.hashCode(this.setter);
        return hash;
    }
    
    
/******************************************************************************/
    
    private static<T> T getValueFromMethodHelper(MethodHandle mh, Object instance) {
        try {
            if(instance==null) return (T) mh.invoke();
            else return (T) mh.invokeWithArguments(instance);
        } catch (Throwable ex) {
            throw new RuntimeException("Error during getting value from a config field. " + ex.getClass() + " " + ex.getMessage());
        }
    }
}
        
//    // helper method to obtain initial value while enforcing static field check
//    // use in super() constructor
//    private static<T> T getValueFromField(Field f, Object instance) {
//        try {
//            // make sure the preconditions apply
//            boolean isStatic = Modifier.isStatic(f.getModifiers());
//            if(instance==null && !isStatic)
//                throw new IllegalStateException("Object instance null with instance field config not allowed.");
//            if(instance!=null && isStatic)
//                throw new IllegalStateException("Object instance not null when field is static not allowed.");
//            // make sure field is accessible
//            f.setAccessible(true);
//            // get value
//            return (T) f.get(instance);
//        } catch (IllegalArgumentException | IllegalAccessException ex) {
//            throw new RuntimeException("Can not access field: " + f.getName() + " for object: " + instance);
//        }
//    }
