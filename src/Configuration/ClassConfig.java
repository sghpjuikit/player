/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.lang.reflect.Field;

/**
 * Class level config. Refers to static field.
 * 
 * @author Plutonium_
 */
public final class ClassConfig extends Config {
    
    ClassConfig(String _name, IsConfig c, Object val, String category, Field field) {
        super(_name, c, val, category, field);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        try {
            return sourceField.get(null);
        } catch (IllegalArgumentException | IllegalAccessException ex) {
            throw new RuntimeException("Field " + getName() + " can not access value.");
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getType() {
        return sourceField.getType();
    }
    
    
}
