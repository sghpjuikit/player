package services;

import java.util.HashMap;
import java.util.Map;
import util.access.V;
import util.conf.CachedConfigurable;
import util.conf.Config;
import util.conf.Configurable;
import util.conf.IsConfig;

/**
 * @author Martin Polakovic
 */
public interface Service extends Configurable {
	void start();

	boolean isRunning();

	void stop();

	default boolean isSupported() { return true; }

	default boolean isDependency() { return false; }

	abstract class ServiceBase implements Service, CachedConfigurable {

		private final HashMap<String,Config<Object>> configs = new HashMap<>();

		@IsConfig(name = "Enabled", info = "Starts or stops the service")
		private final V<Boolean> enabled;

		public ServiceBase(boolean isEnabled) {
			enabled = new V<>(isEnabled, this::enable);
		}

		private void enable(boolean isToBeRunning) {
			boolean isRunning = isRunning();
			if (isRunning==isToBeRunning) return;

			if (isToBeRunning && !isDependency() && isSupported()) start();
			if (!isToBeRunning) stop();
		}

		@Override
		public Map<String,Config<Object>> getFieldsMap() {
			return configs;
		}
	}

}