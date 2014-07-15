/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import utilities.Log;

/**
 * Class level config. Refers to static field.
 * 
 * @author Plutonium_
 */
public final class ClassConfig<T> extends Config<T> {
    
    ClassConfig(String _name, IsConfig c, T val, String category, Field field) {
        super(_name, c, val, category, field);
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
    
    
}
