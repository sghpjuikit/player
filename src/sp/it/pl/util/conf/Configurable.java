package sp.it.pl.util.conf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import sp.it.pl.util.type.Util;
import static sp.it.pl.util.conf.ConfigurationUtil.configsOf;
import static sp.it.pl.util.conf.ConfigurationUtil.createConfig;
import static sp.it.pl.util.functional.Util.list;
import static sp.it.pl.util.type.Util.forEachJavaFXProperty;

/**
 * Defines object that can be configured.
 * <p/>
 * Configurable object exports its configurable state as {@link sp.it.pl.util.conf.Config} fields that
 * encapsulate the configurable values.
 * This can be used to save, restore, serialization or manipulate this state.
 * <p/>
 * Any object can be configurable - an object with
 * some state (fields) or properties {@link javafx.beans.property.Property}, composite object composed
 * of sub Configurables exposing all its aggregated
 * subparts as one or it could even be a simple collection of unrelated Configs.
 * <p/>
 * This interface already provides complete default implementation capable of
 * discovering fields with {@link sp.it.pl.util.conf.IsConfig} annotation.
 * <p/>
 * Note, that every Config is already a singleton Configurable.
 * <p/>
 * It is possible to use your own implementation. It requires to override only
 * getFields and getField methods - the way how the configs are derived).
 * Then one could combine the provided implementation
 * by calling super(), adding custom Configs or manipulate them, etc.
 * <p/>
 * This class provides static utility methods for basic implementations.
 * <pre>
 * The following are some possible implementations for a Configurable:
 *    - reflection: default implementation relying on annotation
 *    - collection: impl. relying on collection storing the Configs as properties
 *    - mix: combination of the above
 * </pre>
 * <p/>
 * Default implementation has the advantage of not storing the configs in memory.
 * The fields can be accessed individually (unless all are requested) and created
 * temporarily for one-time use.
 *
 * @param <T> parameter specifying generic parameter of the Config (specifying config's value type) that can be
 * contained or obtained from this Configurable. In most use cases it raw Configurable should be used. This parameter
 * only is useful for singleton Configurable and Configurable with all Configs of the same type, avoiding the need to
 * cast.
 * <p/>
 * If all configs of this configurable contain the same type of value, use this generic parameter.
 *
 * @see MapConfigurable
 * @see ListConfigurable
 */
public interface Configurable<T> {

	/**
	 * Configurable with no fields.
	 */
	Configurable EMPTY_CONFIGURABLE = new Configurable() {
		@Override
		public Collection<Config> getFields() { return list(); }

		@Override
		public Config getField(String name) { return null; }

		@Override
		public Config getFieldOrThrow(String name) {
			throw new IllegalArgumentException("Config field '" + name + "' not found.");
		}

		@Override
		public void setField(String name, Object v) {
			setFieldOrThrow(name, v);
		}

		@Override
		public void setFieldOrThrow(String name, Object v) {
			throw new IllegalArgumentException("Config field '" + name + "' not found.");
		}
	};

	/**
	 * Get all configs of this configurable.
	 * <p/>
	 * Configs know the type of its value, but that is lost since configs with
	 * different values can form the state of this configurable. Casting is
	 * necessary when accessing configs' value directly.
	 * <p/>
	 * There are three possible resolutions:<br>
	 * <p/>
	 * 1:    Hold the reference to needed config if this configurable was constructed
	 * manually.
	 * <p/>
	 * 2:   Casting to Config with the correct generic parameter and then calling
	 * the getValue() :
	 * <p/>
	 * {@code String val = ((Config<String>) c.getFields().get(0)).getValue()}
	 * <p/>
	 * 3:   Or obtaining the value and then casting it to the correct type. This
	 * should be the preferred way of doing this. Like this:
	 * <p/>
	 * {@code String val = (String) c.getFields().get(0).getValue()}
	 * <p/>
	 * Note: if all configs of this configurable contain the same type of value,
	 * use generic configurable to get config field with proper generic type.
	 *
	 * @return Configs of this configurable
	 */
	@SuppressWarnings("unchecked")
	default Collection<Config<T>> getFields() {
		return (Collection) configsOf(getClass(), this, false, true);
	}

	/**
	 * Get config of this configurable with provided name. Null if not found.
	 * <p/>
	 * Note: if all configs of this configurable contain the same type of value,
	 * use generic configurable to get config field with proper generic type.
	 *
	 * @param name unique name of the field
	 * @return config with given name or null if does not exist.
	 */
	default Config<T> getField(String name) {
		try {
			return getFieldOrThrow(name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Get config of this configurable with provided name. Throws {@link IllegalArgumentException} if not found.
	 * <p/>
	 * Note: if all configs of this configurable contain the same type of value,
	 * use generic configurable to get config field with proper generic type.
	 *
	 * @param name unique name of the field
	 * @return config with given name or null if does not exist.
	 */
	@SuppressWarnings("unchecked")
	default Config<T> getFieldOrThrow(String name) {
		try {
			Class<?> c = this.getClass();
			Field f = Util.getField(c, name);
			return (Config<T>) createConfig(c, f, this, false, true);
		} catch (NoSuchFieldException|SecurityException e) {
			throw new IllegalArgumentException("Config field '" + name + "' not found.");
		}
	}

	/**
	 * Safe set method.
	 * Sets value of config with given name if it exists.
	 * Non null equivalent to: return getField(name).setValue(value);
	 * <p/>
	 * Use when input is not guaranteed to be valid, e.g. contents of a file.
	 *
	 * @param name unique name of the field
	 * @param v value
	 */
	default void setField(String name, T v) {
		Config<T> c = getField(name);
		if (c!=null) c.setValue(v);
	}

	/**
	 * Unsafe set method.
	 * Sets value of config with given name if it exists or throws an exceptioon.
	 * Equivalent to: return getField(name).setValue(value);
	 * <p/>
	 * Use when input is guaranteed to be valid, e.g. using valid value in source code.
	 *
	 * @param name unique name of the field
	 * @param v value
	 */
	default void setFieldOrThrow(String name, T v) {
		Config<T> c = getField(name);
		if (c!=null) c.setValue(v);
		else throw new IllegalArgumentException("Config field '" + name + "' not found.");
	}

	/**
	 * Safe set method.
	 * Sets value of config with given name if it exists, parsing the text input.
	 * <p/>
	 * Exception-free equivalent to: return getField(name).setValueS(value);
	 * Use when deserializing.
	 *
	 * @param name unique name of the field
	 * @param v text value to be parsed
	 */
	default void setField(String name, String v) {
		Config<T> c = getField(name);
		if (c!=null) c.setValueS(v);
	}

	static Collection<Config<Object>> configsFromFieldsOf(Object o) {
		return configsOf(o.getClass(), o, false, true);
	}

	static Collection<Config<Object>> configsFromFieldsOf(String fieldNamePrefix, String category, Object o) {
		return configsOf(o.getClass(), fieldNamePrefix, category, o, false, true);
	}

	@SuppressWarnings("unchecked")
	static Configurable<?> configsFromFxPropertiesOf(Object o) {
		List<Config<?>> cs = new ArrayList<>();
		forEachJavaFXProperty(o, (p, name, type) -> cs.add(Config.forProperty(type, name, p)));
		return new ListConfigurable(cs);
	}
}