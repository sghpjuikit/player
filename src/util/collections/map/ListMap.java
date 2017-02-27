package util.collections.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import static java.util.stream.Collectors.toCollection;

/**
 * More specific cache map using {@link ArrayList} as a cache bucket/accumulation
 * container.
 * <p/>
 * Cache factory returns new list and accumulator adds element to the list, key
 * mapper remains unimplemented.
 * <p/>
 * Defines a
 *
 * @author Martin Polakovic
 */
public class ListMap<E, K> extends CollectionMap<E,K,List<E>> {

	/** Creates collection map with {@link ArrayList} list cache buckets. */
	public ListMap(Function<E,K> keyMapper) {
		super(keyMapper, ArrayList::new, (e, cache) -> cache.add(e));
	}

	public ListMap(Supplier<List<E>> cacheFactory, Function<E,K> keyMapper) {
		super(keyMapper, cacheFactory, (e, cache) -> cache.add(e));
	}

	/** Deaccumulates (removes) given element from this map. */
	public void deAccumulate(E e) {
		// get key
		K k = keyMapper.apply(e);
		// get cache storage with key
		List<E> c = get(k);
		if (c!=null) {
			// remove element
			c.remove(e);
		}
	}

	/**
	 * Multi key get returning the combined content of the cache buckets.
	 *
	 * @return list containing all elements of all cache buckets / accumulation containers assigned to keys in the given
	 * collection.
	 */
	public List<E> getElementsOf(Collection<K> keys) {
		List<E> out = cacheFactory.get();
		for (K k : keys) {
			List<E> c = get(k);
			if (c!=null) out.addAll(c);
		}
		return out;
	}

	// TODO: possible heap pollution @SafeVararg
	/** Array version of {@link #getElementsOf(java.util.Collection)}. */
	public List<E> getElementsOf(K... keys) {
		List<E> out = cacheFactory.get();
		for (K k : keys) {
			List<E> c = get(k);
			if (c!=null) out.addAll(c);
		}
		return out;
	}

	/**
	 * Same as {@link #getElementsOf(java.util.Collection)}.
	 * Avoids creating intermediary collection of keys (parameter) and reduce
	 * memory.
	 */
	public List<E> getElementsOf(Stream<K> keys) {
		return keys.map(this::get)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.collect(toCollection(cacheFactory));
	}
}