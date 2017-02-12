package util.type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import util.access.fieldvalue.ObjectField;
import util.collections.map.ClassMap;

import static util.dev.Util.noØ;
import static util.functional.Util.stream;

/**
 * Map storing sets of attributes for classes, formally: set of {@link util.access.fieldvalue.ObjectField}.
 */
public class ObjectFieldMap {

	private final ClassMap<Set> fields = new ClassMap<>();
	private final ClassMap<Set> cache = new ClassMap<>();
	private final ClassMap<Set> cache2 = new ClassMap<>();

	public <T> void add(Class<T> c, Collection<ObjectField<T>> fields) {
		noØ(c, fields);
		fields.forEach(field -> add(c,field));
	}

	public <T> void add(Class<T> c, ObjectField<T>... fields) {
		noØ(c, fields);
		stream(fields).forEach(field -> add(c,field));
	}

	@SuppressWarnings("unchecked")
	public <T> void add(Class<T> c, ObjectField<T> field) {
		noØ(c, field);
		fields.computeIfAbsent(c, key -> new HashSet<>()).add(field);
		cache.remove(c);
		cache2.remove(c);
	}

	@SuppressWarnings("unchecked")
	public <T> Set<ObjectField<T>> get(Class<T> c) {
		noØ(c);
		return (Set) cache.computeIfAbsent(c, key -> stream(fields.getElementsOfSuperV(key)).flatMap(Set::stream).toSet());
	}

	@SuppressWarnings("unchecked")
	public <T> Set<ObjectField<T>> getExact(Class<T> c) {
		noØ(c);
		return (Set) cache2.computeIfAbsent(c, key -> stream(fields.getElementsOf(key)).flatMap(Set::stream).toSet());
	}
}