/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Action.Action;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.lang.reflect.Field;
import java.util.Objects;
import utilities.Log;

/**
 * Object instance level {@link Config}.
 * <p>
 * Wraps non-static {@link Field}.
 * <p>
 * Use for object instance level configurations. See {#link Configurable}
 * 
 * @author Plutonium_
 */
public class InstanceFieldConfig<T> extends Config<T> {
    
    public T value;
    @XStreamOmitField
    private final Field sourceField;
    @XStreamOmitField
    public Object applier_object;
            
    InstanceFieldConfig(String _name, IsConfig c, T val, String category, Object applier_object, Field field) {
        super(_name, c, val, category);
        
        this.applier_object = applier_object;
        this.sourceField = field;
        value = defaultValue;
    }
    
    InstanceFieldConfig(Action c) {
        super(c);
        sourceField = null;
        value = (T)c;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public T getValue() {System.out.println( " getting value for " + name);
        if(getType().equals(Action.class)) return value;
    
        try {
            Field f = applier_object.getClass().getField(name);
            return (T) f.get(applier_object);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Config " + getName() + 
                        " can not return value. Wrong object type." + e.getMessage());
        } catch (NoSuchFieldException | IllegalAccessException | SecurityException e) {
            throw new RuntimeException("Config " + getName() + " can not return value. " + e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setValue(T val) { System.out.println(" setting value for " + name + val + " " + val.getClass() + " " + applier_object);
        if(getType().equals(Action.class)) {
            value = val;
            return true;
        }
        try {
            Field f = applier_object.getClass().getField(name);
                  f.set(applier_object, val);
                  Log.deb("Config field: " + name + " set to: " + val);
            return true;
        } catch (SecurityException | IllegalAccessException ex) {
            Log.err("Config field: " + name + " failed to set. Reason: " + ex.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Config value wrong object type. Cant "
                    + "set " + getName() + " to: " + val + ".");
        } catch (NoSuchFieldException e) {
            // ignore this one
            return false;
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean applyValue() {
        // for now do nothing
        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<T> getType() {
        return (Class<T>) value.getClass();
    }
    
    /** 
     * Equals if and only if non null, is Config type and source field is equal.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if (o == null || !(o instanceof InstanceFieldConfig)) return false;
        
        InstanceFieldConfig c = (InstanceFieldConfig)o;
        return sourceField.equals(c.sourceField); 
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.sourceField);
        return hash;
    }
    
}
