package util.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collection implementation of {@link Configurable}. Aggregates {@link Config}
 * by name as a map with config name as a key.
 * <p/>
 * This implementation provides O(1) field access.
 * <p/>
 * Use to access configs by name.
 */
public class MapConfigurable<T> implements Configurable<T> {

	Map<String,Config<T>> configs;

	@SuppressWarnings("unchecked")
	@SafeVarargs
	public MapConfigurable(Config<? extends T>... configs) {
		this.configs = new HashMap<>();
		for (Config<? extends T> c : configs) this.configs.put(c.getName(), (Config<T>) c);
	}

	@SuppressWarnings("unchecked")
	public MapConfigurable(List<Config<? extends T>> configs) {
		this.configs = new HashMap<>();
		for (Config<? extends T> c : configs) this.configs.put(c.getName(), (Config<T>) c);
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Implementation details:
	 * <p/>
	 * The configs do not retain their position.
	 */
	@Override
	public List<Config<T>> getFields() {
		return new ArrayList<>(configs.values());
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Implementation details:
	 * Runs in O(1).
	 */
	@Override
	public Config<T> getField(String name) {
		return configs.get(name);
	}

	/**
	 * Add given config to this configurable. For retrieval use config's name.
	 */
	public void addField(Config<T> config) {
		configs.put(config.getName(), config);
	}
}