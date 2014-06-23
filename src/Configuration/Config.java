
package Configuration;

import Action.Action;
import java.util.Objects;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 * A read only object representation of a field annotated with {@link IsConfig}
 * annotation. This object wraps a value of that field and contains information
 * derived from the field so it can be provided to various parts of application.
 * 
 * @author uranium
 */
@Immutable
public class Config {
    
    /** 
     * Alternative name of this config. Intended to be human readable and
     * appropriately formated. 
     * <p>
     * Default value is set to be equivalent to name, but can be specified to
     * differ. Always use for gui building instead of {@link #name}.
     */
    public final String gui_name;
    /** Name of this config. */
    public final String name;
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
    public final Object value;
    /** 
     * Category or group this config belongs to. Use arbitrarily to group
     * multiple configs together - mostly semantically or by intention.
     */
    public final String group;
    /** Description of this config */
    public final String info;
    /** 
     * Indicates editability. Use arbitrarily. Most often sets whether this
     * config should be editable by user via graphical user interface.
     */
    public final boolean editable;
    /** 
     * Indicates visibility. Use arbitrarily. Most often sets whether this
     * config should be displayed in the graphical user interface.
     */
    public final boolean visible;
    /** Minimum allowable value. Applicable only for numbers. In double. */
    public final double min;
    /** Maximum allowable value. Applicable only for numbers. In double. */
    public final double max;
    
    
    public Config(String _name, IsConfig c, Object val, String category) {
        gui_name = c.name().isEmpty() ? _name : c.name();
        name = _name;
        value = objectify(val);
        group = category;
        info = c.info();
        editable = c.editable();
        visible = c.visible();
        min = c.min();
        max = c.max();
    }
    public Config(Action c) {
        gui_name = c.name + " Shortcut";
        name = c.name;
        value = c;
        group = "Shortcuts";
        info = c.info;
        editable = true;
        visible = true;
        min = Double.NaN;
        max = Double.NaN;
    }
    
    public Config(Config old, Object new_value) {
        gui_name = old.gui_name;
        name = old.name;
        value = objectify(new_value);
        group = old.group;
        info = old.info;
        editable = old.editable;
        visible = old.visible;
        min = old.min;
        max = old.max;
    }
    
    /**
     * Use to determine whether min and max fields dont dontain illegal value.
     * If they dont, they can be used to query minimal and maximal number value.
     * Otherwise Double not a number is returned and should not be used.
     * @return true if and only if value is a number and both min and max value 
     * are specified. 
     */
    public boolean isMinMax() {
        return value instanceof Number &&
                !(Double.compare(min, Double.NaN)==0 ||
                    Double.compare(max, Double.NaN)==0);
    }
    
    /** Equals if and only if non null, is Config type and all fields equal */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Config))
            return false;
        
        Config c = (Config)obj;
        
        boolean e = true;
                e &= gui_name.equals(c.gui_name);
                e &= name.equals(c.name);
                e &= value.equals(c.value);
                e &= info.equals(c.info);
                e &= (editable == c.editable);
                e &= (visible == c.visible);
                e &= (Double.compare(min, c.min)==0);
                e &= (Double.compare(max, c.max)==0); 
        return  e;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 37 * hash + Objects.hashCode(this.gui_name);
        hash = 37 * hash + Objects.hashCode(this.name);
        hash = 37 * hash + Objects.hashCode(this.value);
        hash = 37 * hash + Objects.hashCode(this.info);
        hash = 37 * hash + (this.editable ? 1 : 0);
        hash = 37 * hash + (this.visible ? 1 : 0);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.min) ^ (Double.doubleToLongBits(this.min) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.max) ^ (Double.doubleToLongBits(this.max) >>> 32));
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
    
}