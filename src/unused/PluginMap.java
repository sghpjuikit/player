package unused;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.type.Util;

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

	@SuppressWarnings("unchecked")
	public <T> List<T> getPlugins(Class<T> p) {
		List<Class<? extends T>> pcs = (List) m.get(p);
		if (pcs==null) {
			throw new IllegalArgumentException(p + " is not a registered plugin type.");
		} else {
			List<T> ps = new ArrayList<>();
			pcs.forEach(c -> ps.add(instantiate(c)));
			return ps;
		}
	}

	private <T> T instantiate(Class<T> type) {
		boolean isSingleton = isSingleton(type);
		return isSingleton ? instantiateAsSingleton(type) : instantiateWithDefConstructor(type);
	}

	private boolean isSingleton(Class<?> type) {
		String fieldName = "INSTANCE";
		try {
			Field f = type.getDeclaredField(fieldName);
			return Modifier.isStatic(f.getModifiers());
		} catch (NoSuchFieldException e) {
			return false;
		}
	}

	private <T> T instantiateAsSingleton(Class<T> type) {
		String fieldName = "INSTANCE";
		try {
			Field f = type.getDeclaredField(fieldName);
			Class<?> fType = f.getType();

			if (!Modifier.isStatic(f.getModifiers())) throw new NoSuchFieldException(fieldName + " field must be static=" + fType);
			if (fType!=type) throw new NoSuchFieldException(fieldName + " field has wrong type=" + fType);

			try {
				//noinspection unchecked
				return (T) f.get(null);
			} catch (IllegalAccessException e) {
				throw new NoSuchFieldException("Field " + f + " is not accessible");
			}

		} catch(NoSuchFieldException e) {
			throw new IllegalArgumentException("Could not instantiate class=" + type + " as singleton.", e);
		}
	}

	private <T> T instantiateWithDefConstructor(Class<T> type) {
		try {
			return type.getConstructor().newInstance();
		} catch (NoSuchMethodException|InstantiationException|InvocationTargetException|IllegalAccessException e) {
			throw new IllegalStateException("Could not instantiate class=" + type + " using default constructor", e);
		}
	}
}
