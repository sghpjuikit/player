/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Action.Action;
import java.lang.reflect.Field;

/**
 * Instance config. Refers to non static field of object instance.
 * 
 * @author Plutonium_
 */
public class ObjectConfig extends Config {
    
    private Object value;
    
    ObjectConfig(String _name, IsConfig c, Object val, String category, Field field) {
        super(_name, c, val, category, field);
        
        
        value = defaultValue;
    }
    
    ObjectConfig(Action c) {
        super(c);
        value = c;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        return value;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setValue(Object val) {
        if(!value.getClass().equals(val.getClass())) 
            throw new ClassCastException("Can not set value to object config. "
                    + "Parameter class type mismatch.");
        value = val;
        return true;
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
