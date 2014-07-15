/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.lang.reflect.Field;
import java.util.Objects;
import javafx.beans.property.Property;

/**
 *
 * @author Plutonium_
 */
public class PropertyConfig<T> extends Config<T> {
    
    Property<T> property;

    PropertyConfig(String _name, IsConfig c, Property<T> val, String category, Field field) {
        super(_name, c, val.getValue(), category, field);
        property = val;
    }

    @Override
    public T getValue() {
        return property.getValue();
    }

    @Override
    public boolean setValue(Object val) {
        property.setValue((T)val);
        return true;
    }
    
    public boolean setValueSafe(T val) {
        property.setValue(val);
        return true;
    }
    
    @Override
    public boolean applyValue() {
        return true;
    }

    @Override
    public Class<T> getType() {
        return (Class<T>)getValue().getClass();
    }

    /**
     * Equals if and only if object instance of PropertyConfig and its property
     * is the same property as property of this: property==o.property;
     * @param o
     * @return 
     */
    @Override
    public boolean equals(Object o) {
        if(o==this) return true;
        return (o instanceof PropertyConfig && property==((PropertyConfig)o).property);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.property);
        return hash;
    }
    
    
}
