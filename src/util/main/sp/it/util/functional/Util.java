package sp.it.util.functional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import sp.it.util.functional.Functors.F1;
import sp.it.util.functional.Functors.F2;
import sp.it.util.functional.Functors.FP;
import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static sp.it.util.dev.FailKt.failIf;
import static sp.it.util.dev.FailKt.noNull;

@SuppressWarnings("unused")
public interface Util {

	/** Function returning the inputs. */
	F1<Object,Object> IDENTITY = o -> o;

	/** Predicate returning true iff object is not null. */
	FP<Object> ISNT0 = Objects::nonNull;

	/** Predicate returning true iff object is null. */
	FP<Object> IS0 = Objects::isNull;

	/** Predicate returning true. Matches every object. */
	FP<Object> IS = o -> true;

	/** Predicate returning false. Matches no object. */
	FP<Object> ISNT = o -> false;

	/** Comparator returning 0. Produces no order change. */
	Comparator<Object> SAME = (a,b) -> 0;

	/** Repeat action n times. */
	static void repeat(int n, Runnable action) {
		for (int x = 0; x<n; x++) action.run();
	}

	/** Repeat action n times. Action takes the index of execution as parameter starting from 0. */
	static void repeat(int n, IntConsumer action) {
		for (int x = 0; x<n; x++) action.accept(x);
	}

/* ---------- COLLECTORS -------------------------------------------------------------------------------------------- */

	/** Collector returning the minimum element. */
	static <V, C extends Comparable<? super C>> Collector<V,?,Optional<V>> minBy(F1<? super V,C> by) {
		return Collectors.reducing(BinaryOperator.minBy(by(by)));
	}

	/** Collector returning the maximum element. */
	static <V, C extends Comparable<? super C>> Collector<V,?,Optional<V>> maxBy(F1<? super V,C> by) {
		return Collectors.reducing(BinaryOperator.maxBy(by(by)));
	}

	/** Collector returning the minimum element. */
	static <V, C extends Comparable<? super C>> Collector<V,?,V> minByIdentity(V identity, F1<? super V,C> by) {
		return Collectors.reducing(identity, BinaryOperator.minBy(by(by)));
	}

	/** Collector returning the maximum element. */
	static <V, C extends Comparable<? super C>> Collector<V,?,V> maxByIdentity(V identity, F1<? super V,C> by) {
		return Collectors.reducing(identity, BinaryOperator.maxBy(by(by)));
	}

/* ---------- COMPARATORS ------------------------------------------------------------------------------------------- */

	/**
	 * Creates comparator comparing E elements by extracted {@link Comparable} value.
	 * <p/>
	 * The returned comparator is serializable if the specified function
	 * and comparator are both serializable.
	 *
	 * @param <E> the type of element to be compared
	 * @param <C> the type of the extracted value to compare by
	 * @param extractor the function used to extract non null value to compare by
	 * @return comparator that compares by an extracted comparable value
	 * @throws NullPointerException if argument is null
	 */
	static <E, C extends Comparable<? super C>> Comparator<E> by(Function<? super E,? extends C> extractor) {
		noNull(extractor);
		return by(extractor, Comparable::compareTo);
	}

	/**
	 * Creates comparator comparing E elements by their string representation
	 * obtained by provided converter. Utilizes String.compareToIgnoreCase().
	 * <p/>
	 * Easy and concise way to compare objects without code duplication.
	 *
	 * @param extractor the function used to extract non null {@code String} value to compare by the object.
	 */
	static <E> Comparator<E> byNC(Function<E,String> extractor) {
		return (a, b) -> extractor.apply(a).compareToIgnoreCase(extractor.apply(b));
	}

