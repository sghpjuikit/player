
package Configuration;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import utilities.Util;

/**
 * Defines object that can be configured.
 * <p>
 * Configurable object exports its configurable state as {@link Config} fields that
 * encapsulate the configurable values and abstract away from the implementation.
 * This can be used to save or restore a configurable's state (in serialization), 
 * change or manipulate this state or even generate GUI to change for it.
 * <p>
 * Any object can be configurable - an object with 
 * some state (fields) or properties (for example javaFX {@link Property}), or
 * composite object composed of sub Configurables exposing all its aggregated
 * subparts transparently as one or it could even be a virtual object - simply
 * a collection of unrelated Configs aggregated under a Configurable.
 * <p>
 * This interface already provides complete default implementation. It makes use
 * of the {@link Configuration.IsConfig} annotation and reflection to discover
 * these fields.
 * See {@link IsConfig} own documentation to learn more about how to use it.
 * <p>
 * Note, that every Config is already a Configurable acting as a singleton, it
 * is therefore not required to wrap one config in a configurable, rather, use
 * it directly.
 * <p>
 * It is possible to use your own implementation. It requires to override only
 * getFields and getField methods - the way how the configs are derived).
 * Then one could combine the provided implementation
 * by calling super(), but adding custom Configs or manipulate the result in
 * some way or simply use different way of obtaining the configs.
 * <pre>
 * The following are some possible implementations for a Configurable:
 *    - reflection: default implementation relying on annotation
 *    - collection: impl. relying on collection storing the Configs
 *    - mix: combination of the above
 * </pre>
 * <p>
 * Default implementation has the advantage of not storing the configs in memory.
 * The fields can be accessed individually (unless all are requested) and created
 * temporarily for one-time use.
 * <p>
 * Collection impl. would store the configs in a collection.
 * 
 * @param <T> parameter specifying generic parameter of the Configs that can be
 * contained or obtained from this Configurable. The parameter specifies type
 * of value of the Configs.
 * In most use cases it is not needed or possible to use generic Configurable.
 * This parameter becomes useful for singleton Configurables and Configurables
 * with all Configs of the same type.
 * <p>
 * If all configs of this configurable contain the same type of value,
 * use this generic parameter.
 * 
 * @see MapConfigurable
 * @see ListConfigurable
 * 
 * @author uranium
 */
public interface Configurable<T> {
    
    /** 
     * Get all configs of this configurable.
     * <p>
     * Use to get Configs and access their values. Config can provide
     * its value type, but that is helpful only when using the values dynamically.
     * Because the collection does not know about the type of the Config and its
     * value type - casting is necessary when accessing config's value directly.
     * <p>
     * There are two possible ways:<br>
     * <t>Casting to Config with the correct generic parameter and then calling
     * the getValue() like this:
     * <p>
     * String val = ((Config<String>) c.getFields().get(0)).getValue();
     * <p>
     *     Or obtaining the value and then casting it to the correct type. This 
     * should be the preferred way of doing this. Like this:
     * <p>
     * String val = (String) c.getFields().get(0).getValue();
     * <p>
     * Note: if all configs of this configurable contain the same type of value,
     * use generic configurable to avoid the need to cast.
     * 
     * @return Configs of this configurable
     */
    default public List<Config<T>> getFields() {
        return new ArrayList(Configuration.getConfigsOf(getClass(), this, false, true).values());
    }
    
    /**
     * Get config of this configurable with specified name
     * <p>
     * Because name is a unique identifier, we know the correct value type of
     * the config.
     * <p>
     * Note: if all configs of this configurable contain the same type of value,
     * use generic configurable to avoid the need to cast.
     * 
     * @param name
     * @return config or null if no available. Note: never check for null, rather
     * let NullPointerException be thrown if null is returned. Null always
     * signifies programming error.
     */
    default public Config<T> getField(String name) {
        try {
            Class c = getClass();
            Field f = Util.getField(c,name);
            return Configuration.createConfig(c, f, this, false, true);
        } catch (NoSuchFieldException | SecurityException ex) {
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
    default public boolean setField(String name, T value) {
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