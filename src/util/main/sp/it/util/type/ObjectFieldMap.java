package sp.it.util.type;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import sp.it.util.access.fieldvalue.ObjectField;
import sp.it.util.collections.map.ClassMap;
import static java.util.stream.Collectors.toSet;
import static sp.it.util.dev.FailKt.noNull;
import static sp.it.util.functional.Util.stream;

/**
 * Map storing sets of attributes for classes, formally: set of {@link sp.it.util.access.fieldvalue.ObjectField}.
 */
public class ObjectFieldMap {

	private final ClassMap<Set> fields = new ClassMap<>();
	private final ClassMap<Set> cache = new ClassMap<>();
	private final ClassMap<Set> cache2 = new ClassMap<>();

	public <T> void add(Class<T> c, Collection<? extends ObjectField<T,?>> fields) {
		noNull(c);
		noNull(fields);
		fields.forEach(field -> add(c, field));
	}

	@SafeVarargs
	public final <T> void add(Class<T> c, ObjectField<T,?>... fields) {
		noNull(c);
		noNull(fields);
		stream(fields).forEach(field -> add(c, field));
	}

	@SuppressWarnings("unchecked")
	public <T> void add(Class<T> c, ObjectField<T,?> field) {
		noNull(c);
		noNull(fields);
		fields.computeIfAbsent(c, key -> new HashSet<>()).add(field);
		cache.keySet().removeIf(c::isAssignableFrom);
		cache2.keySet().removeIf(c::isAssignableFrom);
	}

	@SuppressWarnings({"unchecked", "RedundantCast"})
	public <T> Set<ObjectField<T,?>> get(Class<T> c) {
		noNull(c);
		return (Set) cache.computeIfAbsent(c, key -> (Set)
			fields.getElementsOfSuperV(key).stream().flatMap(Set::stream).collect(toSet()));
	}

	@SuppressWarnings({"unchecked", "RedundantCast"})
	public <T> Set<ObjectField<T,?>> getExact(Class<T> c) {
		noNull(c);
		return (Set) cache2.computeIfAbsent(c, key -> (Set)
			fields.getElementsOf(key).stream().flatMap(Set::stream).collect(toSet()));
	}

	public static final @NotNull ObjectFieldMap DEFAULT = new ObjectFieldMap();
}