package util.collections.map;

import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import unused.TriConsumer;
import util.collections.map.Map2D.Key;
import static java.util.stream.Collectors.toSet;

/**
 * 2 dimensional map - map with key consisting of two keys. For key identity, see {@link Key}.
 */
public class Map2D<K1, K2, E> extends HashMap<Key<K1,K2>,E> {

	public void put(K1 key1, K2 key2, E value) {
		put(new Key<>(key1, key2), value);
	}

	public void forEach(TriConsumer<K1,K2,E> action) {
		forEach((key, element) -> action.accept(key.key1(), key.key2(), element));
	}

	/** Removes entry mapped to key constructed from the sub-keys in given order. */
	public E remove2D(K1 key1, K2 key2) {
		return remove(new Key<>(key1, key2));
	}

	/** Removes entry mapped to given key. */
	public E remove2D(Key<K1,K2> key) {
		return remove(key);
	}

	public Stream<E> removeIf(Predicate<Key<K1,K2>> condition) {
		return keySet().stream().filter(condition)
				.collect(toSet()).stream()
				.map(this::remove2D);
	}

	public Stream<E> removeIfKey1(K1 key1) {
		return removeIf(k -> k.key1().equals(key1));
	}

	public Stream<E> removeIfKey2(K2 key2) {
		return removeIf(k -> k.key2().equals(key2));
	}

	public Optional<E> getOpt(Key<K1,K2> key) {
		return Optional.ofNullable(get(key));
	}

	/**
	 * 2 dimensional map key consisting of 2 sub-keys.
	 * Use with {@link Map2D} or any map implementation.
	 * <p/>
	 * It is impossible to safely use this class to map objects by 2 sub-keys of the same type when
	 * the sub-key's value sets are not distinct (see below).
	 * <p/>
	 * Note that if type of sub-keys a and b are equivalent, i.e. this key is homogeneous,
	 * (e.g. (String, String)), the following is always true:
	 * <p/>
	 * {@code new Key(a,b).equals(new Key(b,a))}
	 * <p/>
	 * In fact, this is always true, but remains hidden due to compile-time order enforcement when
	 * this key is heterogeneous, i.e. it is impossible to do both:
	 * <p/>
	 * {@code new Key(a,b);}
	 * <p/>
	 * and
	 * <p/>
	 * {@code new Key(b,a);}
	 */
	public static class Key<K1, K2> {
		private final Object key1;
		private final Object key2;
		private final boolean switched;

		public Key(K1 key1, K2 key2) {
			switched = key1.hashCode()>key2.hashCode();
			this.key1 = switched ? key2 : key1;
			this.key2 = switched ? key1 : key2;
		}

		/** Returns 1st sub-key according to the order at creation time. */
		@SuppressWarnings("unchecked")
		public K1 key1() {
			return (K1) (switched ? key2 : key1);
		}

		/** Returns 2st sub-key according to the order at creation time. */
		@SuppressWarnings("unchecked")
		public K2 key2() {
			return (K2) (switched ? key1 : key2);
		}

		/**
		 * Keys are equal when their sub-keys are equal. Note that sub-key order does not play a
		 * role, i.e.
		 * <p/>
		 * {@code new Key(a,b).equals(new Key(b,a))}
		 * <p/>
		 * always hold true.
		 */
		@SuppressWarnings("SimplifiableIfStatement")
		@Override
		public boolean equals(Object obj) {
			if (this==obj) return true;
			return obj instanceof Key && key1.equals(((Key) obj).key1) && key2.equals(((Key) obj).key2);
		}

		@Override
		public int hashCode() {
			int hash = 5;
			hash = 41*hash + Objects.hashCode(this.key1);
			hash = 41*hash + Objects.hashCode(this.key2);
			return hash;
		}

	}
}