/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Configuration;

import Configuration.Config.ConfigBase;
import java.util.Objects;
import javafx.beans.property.Property;
import javafx.beans.value.WritableValue;
import util.access.ApplicableValue;

/**
 * {@link Config} wrapping a {@link WritableValue}, most often {@link Property}.
 * or {@link util.access.Accessor}
 * <p>
 * Property Config wraps a Property and acts as a Config.
 * The getter and setter directly sets and gets the
 * value from the property as if the PropertyConfig were the Property
 * itself. The result is clean reflection free Config implementation.
 * <p>
 * Note that the wrapped property must be defined as final, otherwise it could
 * lead to problems if the property would be set to different one.
 * <p>
 * Bindable Properties are the most interesting, but if not needed simpler
 * WritableValue object can be used too.
 * 
 * @param <T> generic type of the property.
 * 
 * @author Plutonium_
 */
public class PropertyConfig<T> extends ConfigBase<T> {
    
    WritableValue<T> value;

    /**
     * Constructor to be used with framework
     * @param _name
     * @param c the annotation
     * @param property WritableValue to wrap. Mostly a {@link Property}.
     * @param category
     * @throws IllegalStateException if the property field is not final
     */
    PropertyConfig(String _name, IsConfig c, WritableValue<T> property, String category) {
        super(_name, c, property.getValue(), category);
        value = property;
    }
    /**
     * @param _name
     * @param property WritableValue to wrap. Mostly a {@link Property}.
     * @throws IllegalStateException if the property field is not final
     */
    public PropertyConfig(String name, WritableValue<T> property) {
        this(name, name, property, "", "", true, Double.NaN, Double.NaN);
    }
     /**
     * @param _name
     * @param property WritableValue to wrap. Mostly a {@link Property}.
     * @param info description, for tooltip for example
     * @throws IllegalStateException if the property field is not final
     */
    public PropertyConfig(String name, WritableValue<T> property, String info) {
        this(name, name, property, "", info, true, Double.NaN, Double.NaN);
    }
    /**
     * @param _name
     * @param property WritableValue to wrap. Mostly a {@link Property}.
     * @param category category, for generating config groups
     * @param info description, for tooltip for example
     * @param editable 
     * @param min use in combination with max if value is Number
     * @param max use in combination with min if value is Number
     * @throws IllegalStateException if the property field is not final
     */
    public PropertyConfig(String name, String gui_name, WritableValue<T> property, String category, String info, boolean editable, double min, double max) {
        super(name, gui_name, property.getValue(), category, info, editable, min, max);
        value = property;
    }

    @Override
    public T getValue() {
        return value.getValue();
    }

    @Override
    public void setValue(T val) {
        value.setValue(val);
    }
    
    @Override
    public void applyValue() {
        // apply if value applicable
        if (value instanceof ApplicableValue)
            ApplicableValue.class.cast(value).applyValue();
    }
    
    @Override
    public void applyValue(T val) {
        // apply if value applicable
        if (val instanceof ApplicableValue)
            ApplicableValue.class.cast(val).applyValue();
    }

    @Override
    public Class getType() {
        return getValue().getClass();
    }
    
    public WritableValue<T> getProperty() {
        return value;
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
        return (o instanceof PropertyConfig && value==((PropertyConfig)o).value);
    }

    @Override
    public int hashCode() {
        return 43 * 7 + Objects.hashCode(this.value);
    }
    
    
}
