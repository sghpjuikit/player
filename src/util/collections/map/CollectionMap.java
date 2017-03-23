package util.collections.map;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toCollection;

/**
 * Collection based implementation of {@link util.collections.map.AccumulationMap} - values mapped to the same key are
 * added in a collection (of provided type).
 *
 * @author Martin Polakovic
 */
public class CollectionMap<E, K, C extends Collection<E>> extends AccumulationMap<E,K,C> {

	public CollectionMap(Supplier<? extends C> cacheFactory, Function<E,K> keyMapper) {
		super(keyMapper, cacheFactory, (e, cache) -> cache.add(e));
	}

	/** De-accumulates (removes) given element from this map. */
	public void deAccumulate(E e) {
		K k = keyMapper.apply(e);
		C c = get(k);
		if (c!=null) {
			c.remove(e);
		}
	}

	/**
	 * Multi key get returning the combined content of the cache buckets.
	 *
	 * @return list containing all elements of all cache buckets / accumulation containers assigned to keys in the given
	 * collection.
	 */
	public C getElementsOf(Collection<K> keys) {
		C out = cacheFactory.get();
		for (K k : keys) {
			C c = get(k);
			if (c!=null) out.addAll(c);
		}
		return out;
	}

	/** Array version of {@link #getElementsOf(java.util.Collection)}. */
	@SafeVarargs
	public final C getElementsOf(K... keys) {
		C out = cacheFactory.get();
		for (K k : keys) {
			C c = get(k);
			if (c!=null) out.addAll(c);
		}
		return out;
	}

	/**
	 * Same as {@link #getElementsOf(java.util.Collection)}.
	 * Avoids creating intermediary collection of keys (parameter) and reduce
	 * memory.
	 */
	public C getElementsOf(Stream<K> keys) {
		return keys.map(this::get)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.collect(toCollection(cacheFactory));
	}
}