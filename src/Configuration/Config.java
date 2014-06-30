
package Configuration;

import Action.Action;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * Object representation of a field annotated with {@link IsConfig}
 * annotation. This object wraps a value of that field or the field itself and 
 * provides access to it and its meta information so it can be provided to various
 * parts of application.
 * 
 * @author uranium
 */
@Immutable
public abstract class Config {
    
    final private String gui_name;
    final private String name;
    final private String group;
    final private String info;
    final private boolean editable;
    final private boolean visible;
    final private double min;
    final private double max;
    
    Field sourceField;
    Method applierMethod;
    public Object defaultValue;
    
    
    Config(String _name, IsConfig c, Object val, String category, Field field) {
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
        defaultValue = c;
        group = "Shortcuts";
        info = c.info;
        editable = true;
        visible = true;
        min = Double.NaN;
        max = Double.NaN;
    }
    
    Config(Config old, Object new_value) {
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
    public abstract Object getValue();
    
    /**
     * Returns class type of the value. The value and default value can only
     * be safely cast into the this class.
     * <p>
     * Semantically equivalent to getValue().getClass() but will never fail to
     * return proper class even if the value is null.
     */
    public abstract Class<?> getType();
    
    /**
     * Returns source class this config originates from.
     * @return 
     */
    Class<?> getSourceClass() {
        return sourceField.getDeclaringClass();
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
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Config)) return false;
        
        Config c = (Config)obj;
        return getName().equals(c.getName()) & sourceField.equals(c.sourceField); 
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + Objects.hashCode(this.getName());
        hash = 59 * hash + Objects.hashCode(this.sourceField);
        return hash;
    }
    
    private Object objectify(Object o) {
        Class<?> clazz = o.getClass();
        if (boolean.class.equals(clazz)) return new Boolean((boolean)o);
        else if (float.class.equals(clazz)) return new Float((float)o);
        else if (int.class.equals(clazz)) return new Integer((int)o);
        else if (double.class.equals(clazz)) return new Double((double)o);
        else if (long.class.equals(clazz)) return new Long((long)o);
        else if (byte.class.equals(clazz)) return new Byte((byte)o);
        else if (short.class.equals(clazz)) return new Short((short)o);
        else return o;
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
    
    
}