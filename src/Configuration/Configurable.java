
package Configuration;

import static Configuration.Configuration.configsOf;
import java.lang.reflect.Field;
import java.util.Collection;
import static java.util.stream.Collectors.toList;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.WritableValue;
import util.Util;
import static util.functional.Util.forEachIStream;
import static util.functional.Util.listM;

/**
 * Defines object that can be configured.
 * <p>
 * Configurable object exports its configurable state as {@link Config} fields that
 * encapsulate the configurable values.
 * This can be used to save, restore, serialization or manipulate this state.
 * <p>
 * Any object can be configurable - an object with 
 * some state (fields) or properties {@link Property}, composite object composed
 * of sub Configurables exposing all its aggregated
 * subparts as one or it could even be a simple collection of unrelated Configs.
 * <p>
 * This interface already provides complete default implementation capable of
 * discovering fields with {@link Configuration.IsConfig} annotation.
 * <p>
 * Note, that every Config is already a singleton Configurable.
 * <p>
 * It is possible to use your own implementation. It requires to override only
 * getFields and getField methods - the way how the configs are derived).
 * Then one could combine the provided implementation
 * by calling super(), adding custom Configs or manipulate them, etc.
 * <p>
 * This class provides static utility methods for basic implementations.
 * <pre>
 * The following are some possible implementations for a Configurable:
 *    - reflection: default implementation relying on annotation
 *    - collection: impl. relying on collection storing the Configs as properties
 *    - mix: combination of the above
 * </pre>
 * <p>
 * Default implementation has the advantage of not storing the configs in memory.
 * The fields can be accessed individually (unless all are requested) and created
 * temporarily for one-time use.

 * @param <T> parameter specifying generic parameter of the Config (specifying
 * config's value type) that can be contained or obtained from this Configurable.
 * In most use cases it raw Configurable should be used.
 * This parameter only is useful for singleton Configurable and Configurable
 * with all Configs of the same type, avoiding the need to cast.
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
     * Configs know the type of its value, but that is lost since configs with
     * different values can form the state of this configurable. Casting is 
     * necessary when accessing configs' value directly.
     * <p>
     * There are three possible resolutions:<br>
     * <p>
     * 1:    Hold the reference to needed config if this configurable was constructed
     * manually.
       <p>
     * 2:   Casting to Config with the correct generic parameter and then calling
     * the getValue() :
       <p>
     * {@code String val = ((Config<String>) c.getFields().get(0)).getValue()}
       <p>
     * 3:   Or obtaining the value and then casting it to the correct type. This 
     * should be the preferred way of doing this. Like this:
       <p>
     * {@code String val = (String) c.getFields().get(0).getValue()}
       <p>
     * Note: if all configs of this configurable contain the same type of value,
     * use generic configurable to get config field with proper generic type.
     * 
     * @return Configs of this configurable
     */
    default public Collection<Config<T>> getFields() {
        return (Collection) configsOf(getClass(), this, false, true).values();
    }
    
    /**
     * Get config of this configurable for field with provided name.
     * <p>
     * Note: if all configs of this configurable contain the same type of value,
     * use generic configurable to get config field with proper generic type.
     * <p>
     * Note: implementation must not throw exception, but return null on error.
     * @param n unique name of the field
     * @return config with given name or null if does not exist.
     */
    default public Config<T> getField(String n) {
        try {
            Class c = this.getClass();
            Field f = Util.getField(c,n);
            return Configuration.createConfig(c, f, this, false, true);
        } catch (NoSuchFieldException | SecurityException ex) {
            return null;
        }
    }
    
    /**
     * Get config of this configurable for field with provided name or dummy
     * config wrapping the provided value if not found.
     * <p>
     * Note: if all configs of this configurable contain the same type of value,
     * use generic configurable to get config field with proper generic type.
     * 
     * @param name unique name of the field
     * @param dv default value to wrap in case the config does not exist
     * @return config with given name, never null
     */
    default public Config<T> getFieldOr(String name, T dv) {
        Config<T> c = getField(name);
        return c==null ? new ValueConfig(name,dv) : c;
    }
    
    /**
     * Safe set method.
     * Sets value of config with given name if it exists.
     * Non null equivalent to: return getField(name).setValue(value);
     * <p>
     * Use when input isnt guaranteed to be valid, e.g. contents of a file.
    
     * @param n unique name of the field
     * @param v value
     */
    default public void setField(String n, T v) {
        Config<T> c = getField(n);
        if(c!=null) c.setValue(v);
    }
    
    /**
     * Unsafe set method.
     * Sets value of config with given name if it exists or throws an exceptioon.
     * Equivalent to: return getField(name).setValue(value);
     * <p>
     * Use when input is guaranteed to be valid, e.g. using valid value in source code.
    
     * @param n unique name of the field
     * @param v value
     */
    default public void setFieldOrThrow(String n, T v) {
        Config<T> c = getField(n);
        if(c!=null) c.setValue(v);
        else throw new IllegalArgumentException("Config field '" + n + "' not found.");
    }
    
    /**
     * Safe set method.
     * Sets value of config with given name if it exists, parsing the text input.
     * <p>
     * No exception equivalent to: return getField(name).setValueS(value);
     * Use when deserializing.
    
     * @param n unique name of the field
     * @param v text value to be parsed 
     */
    default public void setField(String n, String v) {
        Config<T> c = getField(n);
        if(c!=null) c.setValueS(v);
    }
    
    
    
    
    public static Collection<Config> configsFromValues(Collection<WritableValue> vals) {
        return forEachIStream(vals, (i,v) -> new PropertyConfig(String.valueOf(i),v)).collect(toList());
    }
    
    public static <E extends ReadOnlyProperty & WritableValue> Collection<Config> configsFromProperties(Collection<E> vals) {
        return listM(vals,v -> new PropertyConfig(v.getName(),v));
    }
    
    public static <E extends ReadOnlyProperty & WritableValue> Collection<Config> configsFromProperties(E... vals) {
        return listM(vals,v -> new PropertyConfig(v.getName(),v));
    }
    
    public static <E extends ReadOnlyProperty & WritableValue> Collection<Config> configsFromFieldsOf(Object o) {
        return configsOf(o.getClass(), o, false, true).values();
    }
}