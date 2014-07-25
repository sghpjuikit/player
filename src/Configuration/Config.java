
package Configuration;

import java.lang.reflect.Method;
import java.util.Objects;
import jdk.nashorn.internal.ir.annotations.Immutable;
import utilities.Parser.Parser;

/**
 * Object representation of a configurable value - possibly field annotated 
 * with {@link IsConfig} annotation. 
 * Config wraps a value of that field or the field itself or an object (depending
 * on the implementation) and provides a means to set or get the object or its
 * value and also associated meta information.
 * <p>
 * Useful for creating {@link Configurable} objects or exporting values in a
 * standardized way.
 * 
 * @author uranium
 */
@Immutable
public abstract class Config<T> {
    
    final protected String gui_name;
    final protected String name;
    final protected String group;
    final protected String info;
    final protected boolean editable;
    final protected boolean visible;
    final protected double min;
    final protected double max;
    
    Method applierMethod;
    public T defaultValue;
    
    /**
     * 
     * @param name
     * @param gui_name
     * @param val
     * @param category
     * @param info
     * @param editable
     * @param visible
     * @param min
     * @param max 
     * 
     * @throws NullPointerException if val parameter null. The wrapped value must
     * no be null.
     */
    Config(String name, String gui_name, T val, String category, String info, boolean editable, boolean visible, double min, double max) {
        Objects.requireNonNull(val);
        this.gui_name = gui_name;
        this.name = name;
        this.defaultValue = val;
        this.group = category;
        this.info = info;
        this.editable = editable;
        this.visible = visible;
        this.min = min;
        this.max = max;
    }
    
    /**
     * 
     * @param _name
     * @param c
     * @param val
     * @param category 
     * 
     * @throws NullPointerException if val parameter null. The wrapped value must
     * no be null.
     */
    Config(String _name, IsConfig c, T val, String category) {
        Objects.requireNonNull(val);
        gui_name = c.name().isEmpty() ? _name : c.name();
        name = _name;
        defaultValue = val;
        group = category;
        info = c.info();
        editable = c.editable();
        visible = c.visible();
        min = c.min();
        max = c.max();
    }
    
    /**
     * Value wrapped in this config. Always {@link Object}. Primitives are
     * wrapped automatically.
     * <p>
     * The inspection of the value's class might be used. In such case checking
     * for primitives is unnecessary.
     * <pre>
     * To check for type use:
     *     value instanceof SomeClass.class
     * or
     *     value instanceof SomeClass
     *
     * For enumerations use:
     *     value instance of Enum
     * </pre>
     */
    public abstract T getValue();
    
    /**
     * Get default value for this config. It is the first value this config
     * contained.
     * @return default value. Never null.
     */
    public T getDefaultValue() {
        return defaultValue;
    }
    
    public abstract boolean setValue(T val);
    
    public abstract boolean applyValue();
    
    public void setNapplyValue(T val) {
        // set new config value
        boolean was_set = setValue(val);
        // apply new field value on success
        if(was_set) applyValue();
    }
    
    /**
     * Returns class type of the value. The value and default value can only
     * be safely cast into the this class.
     * <p>
     * Semantically equivalent to getValue().getClass() but will never fail to
     * return proper class even if the value is null.
     */
    public abstract Class<T> getType();
    
    /**
     * Use to determine whether min and max fields dont dontain illegal value.
     * If they dont, they can be used to query minimal and maximal number value.
     * Otherwise Double not a number is returned and should not be used.
     * @return true if and only if value is a number and both min and max value 
     * are specified. 
     */
    public boolean isMinMax() {
        return getValue() instanceof Number &&
                !(Double.compare(min, Double.NaN)==0 ||
                    Double.compare(max, Double.NaN)==0);
    }

    /**
     * Alternative name of this config. Intended to be human readable and
     * appropriately formated. 
     * <p>
     * Default value is set to be equivalent to name, but can be specified to
     * differ. Always use for gui building instead of {@link #getName()}.
     */
    public String getGuiName() {
        return gui_name;
    }

    /**
     * Name of this config.
     */
    public String getName() {
        return name;
    }

    /**
     * Category or group this config belongs to. Use arbitrarily to group
     * multiple configs together - mostly semantically or by intention.
     */
    public String getGroup() {
        return group;
    }

    /**
     * Description of this config
     */
    public String getInfo() {
        return info;
    }

    /**
     * Indicates editability. Use arbitrarily. Most often sets whether this
     * config should be editable by user via graphical user interface.
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * Indicates visibility. Use arbitrarily. Most often sets whether this
     * config should be displayed in the graphical user interface.
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * Minimum allowable value. Applicable only for numbers. In double.
     */
    public double getMin() {
        return min;
    }

    /**
     * Maximum allowable value. Applicable only for numbers. In double.
     */
    public double getMax() {
        return max;
    }
    
    
    /**
     * Converts the value to String utilizing generic {@link Parser}.
     * Use for serialization or filling out guis.
     */
    public String toS() {
        return Parser.toS(getValue());
    }
    
    /**
     * Converts the string to a valid value for this config utilizing generic 
     * {@link Parser}.
     * Use for serialization or filling out guis.
     * <p>
     * Note: the value of this config is not changed.
     * @see #setValueFrom(java.lang.String)
     */
    public T fromS(String str) {
        return (T) Parser.fromS(getType(), str);
    }
    
    /**
     * Sets value converted from string.
     * Equivalent to: return setValue(fromS(str));
     * @param str
     * @return 
     */
    public boolean setValueFrom(String str) {
        return setValue(fromS(str));
    }
    
}