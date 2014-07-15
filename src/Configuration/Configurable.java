
package Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import utilities.Log;
import utilities.Parser.Parser;

/**
 * Denotes object that can be configured.
 * Configurable object exports its configuration as {@link Config} fields. This can
 * be very useful example to save a configurable's 'state' or restore it back 
 * as a means to serialize complicated or composite objects. Also Configurable
 * can be used to generate property sheet GUI to set change the state of the object.
 * <p>
 * The object denoted by a Configurable can be simple objects with some properties,
 * composite object composed of sub Configurables exposing all its aggregated
 * subparts transparently as one or it could even be a virtual object - simply
 * a collection of objects and values wrapped into a Configurable.
 * <p>
 * This interface already provides default implementations of all its methods.
 * Implementing classes therefore get all the behavior with no additional work.
 * This is because the interface uses default methods and reflection to introspect this
 * object's class and configurations.
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
                    fields.add(new InstanceFieldConfig(f.getName(),c, f.get(this), getClass().getSimpleName(), this, f));
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
                  Log.deb("Config field: " + name + " set to: " + value);
            return true;
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
            if(!name.equals("preferred"))
            Log.err("Config field: " + name + " failed to set. Reason: " + ex.getMessage());
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
                  Log.deb("Config field: " + name + " set to: " + value);
            return true;
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
            if(!name.equals("preferred"))
            Log.err("Config field: " + name + " failed to set. Reason: " + ex.getMessage());
            return false;
        }
    }
}
