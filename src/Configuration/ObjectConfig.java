/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Action.Action;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.lang.reflect.Field;
import utilities.Log;

/**
 * Instance config. Refers to non static field of object instance.
 * 
 * @author Plutonium_
 */
public class ObjectConfig extends Config {
    
    public Object value;
    @XStreamOmitField
    public Object applier_object;
//    @XStreamOmitField
    public Object tmp;
            
    ObjectConfig(String _name, IsConfig c, Object val, String category, Object applier_object, Field field) {
        super(_name, c, val, category, field);
        
        this.applier_object = applier_object;
        value = defaultValue;
        tmp = defaultValue;
    }
    
    ObjectConfig(Action c) {
        super(c);
        value = c;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {System.out.println( " getting value for " + name);
        if(getType().equals(Action.class)) return value;
    
        try {
            Field f = applier_object.getClass().getField(name);
            return f.get(applier_object);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Config " + getName() + 
                        " can not return value. Wrong object type." + e.getMessage());
        } catch (NoSuchFieldException e) {
            // ignore this one
            return false;
        } catch (IllegalAccessException | SecurityException e) {
            throw new RuntimeException("Config " + getName() + " can not return value. " + e.getMessage());
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setValue(Object val) { System.out.println(" setting value for " + name + val + " " + val.getClass() + " " + applier_object);
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

    @Override
    public boolean applyValue() {
        // for now do nothing
        return true;
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return value.getClass();
    }
    
}