	/**
	 * Accepts a function that extracts a sort key from a type {@code E}, and
	 * returns a {@code Comparator<E>} that compares by that sort key using
	 * the specified {@link Comparator}.
	 * <p/>
	 * The returned comparator is serializable if the specified function
	 * and comparator are both serializable.
	 *
	 * @param <E> the type of element to be compared
	 * @param <C> the type of the extracted value to compare by
	 * @param extractor the function used to extract non null value to compare by
	 * @param comparator the {@code Comparator} used to compare the sort key
	 * @return comparator that compares by an extracted value using the specified {@code Comparator}
	 * @throws NullPointerException if any argument is null
	 * @apiNote For example, to obtain a {@code Comparator} that compares {@code Person} objects by their last name
	 * ignoring case differences,
	 * <p>
	 * <pre>{@code
	 *     Comparator<Person> cmp = by(Person::getLastName, String.CASE_INSENSITIVE_ORDER);
	 * }</pre>
	 */
	static <E, C> Comparator<E> by(Function<? super E,? extends C> extractor, Comparator<? super C> comparator) {
		noNull(extractor);
		noNull(comparator);
		return (Comparator<E> & Serializable) (a, b) -> comparator.compare(extractor.apply(a), extractor.apply(b));
	}

	/**
	 * Accepts a function that extracts a sort key from a type {@code E}, and
	 * returns a {@code Comparator<E>} that compares by that sort key using
	 * the {@link Comparator} derived from {@link java.util.Comparator#naturalOrder()}.
	 * <p/>
	 *
	 * @param <E> the type of element to be compared
	 * @param <C> the type of the extracted value to compare by
	 * @param extractor the function used to extract nullable value to compare by
	 * @param comparatorModifier function that modifies comparing, it is supplied a default {@link
	 * java.util.Comparator#naturalOrder()} comparator.
	 * @return comparator that compares by an extracted value using the specified {@code Comparator}
	 * @throws NullPointerException if any argument is null
	 * @apiNote convenience method, helpful for cases where the extracted value may be null (and using for example
	 * {@link Comparator#nullsFirst(java.util.Comparator)}).
	 */
	static <E, C extends Comparable<? super C>> Comparator<E> by(Function<? super E,? extends C> extractor, Function<Comparator<? super C>,Comparator<? super C>> comparatorModifier) {
		noNull(extractor);
		noNull(comparatorModifier);
		return (Comparator<E> & Serializable) (a, b) -> comparatorModifier.apply(Comparator.naturalOrder()).compare(extractor.apply(a), extractor.apply(b));
	}

	/** @return set of elements of provided collection with no duplicates. Order undefined. */
	static <E> Set<E> noDups(Collection<E> c) {
		return new HashSet<>(c);
	}

	/** @return set of elements of provided collection with no duplicates. Retains order. */
	static <E> Set<E> noDupsStable(Collection<E> c) {
		return new LinkedHashSet<>(c);
	}

/* ---------- SUPPLIERS --------------------------------------------------------------------------------------------- */

	/** Returns the first supplied non null value or null if all supplied values are null. */
	@SafeVarargs
	static <I> I firstNotNull(Supplier<I>... suppliers) {
		for (Supplier<I> s : suppliers) {
			I i = s.get();
			if (i!=null) return i;
		}
		return null;
	}

/* ---------- COLLECTION -> OBJECT ------------------------------------------------------------------------------ */

	/**
	 * Checks whether all elements of the list are equal by some property
	 * obtained using specified transformation.
	 * <p/>
	 * For example checking whether all lists have the same size:
	 * <pre>{@code equalBy(lists,List::size) }</pre>
	 *
	 * @return true if transformation of each element in the list produces equal result.
	 */
	static <V, R> boolean equalBy(List<V> o, Function<V,R> by) {
		if (o.size()<2) return true;
		R r = by.apply(o.get(0));
		for (int i = 1; i<o.size(); i++)
			if (!r.equals(by.apply(o.get(i)))) return false;
		return true;
	}

	/**
	 * Assuming a map of lists, i-slice is a list i-th elements in every list.
	 *
	 * @return i-th slice of the map m
	 */
	static <K, T> Map<K,T> mapSlice(Map<K,List<? extends T>> m, int i) {
		Map<K,T> o = new HashMap<>();
		m.forEach((key, value) -> o.put(key, value.get(i)));
		return o;
	}

/* ---------- FOR --------------------------------------------------------------------------------------------------- */

