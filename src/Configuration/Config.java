
package Configuration;

import Action.Action;
import java.util.Objects;
import jdk.nashorn.internal.ir.annotations.Immutable;

/**
 *
 * @author uranium
 */
@Immutable
public class Config {
    public final String gui_name;
    public final String name;
    public final Object value;
    public final Class<?> type;
    public final String group;
    public final String info;
    public final boolean editable;
    public final boolean visible;
    public final double min;
    public final double max;
    
    public Config(String _name, IsConfig c, Object val, String category) {
        gui_name = c.name().isEmpty() ? _name : c.name();
        name = _name;
        value = val;
        type = val.getClass();
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
        type = c.getClass();
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
        value = new_value;
        type = old.type;
        group = old.group;
        info = old.info;
        editable = old.editable;
        visible = old.visible;
        min = old.min;
        max = old.max;
    }
    
    public boolean isMinMax() {
        return !(Double.compare(min, Double.NaN)==0 ||
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
                e &= type.equals(c.type);
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
        hash = 37 * hash + Objects.hashCode(this.type);
        hash = 37 * hash + Objects.hashCode(this.info);
        hash = 37 * hash + (this.editable ? 1 : 0);
        hash = 37 * hash + (this.visible ? 1 : 0);
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.min) ^ (Double.doubleToLongBits(this.min) >>> 32));
        hash = 37 * hash + (int) (Double.doubleToLongBits(this.max) ^ (Double.doubleToLongBits(this.max) >>> 32));
        return hash;
    }
    
}