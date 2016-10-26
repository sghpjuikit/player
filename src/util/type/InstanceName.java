package util.type;

import java.util.Objects;

import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;

/**
 *
 * @author Martin Polakovic
 */
public class InstanceName {

	private static final Ƒ1<?,String> DEF = Objects::toString;  // Default implementation must be able to handle null
	private final ClassMap<Ƒ1<?,String>> names = new ClassMap<>();

	/**
	 * Registers name function for specified class and all its subclasses that
	 * do not have any name registered.
	 *
	 * @param c type to add name function to. Use {@link Void} class to handle null (since only null can be an
	 *          'instance' of Void).
	 * @param parser instance to instance name transformer function
	 */
	public <T> void add(Class<T> c, Ƒ1<? super T,String> parser) {
		names.put(c, parser);
	}

	/**
	 * Returns name/string representation of the object instance. If none is
	 * provided, {@link Objects#toString(java.lang.Object)} is used.
	 *
	 * @param instance Object to get name of. Can be null, in which case its
	 * treated as of type {@link Void}.
	 * @return computed name of the object instance. Never null.
	 */
	@SuppressWarnings("unchecked")
	public String get(Object instance) {
		// Handle null as void so user can register his own function
		Class c = instance==null ? Void.class : instance.getClass();
		Ƒ1<?,String> f = names.getElementOfSuper(c);
		// Fall back to default implementation
		if (f==null) f = DEF;

		return ((Ƒ1<Object,String>) f).apply(instance);
	}

}