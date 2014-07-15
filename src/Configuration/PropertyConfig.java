/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import java.util.Objects;
import javafx.beans.property.Property;

/**
 * {@Config} wrapping a {@Property}.
 * <p>
 * Property Config wraps a Property and acts as a Config, but as opposed to
 * other Config implementations, its getter and setter directly sets and gets the
 * value from the property acting as if the PropertyConfig were the Property
 * itself. The result is clean reflection free Config implementation.
 * <p>
 * Note that the wrapped property should be defined as final or be effectively
 * final, otherwise it could be changed to different one and this config would 
 * no longer be accessing the same property, leading to bugs.
 *
 * @param <T> generic type of the property.
 * 
 * @author Plutonium_
 */
public class PropertyConfig<T> extends Config<T> {
    
    Property<T> property;

    /**
     * 
     * @param _name
     * @param c
     * @param property Property to wrap. 
     * @param category
     * @param field Field of the property to wrap.
     * @throws IllegalStateException if the property field is not final
     */
    PropertyConfig(String _name, IsConfig c, Property<T> property, String category) {
        super(_name, c, property.getValue(), category);
        this.property = property;
    }
    
    public PropertyConfig(String name, Property<T> property) {
        this(name, name, property, "", "", true, true, Double.NaN, Double.NaN);
    }
    
    public PropertyConfig(String name, Property<T> property, String info) {
        this(name, name, property, "", info, true, true, Double.NaN, Double.NaN);
    }
    
    public PropertyConfig(String name, String gui_name, Property<T> property, String category, String info, boolean editable, boolean visible, double min, double max) {
        super(name, gui_name, property.getValue(), category, info, editable, visible, min, max);
        this.property = property;
    }

    @Override
    public T getValue() {
        return property.getValue();
    }

    @Override
    public boolean setValue(T val) {
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
