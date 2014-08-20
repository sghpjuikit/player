/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Configuration.Config.ConfigBase;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import utilities.Log;

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
    
    private final Field field;
    Method applierMethod;
    private final Object instance;
    
    /**
     * 
     * @param _name
     * @param c
     * @param category
     * @param instance owner of the field or null if static
     * @param field to wrap
     * 
     * @throws NullPointerException if field value is null. The wrapped value must no be
     * null.
     * @throws IllegalStateException if field is not static. Field must be static.
     */
    FieldConfig(String _name, IsConfig c, Object instance, String category, Field field) {
        super(_name, c, getValueFromField(field, instance), category);
        this.field = field;
        this.instance = instance;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue() {
        try {
            field.setAccessible(true);
            return (T) field.get(instance);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Config " + getName() + " can not return "
                    + "value. Wrong object type." + e.getMessage());
        } catch (IllegalAccessException | SecurityException e) {
            throw new RuntimeException("Config " + getName() + " can not access "
                    + "value. " + e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(T val) { 
        try {
            field.setAccessible(true);
            field.set(instance, val);
            Log.deb("Config field: " + getName() + " set to: " + val);
        } catch (SecurityException | IllegalAccessException ex) {
            Log.err("Config field: " + getName() + " failed to set. Reason: " + ex.getMessage());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Config value wrong object type. Cant set "
                    + getName() + " to: " + val + ".");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void applyValue() {
        Log.deb("Applying config: " + getName());
        if(applierMethod != null) {
            
            try {
                applierMethod.setAccessible(true);
                
                // create parameters
                int i = applierMethod.getParameterCount();
                Object[] params = new Object[i];
                if(i==1) params[1] = getValue();
                
                applierMethod.invoke(instance, params);
                Log.deb("    Success.");
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
                Log.err("    Failed to apply config field: " + getName() + ". Reason: " + e.getMessage());
            }
        } else {
            Log.deb("    Nothing to apply: no applier method.");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class getType() {
        return getValue().getClass(); // unfortunately the only working solution
        // field.getType() wil not work if we pass subcasses into the field
        // like Object field; field = "some string".  We ned to get the runtime
        // tpe of z
    }
    
    /** 
     * Equals if and only if non null, is Config type and source field is equal.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if (o == null || !(o instanceof FieldConfig)) return false;
        
        FieldConfig c = (FieldConfig)o;
        return field.equals(c.field); 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.field);
        return hash;
    }
    
    
/******************************************************************************/
    
    // helper method to obtain initial value while enforcing static field check
    // use in super() constructor
    private static<T> T getValueFromField(Field f, Object instance) {
        try {
            // make sure the preconditions apply
            boolean isStatic = Modifier.isStatic(f.getModifiers());
            if(instance==null && !isStatic)
                throw new IllegalStateException("Object instance null with instance field config not allowed.");
            if(instance!=null && isStatic)
                throw new IllegalStateException("Object instance not null when field is static not allowed.");
            // make sure field is accessible
            f.setAccessible(true);
            // get value
            return (T) f.get(instance);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for object: " + instance);
        }
    }
    
    
}
