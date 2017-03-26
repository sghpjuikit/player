package util.plugin;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.type.Util;
import static util.dev.Util.log;

/**
 * <p/>
 *
 * @author Martin Polakovic
 */
public class PluginMap {
	private final Map<Class,List<Class>> m = new HashMap<>();

	public <T> void registerPluginType(Class<T> p) {
		List<Class<?>> superclasses = Util.getSuperClasses(p);
		boolean exists = superclasses.stream().anyMatch(m::containsKey);
		if (exists) throw new IllegalStateException("Super class of " + p + " already registered as plugin type.");

		m.put(p, new ArrayList<>());
	}

	public <T> void registerPlugin(Class<T> p) {
		List<Class<?>> superclasses = Util.getSuperClasses(p);
		if (superclasses.isEmpty())
			throw new IllegalArgumentException("Plugin " + p + " must extend/implement at least one class/interface.");

		superclasses.stream()
				.filter(m::containsKey)
				.map(m::get)
				.forEach(l -> l.add(p));
	}

	public <T> List<T> getPlugins(Class<T> p) {
		@SuppressWarnings("unchecked")
		List<Class<? extends T>> pcs = (List) m.get(p);
		if (pcs==null) throw new IllegalArgumentException(p + " is not a registered plugin type.");
		else {
			List<T> ps = new ArrayList<>();
			pcs.forEach(c -> {
				try {
					ps.add(c.getConstructor().newInstance());
				} catch (NoSuchMethodException|InstantiationException|InvocationTargetException|IllegalAccessException e) {
					Logger.getLogger(PluginMap.class.getName()).log(Level.SEVERE, null, e);
					log(PluginMap.class).error("Could not initialize plugin for class={}", c, e);
				}
			});
			return ps;
		}
	}
}
