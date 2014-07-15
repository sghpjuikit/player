
package Configuration;

import Action.Action;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import jdk.nashorn.internal.ir.annotations.Immutable;
import utilities.Parser.Parser;

/**
 * Object representation of a configurable value - most often field annotated 
 * with {@link IsConfig} annotation. 
 * Config wraps a value of that field or the field itself or an object (depending
 * on the implementation) and provides a means to change it. It also provides 
 * access to the value and associated meta information.
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
    
    Field sourceField;
    Method applierMethod;
    public T defaultValue;
    
    Config(String name, String gui_name, T val, String category, String info, boolean editable, boolean visible, double min, double max, Field source_field) {
        this.gui_name = gui_name;
        this.name = name;
        this.defaultValue = objectify(val);
        this.group = category;
        this.info = info;
        this.editable = editable;
        this.visible = visible;
        this.min = min;
        this.max = max;
        this.sourceField = source_field;
    }
    Config(String _name, IsConfig c, T val, String category, Field field) {
        gui_name = c.name().isEmpty() ? _name : c.name();
        name = _name;
        defaultValue = objectify(val);
        group = category;
        info = c.info();
        editable = c.editable();
        visible = c.visible();
        min = c.min();
        max = c.max();
        sourceField = field;
    }
    
    Config(Action c) {
        gui_name = c.name + " Shortcut";
        name = c.name;
        defaultValue = (T)c;
        group = "Shortcuts";
        info = c.info;
        editable = true;
        visible = true;
        min = Double.NaN;
        max = Double.NaN;
    }
    
    Config(Config<T> old, Object T) {
        gui_name = old.gui_name;
        name = old.name;
        defaultValue = old.defaultValue;
        group = old.group;
        info = old.info;
        editable = old.editable;
        visible = old.visible;
        min = old.min;
        max = old.max;
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
     * Returns source class this config originates from.
     * @return 
     */
    Class<T> getSourceClass() {
        return (Class<T>) sourceField.getDeclaringClass();
    }
    
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
     * Equals if and only if non null, is Config type and their name and source
     * field are equal.
     */
    @Override
    public boolean equals(Object o) {
        if(this==o) return true; // this line can make a difference
        
        if (o == null || !(o instanceof Config)) return false;
        
        Config c = (Config)o;
        return getName().equals(c.getName()) & sourceField.equals(c.sourceField); 
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.getName());
        hash = 59 * hash + Objects.hashCode(this.sourceField);
        return hash;
    }
    
    private T objectify(T o) {
        Class<?> clazz = o.getClass();
//        if (boolean.class.equals(clazz)) return new Boolean((boolean)o);
//        else if (float.class.equals(clazz)) return new Float((float)o);
//        else if (int.class.equals(clazz)) return new Integer((int)o);
//        else if (double.class.equals(clazz)) return new Double((double)o);
//        else if (long.class.equals(clazz)) return new Long((long)o);
//        else if (byte.class.equals(clazz)) return new Byte((byte)o);
//        else if (short.class.equals(clazz)) return new Short((short)o);
//        else 
        return (T) o;
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
    
    
    
    public String toS() {
        return Parser.toS(getValue());
    }
    
    public Object fromS(String str) {
        return Parser.fromS(getType(), str);
    }
    
    
}