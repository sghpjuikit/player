
package Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import utilities.Log;
import utilities.Parser.Parser;

/**
 * Denotes object that can be configured.
 * Configurable object exports its configuration (which are in fact simple
 * annotated fields).
 * All configurable fields can be exported as a list of {@link Config}. This can
 * be very useful for serialization for example to save a configurable 'state'
 * of the object.
 * <p>
 * This interface already provides default implementations of all its methods.
 * Implementing classes therefore get all the behavior with no additional work.
 * There is no additional work to do other than implementing this interface.
 * This is because default implementation uses reflection to introspect this
 * object's configurations.
 * <p>
 * The default implementation makes use of the {@link Configuration.IsConfig}
 * annotation. Annotating a field will make it compatible with default behavior
 * of this interface and is necessary and only requirement.
 * <pre>
 * It is required for a field to not be final. Final field will be ignored and
 * can not have its value set (practically read-only configuration field).
 * 
 * @author uranium
 */
public interface Configurable {
    
    /** @return Config Fields of this object */
    default public List<Config> getFields() {
        List<Config> fields = new ArrayList();
        for (Field f: getClass().getFields()) {
            try {
                IsConfig c = f.getAnnotation(IsConfig.class);
                if (c != null)
                    fields.add(new ObjectConfig(f.getName(),c, f.get(this), getClass().getSimpleName(), f));
            } catch (IllegalAccessException ex) {
                Log.err(ex.getMessage());
            }
        }
        return fields;
    }
    
    /**
     * Set configurable field of specified name to specified value.
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
            Log.err(ex.getMessage());
            return false;
        }
    }
    
    /**
     * Set configurable field of specified name to value specified by String. This method
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
            Log.err(ex.getMessage());
            return false;
        }
    }
}
