package sp.it.pl.util.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import sp.it.pl.util.collections.map.ClassMap;
import sp.it.pl.util.functional.Functors.Ƒ1;
import static java.util.Collections.emptyMap;

public class InstanceInfo {

	private static final Ƒ1<?,Map<String,String>> DEF = o -> emptyMap();
	private final ClassMap<Ƒ1<?,Map<String,String>>> names = new ClassMap<>();

	/**
	 * Registers info function for specified class..
	 *
	 * @param c type to add info extractor to. Use {@link Void} class to handle null (since only null can be an
	 * 'instance' of Void).
	 * @param extractor function that transforms instance to map of key value pairs representing information about the
	 * instance
	 */
	public <T> void add(Class<T> c, Ƒ1<? super T,Map<String,String>> extractor) {
		names.put(c, extractor);
	}

	/**
	 * Convenience method. Alternative to {@link #add(Class, sp.it.pl.util.functional.Functors.Ƒ1)} which passes already
	 * created map to the extractor.
	 *
	 * @param c type to add info extractor to. Use {@link Void} class to handle null (since only null can be an
	 * 'instance' of Void).
	 * @param extractor function that puts a key value pairs representing information about the instance into the
	 * provided map
	 */
	public <T> void add(Class<T> c, BiConsumer<? super T,Map<String,String>> extractor) {
		names.put(c, (T o) -> {
			Map<String,String> m = new HashMap<>();
			extractor.accept(o, m);
			return m;
		});
	}

	/**
	 * Returns name/string representation of the object instance. If none is
	 * provided, {@link Objects#toString(java.lang.Object)} is used.
	 *
	 * @param instance Object to get name of. Can be null, in which case its treated as of type {@link Void}.
	 * @return computed name of the object instance. Never null.
	 */
	@SuppressWarnings("unchecked")
	public Map<String,String> get(Object instance) {
		// Handle null as void so user can register his own function
		Class c = instance==null ? Void.class : instance.getClass();
		Ƒ1<?,Map<String,String>> f = names.getElementOfSuper(c);
		// Fall back to default implementation
		if (f==null) f = DEF;

		return ((Ƒ1<Object,Map<String,String>>) f).apply(instance);
	}

}