/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
public final class StaticFieldConfig<T> extends Config<T> {
    
    @XStreamOmitField
    private final Field sourceField;
    
    /**
     * 
     * @param _name
     * @param c
     * @param val
     * @param category
     * @param field static field to wrap
     * 
     * @throws NullPointerException if field value is null. The wrapped value must no be
     * null.
     * @throws IllegalStateException if field is not static. Field must be static.
     */
    StaticFieldConfig(String _name, IsConfig c, String category, Field field) {
        super(_name, c, getValueFromField(field), category);
        sourceField = field;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue() {
        try {
            return (T) sourceField.get(null);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Field " + getName() + " can not access value.");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setValue(T val) {
        try {
            sourceField.set(null, val);
            Log.deb("Config field " + name + " set.");
            return true;
        } catch (IllegalAccessException e) {
            Log.err("Failed to set config field: " + name + " . Reason: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applyValue() {
        if(applierMethod != null) {
            Log.deb("Applying config: " + name);
            try {
                applierMethod.setAccessible(true);
                applierMethod.invoke(null, new Object[0]);
                return true;
            } catch (IllegalAccessException | IllegalArgumentException | 
                    InvocationTargetException | SecurityException e) {
                Log.err("Failed to apply config field: " + name + ". Reason: " + e.getMessage());
                return false;
            } finally {
                applierMethod.setAccessible(false);
            }
        } else {
            Log.deb("Ommitting to apply config field: " + name + ". Reason: No applier method.");
            return true;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return (Class<T>) sourceField.getType();
    }
    
    /** 
     * Equals if and only if non null, is Config type and source field is equal.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if (o == null || !(o instanceof StaticFieldConfig)) return false;
        
        StaticFieldConfig c = (StaticFieldConfig)o;
        return sourceField.equals(c.sourceField); 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.sourceField);
        return hash;
    }
    
    
/******************************************************************************/
    
    // helper method to obtain initial value while enforcing static field check
    // use in super() constructor
    private static<T> T getValueFromField(Field f) {
        try {
            if(!Modifier.isStatic(f.getModifiers()))
                throw new IllegalStateException("Property config must be final.");
            f.setAccessible(true);
            return (T) f.get(null);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Can not access field: " + f.getName() + " for class: " + f.getDeclaringClass());
        } finally {
            f.setAccessible(false);
        }
    }
    
    
}
