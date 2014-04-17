
package Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import utilities.Log;
import utilities.Parser.Parser;

/**
 *
 * @author uranium
 */
public interface Configurable {
    
    /** @return Config Fields of this object */
    default public List<Config> getFields() {
        List<Config> fields = new ArrayList<>();
        for (Field f: getClass().getFields()) {
            try {
                IsConfig c = f.getAnnotation(IsConfig.class);
                if (c != null)
                    fields.add(new Config(f.getName(),c, f.get(this), getClass().getSimpleName()));
            } catch (IllegalAccessException ex) {
                Log.err(ex.getMessage());
            }
        }
        return fields;
    }
    
    /**
     * Set config field of specified name to specified value.
     * @param name
     * @param value 
     * @return true if field has been set, false otherwise
     */
    default public boolean setField(String name, Object value) {
        try {
            Field f = getClass().getField(name);
                  f.set(this, value);
            return true;
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
            return false;
        }
    }
    
    /**
     * Set config field of specified name to value specified by String. This method
     * only works for fields of type that can be parsed from String.
     * @param name
     * @param value 
     * @return true if field has been set, false otherwise
     */
    default public boolean setField(String name, String value) {
        try {
            Field f = getClass().getField(name);
                  f.set(this, Parser.fromS(f.getType(), value));
            return true;
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
            return false;
        }
    }
}
