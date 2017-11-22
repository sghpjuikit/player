package sp.it.pl.util.collections.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Map accumulating multiple values of key into into single cumulative value. It accumulates elements E to accumulation
 * buckets C based on key K derived from the elements.
 *
 * @param <E> element that will be split/accumulated
 * @param <K> key extracted from element E to hash cache buckets on
 * @param <C> accumulation bucket. (Not necessarily) a collection such as List. Can even be an element E itself. For
 * example a sum or last added element (per key). Depends on accumulation strategy. Because the type of accumulation
 * bucket is unknown, elements can not be removed. See {@link sp.it.pl.util.collections.map.CollectionMap}.
 *
 * @see CollectionMap
 */
public class AccumulationMap<E, K, C> extends HashMap<K,C> {
	/**
	 * Extracts keys from elements. Determines the splitting parts of the caching  strategy, e.g. using a predicate
	 * would split the original collection on two parts - elements that test true, and elements that test false.
	 */
	public Function<? super E,? extends K> keyMapper;
	/**
	 * Builds cache bucket/accumulation container when there is none for the given key during accumulation.
	 */
	public Supplier<? extends C> cacheFactory;
	/**
	 * Defines how the elements will be accumulated into the cache bucket. If the bucket is a collection, you probably
	 * wish to use {@code (element, collection) -> collection.add(element);}, but different reducing strategies can be
	 * used, for example {@code (element,sum) -> sum+number;}
	 */
	public BiConsumer<? super E,? super C> cacheAccumulator;

	public AccumulationMap(Function<? super E,? extends K> keyMapper, Supplier<? extends C> cacheFactory, BiConsumer<? super E,? super C> cacheAccumulator) {
		this.keyMapper = keyMapper;
		this.cacheFactory = cacheFactory;
		this.cacheAccumulator = cacheAccumulator;
	}

	/** Accumulates given collection into this cache map. The collection remains unaffected. */
	public void accumulate(Iterable<? extends E> es) {
		for (E e : es) accumulate(e);
	}

	public void accumulate(K k, Iterable<? extends E> es) {
		for (E e : es) accumulate(k, e);
	}

	/** Accumulates given element into this map. */
	public void accumulate(E e) {
		K k = keyMapper.apply(e);
		C c = computeIfAbsent(k, k1 -> cacheFactory.get());
		cacheAccumulator.accept(e, c);
	}

	// TODO: enable, but rename, as it adds ambiguity with #accumulate(K k, E... es)
//	public void accumulate(E... es) {
//		for (E e : es)
//			accumulate(e);
//	}

	public void accumulate(K k, E e) {
		C c = computeIfAbsent(k, k1 -> cacheFactory.get());
		cacheAccumulator.accept(e, c);
	}

	@SafeVarargs
	public final void accumulate(K k, E... es) {
		for (E e : es)
			accumulate(k, e);
	}

	/**
	 * Multi key get.
	 *
	 * @return list containing of all cache buckets / accumulation containers assigned to keys in the given collection.
	 */
	public List<C> getCacheOf(Collection<K> keys) {
		List<C> out = new ArrayList<>();
		for (K k : keys) {
			C c = get(k);
			if (c!=null) out.add(c);
		}
		return out;
	}

	@Override
	public void clear() {
		super.clear();
	}
}