	/** Functional equivalent of a for loop. */
	static <I> void forEach(Iterable<I> items, Consumer<? super I> action) {
		for (I item : items)
			action.accept(item);
	}

	/** Functional equivalent of a for loop. */
	static <I> void forEach(I[] items, Consumer<? super I> action) {
		for (I item : items)
			action.accept(item);
	}

	/** Loops over both lists simultaneously. Must be of the same size. */
	static <A, B> void forEachBoth(List<A> a, List<B> b, BiConsumer<A,B> action) {
		failIf(a.size()!=b.size());
		for (int i = 0; i<a.size(); i++)
			action.accept(a.get(i), b.get(i));
	}

	/** Loops over both arrays simultaneously. Must be of the same size. */
	static <A, B> void forEachBoth(A[] a, B[] b, BiConsumer<A,B> action) {
		failIf(a.length!=b.length);
		for (int i = 0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	static void forEachBoth(int[] a, int[] b, IntBiConsumer action) {
		failIf(a.length!=b.length);
		for (int i = 0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	static void forEachBoth(long[] a, long[] b, LongBiConsumer action) {
		failIf(a.length!=b.length);
		for (int i = 0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	static void forEachBoth(double[] a, double[] b, DoubleBiConsumer action) {
		failIf(a.length!=b.length);
		for (int i = 0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	/** Loops over list zipping index with each item. Index starts at 0. */
	static <T> void forEachWithI(Collection<T> c, BiConsumer<Integer,T> action) {
		int i = 0;
		for (T item : c) {
			action.accept(i, item);
			i++;
		}
	}

	/** Loops over array zipping index with each item. Index starts at 0. */
	static <T> void forEachWithI(T[] c, BiConsumer<Integer,T> action) {
		int i = 0;
		for (T item : c) {
			action.accept(i, item);
			i++;
		}
	}

	interface IntBiConsumer {
		void accept(int i1, int i2);
	}

	interface LongBiConsumer {
		void accept(long d1, long d2);
	}

	interface DoubleBiConsumer {
		void accept(double d1, double d2);
	}

	/** Loops over cartesian product C x C of a collection C. */
	static <E> void forEachPair(Collection<E> c, BiConsumer<? super E,? super E> action) {
		forEachPair(c, c, action);
	}

	/** Loops over cartesian product C x C of a collection C, ignoring symmetric elements (i,j) (j;i). */
	static <E> void forEachCartesianHalf(Collection<E> c, BiConsumer<? super E,? super E> action) {
//        for (int i=0; i<c.size(); i++)
//            for (int j=i; j<c.size(); j++)
//                action.accept(c.get(i), c.get(j));

//        int j = 0;
//        for (E e : c) {
//            c.stream().skip(j).forEach(t -> action.accept(e,t));
//            j++;
//        }

		int j = 1;
		for (E e : c) {
			int i = j;
			for (E t : c) {
				if (i>0) i--;
				if (i==0) action.accept(e, t);
			}
			j++;
		}
	}

	/**
	 * Loops over cartesian product C x C of a collection C, ignoring symmetric elements (i,j)(j;i) and
	 * self elements (i,i).
	 */
	static <E> void forEachCartesianHalfNoSelf(Collection<E> c, BiConsumer<? super E,? super E> action) {
//        for (int i=0; i<c.size(); i++)
//            for (int j=i+1; j<c.size(); j++)
//                action.accept(c.get(i), c.get(j));

//        int j = 1;
//        for (E e : c) {
//            c.stream().skip(j).forEach(t -> action.accept(e,t));
//            j++;
//        }

		int j = 1;
		for (E e : c) {
			int i = j;
			for (E t : c) {
				if (i==0) action.accept(e, t);
				if (i>0) i--;
			}
			j++;
		}
	}

	/**
	 * Loops over cartesian product C1 x C2 of collections C1, C2.
	 *
	 * @param action 1st parameter is element from 1st collection, 2nd parameter is el. from 2nd
	 */
	static <E, T> void forEachPair(Collection<E> c1, Collection<T> c2, BiConsumer<? super E,? super T> action) {
		for (E e : c1) for (T t : c2) action.accept(e, t);
	}

	/** Loops over list zipping each item with a companion derived from it. */
	static <T, W> void forEachWith(Collection<T> c, Function<T,W> toCompanion, BiConsumer<? super T,? super W> action) {
		for (T t : c)
			action.accept(t, toCompanion.apply(t));
	}

	/**
	 * Returns stream of elements mapped by the mapper from index-element pairs of specified collection. Indexes start
	 * at 0.
	 * <p/>
	 * Functionally equivalent to: List.stream().mapB(item->new Pair(item,list.indexOf(item))).mapB(pair->mapper.mapB(p))
	 * but avoiding the notion of a Pair or Touple, and without any collection traversal to get indexes.
	 *
	 * @param <T> element type
	 * @param <R> result type
	 * @param c collection to iterate
	 * @param mapper mapper of the element (zipped with its index as 1st parameter) to a transformed one
	 * @return stream of mapped elements
	 */
	static <T, R> Stream<R> forEachIStream(Collection<T> c, BiFunction<Integer,T,R> mapper) {
		int i = 0;
		Stream.Builder<R> b = Stream.builder();
		for (T item : c) {
			b.accept(mapper.apply(i, item));
			i++;
		}
		return b.build();
	}

	/**
	 * Equivalent to {@link #forEachIStream(java.util.Collection, java.util.function.BiFunction)}
	 * but iterates backwards (the index values are also reversed so last item
	 * has index value of 0).
	 */
	static <T, R> Stream<R> forEachIRStream(List<T> c, BiFunction<Integer,T,R> mapper) {
		int size = c.size();
		Stream.Builder<R> b = Stream.builder();
		for (int i = 1; i<=size; i++)
			b.accept(mapper.apply(i - 1, c.get(size - i)));
		return b.build();
	}

	/**
	 * More general version of {@link #forEachIStream(java.util.Collection, java.util.function.BiFunction)} .
	 * The index can now be of any type and how it changes is defined by a parameter.
	 *
	 * @param <I> key type
	 * @param <T> element type
	 * @param <R> result type
	 * @param c collection to iterate
	 * @param initial_val for example: 0
	 * @param operation for example: number -> number++
	 * @param mapper maps the key-object pair into another object
	 * @return stream of mapped values by a mapper out of key-element pairs
	 */
	static <I, T, R> Stream<R> forEachIStream(Collection<T> c, I initial_val, Function<I,I> operation, BiFunction<I,T,R> mapper) {
		I i = initial_val;
		Stream.Builder<R> b = Stream.builder();
		for (T item : c) {
			b.accept(mapper.apply(i, item));
			i = operation.apply(i);
		}
		return b.build();
	}

	static <R> Stream<R> forEachInLine(double fromX, double fromY, double toX, double toY, long count, BiFunction<Double,Double,R> mapper) {
		failIf(count<0);
		return forEachInLineBy(fromX, fromY, count<=1 ? 0 : (toX - fromX)/(count - 1), count<=1 ? 0 : (toY - fromY)/(count - 1), count, mapper);
	}

	static <R> Stream<R> forEachInLineBy(double x, double y, double byX, double byY, long count, BiFunction<Double,Double,R> mapper) {
		failIf(count<0);
		return IntStream.iterate(0, i -> i<=count, i -> i + 1)
				.mapToObj(i -> mapper.apply(x + i*byX, y + i*byY));
	}

	static <R> Stream<R> forEachOnCircle(long count, TriDoubleFunction<R> mapper) {
		failIf(count<0);
		return forEachOnCircle(0, 0, 1, count, mapper);
	}

	static <R> Stream<R> forEachOnCircleBy(double x, double y, double by, long count, TriDoubleFunction<R> mapper) {
		failIf(count<0);
		double circumference = by*count;
		double radius = circumference/(2*PI);
		return forEachOnCircle(x, y, radius, count, mapper);
	}

	static <R> Stream<R> forEachOnCircle(double x, double y, double radius, long count, TriDoubleFunction<R> mapper) {
		failIf(count<0);
		return DoubleStream.iterate(0, a -> a + 2*PI/count)
				.limit(count)
				.mapToObj(a -> mapper.apply(x + radius*cos(a), y + radius*sin(a), a));
	}

	interface TriDoubleFunction<R> {
		R apply(double d1, double d2, double d3);
	}

	/****************************** () -> collection ******************************/

	static <T> Stream<Indexed<T>> toIndexedStream(Collection<T> c) {
		int i = 0;
		Stream.Builder<Indexed<T>> b = Stream.builder();
		for (T item : c) {
			b.accept(new Indexed<>(i, item));
			i++;
		}
		return b.build();
	}

	class Indexed<T> {
		public final int i;
		public final T value;

		public Indexed(int i, T value) {
			this.i = i;
			this.value = value;
		}
	}

	/** Creates an array filled with provided elements. The array's length will equal element count. */
	@SafeVarargs
	static <T> T[] array(T... elements) {
		return elements;
	}

	@SafeVarargs
	static <T> Set<T> set(T... ts) {
		HashSet<T> s = new HashSet<>(ts.length);
		for (T t : ts) s.add(t);
		return s;
	}

	/** Returns modifiable list containing specified elements. */
	@SafeVarargs
	static <T> List<T> list(T... ts) {
		ArrayList<T> s = new ArrayList<>(ts.length);
		for (T t : ts) s.add(t);
		return s;
	}

	/**
	 * Returns unmodifiable list containing specified elements.
	 * Optimized:
	 * <ul>
	 * <li> if zero parameters - {@link Collections#EMPTY_LIST}
	 * <li> if 1 parameters - {@link Collections#singletonList(java.lang.Object)}
	 * <li> else parameters - {@link java.util.Arrays#asList(Object[])}
	 * </ul>
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	static <T> List<T> listRO(T... ts) {
		int size = ts.length;
		if (size==0) return EMPTY_LIST;
		if (size==1) return singletonList(ts[0]);
		else return Arrays.asList(ts);
	}

	/** Returns modifiable list containing elements in the specified collection. */
	static <T> List<T> list(Collection<? extends T> a) {
		return new ArrayList<>(a);
	}

	/** Returns modifiable list containing specified element i times. */
	static <T> List<T> list(int i, T a) {
		List<T> l = new ArrayList<>(i);
		for (int j = 0; j<i; j++) l.add(a);
		return l;
	}

	/** Returns modifiable list containing element supplied by specified supplier i times. */
	static <T> List<T> list(int i, Supplier<T> factory) {
		List<T> l = new ArrayList<>(i);
		for (int j = 0; j<i; j++) l.add(factory.get());
		return l;
	}

	/**
	 * Returns modifiable list containing element supplied by specified supplier i times. Integer
	 * params range from 0 to i-1 inclusive;
	 */
	static <T> List<T> listF(int i, Function<Integer,T> factory) {
		List<T> l = new ArrayList<>(i);
		for (int j = 0; j<i; j++) l.add(factory.apply(j));
		return l;
	}

	static <T> Stream<T> stream() {
		return Stream.empty();
	}

	static <T> Stream<T> stream(T t) {
		return Stream.of(t);
	}

	@SafeVarargs
	static <T> Stream<T> stream(T... t) {
		return t.length==0 ? Stream.empty() : Stream.of(t);
	}

	static <T> Stream<T> stream(Stream<? extends T> s1, Stream<? extends T> s2) {
		return Stream.concat(s1, s2);
	}

	static <T> Stream<T> stream(T o, Stream<T> t) {
		return Stream.concat(Stream.of(o), t);
	}

	static <T> Stream<T> stream(Collection<T> t) {
		return t.stream();
	}

	static <T> Stream<T> stream(Iterator<T> t) {
		Iterable<T> iterable = () -> t;
		return StreamSupport.stream(iterable.spliterator(), false);
	}

	static <T> Stream<T> stream(Iterable<T> t) {
		return stream(t.iterator());
	}

	static <T> Stream<T> stream(Enumeration<T> t) {
		return stream(t.asIterator());
	}

	static <A, B, R> Stream<R> streamBi(A[] a, B[] b, F2<A,B,R> zipper) {
		failIf(a.length!=b.length);
		Stream.Builder<R> builder = Stream.builder();
		for (int i = 0; i<a.length; i++)
			builder.accept(zipper.apply(a[i], b[i]));
		return builder.build();
	}

	/** Creates stream of {@link Integer} in range from-to inclusive. */
	static Stream<Integer> range(int fromInclusive, int toInclusive) {
		Stream.Builder<Integer> b = Stream.builder();
		for (int i = fromInclusive; i<=toInclusive; i++) b.accept(i);
		return b.build();
	}

	static Stream<Double> range(double fromInclusive, double toInclusive) {
		Stream.Builder<Double> b = Stream.builder();
		for (double i = fromInclusive; i<=toInclusive; i++) b.accept(i);
		return b.build();
	}

/* ---------- COLLECTION -> COLLECTION ------------------------------------------------------------------------------ */

	/** Filters array. Returns list. Source remains unchanged. */
	static <T> List<T> filter(T[] a, Predicate<T> f) {
		return Stream.of(a).filter(f).collect(toList());
	}

	/** Filters collection. Returns list. Source remains unchanged. */
	static <T> List<T> filter(Collection<T> c, Predicate<T> f) {
		return c.stream().filter(f).collect(toList());
	}

	/** Maps array. Returns list. Source remains unchanged. */
	static <T, R> List<R> map(T[] a, Function<T,R> m) {
		return Stream.of(a).map(m).collect(toList());
	}

	/** Maps collection. Returns list. Source remains unchanged. */
	static <T, R> List<R> map(Collection<? extends T> c, Function<? super T,? extends R> m) {
		return c.stream().map(m).collect(toList());
	}

	@SuppressWarnings("unchecked")
	static <T> List<T> split(String txt, String regex, int i, Function<String,T> m) {
		if (txt.isEmpty()) return EMPTY_LIST;
		return Stream.of(txt.split(regex, i)).map(m).collect(toList());
	}

	@SuppressWarnings("unchecked")
	static <T> List<T> split(String txt, String regex, Function<String,T> m) {
		if (txt.isEmpty()) return EMPTY_LIST;
		return Stream.of(txt.split(regex, -1)).map(m).collect(toList());
	}

	@SuppressWarnings("unchecked")
	static List<String> split(String txt, String regex, int i) {
		if (txt.isEmpty()) return EMPTY_LIST;
		return Stream.of(txt.split(regex, i)).collect(toList());
	}

	@SuppressWarnings("unchecked")
	static List<String> split(String txt, String regex) {
		if (txt.isEmpty()) return EMPTY_LIST;
		return Stream.of(txt.split(regex, -1)).collect(toList());
	}

	/**
	 * Finds the smallest integer key greater than or equal to specified value, that the map
	 * doesn't contain mapping for.
	 *
	 * @param map map to search keys in
	 * @param from the first and smallest key to check
	 * @return the smallest nonexistent integer key key
	 */
	@SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
	static int findFirstEmptyKey(Map<Integer,?> map, int from) {
		return IntStream.iterate(from, i -> i + 1).filter(i -> !map.containsKey(i)).findFirst().getAsInt();
	}

	@SuppressWarnings({"ConstantConditions", "OptionalGetWithoutIsPresent"})
	static int findFirstInt(int from, IntPredicate condition) {
		return IntStream.iterate(from, i -> i + 1).filter(condition).findFirst().getAsInt();
	}

}