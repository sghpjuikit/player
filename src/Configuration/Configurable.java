
package Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import utilities.Log;

/**
 * Defines object that can be configured.
 * <p>
 * Configurable object exports its configuration as {@link Config} fields. This can
 * be very used to save or restore a configurable's state (in serialization), 
 * change or manipulate this state or even generate property sheet GUI to change
 * the state of the object.
 * <p>
 * Anything can be configurable (depends on implementation) - an object with 
 * some state (fields) or properties (for example javaFX {@link Property}), or
 * composite object composed of sub Configurables exposing all its aggregated
 * subparts transparently as one or it could even be a virtual object - simply
 * a collection of Configs into a Configurable.
 * <p>
 * This interface already provides default implementations of all its methods.
 * The default implementation makes use of the {@link Configuration.IsConfig}
 * annotation and discovers all fields of this object by reflection.
 * See IsConfig's own documentation to learn more about how to use it.
 * <p>
 * It is possible to use your own implementation. Such would require to override
 * getFields and getField methods.
 * 
 * @author uranium
 */
public interface Configurable {
    
    /** 
     * Get all configs of this configurable.
     * @return Configs of this configurable
     */
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
     * Get config of this configurable with specified name
     * @param name
     * @return config or null if no available. Note: never check for null, rather
     * let NullPointerException be thrown if null is returned. Null always
     * signifies programming error.
     */
    default public Config getField(String name) {
        try {
            Field f = getClass().getField(name);
            IsConfig a = f.getAnnotation(IsConfig.class);
            Object val = f.get(this);
            String group = getClass().getName();
            if(a==null)
                Log.warn("Config '" + name + "' not found. Field exists, but annotation missing");
            return new InstanceFieldConfig(name,a,val,group,this,f);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException ex) {
            System.out.println(ex.getClass());System.out.println(ex.getMessage());
            return null;
        }
    }
    
    /**
     * Convenience method. Equivalent to: return getField(name).setValue(value);
     * <p>
     * Set configurable field of specified name to specified value.
     * @param name
     * @param value 
     * @return true if field has been set, false otherwise
     * @throws NullPointerException if no field available. Note: never catch
     * this exception or check for null. It signifies programming error.
     */
    default public boolean setField(String name, Object value) {
        return getField(name).setValue(value);
    }
    
    /**
     * Convenience method. Equivalent to: return getField(name).setValueFrom(value);
     * <p>
     * Set configurable field of specified name to value specified by String.
     * @param name
     * @param value 
     * @return true if field has been set, false otherwise
     * @throws NullPointerException if no field available. Note: never catch
     * this exception or check for null. It signifies programming error.
     */
    default public boolean setField(String name, String value) {
        return getField(name).setValueFrom(value);
    }
}