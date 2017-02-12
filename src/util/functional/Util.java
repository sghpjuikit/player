package util.functional;

import java.io.Serializable;
import java.util.*;
import java.util.Collections;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.reactfx.util.TriFunction;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.EntryStream;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import util.SwitchException;
import util.collections.Tuple2;
import util.functional.Functors.*;

import static java.lang.Math.*;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static util.collections.Tuples.tuple;
import static util.dev.Util.*;

/**
 *
 * @author Martin Polakovic
 */
@SuppressWarnings("unused")
public interface Util {

	/** Function returning the inputs. */
	Ƒ1<Object,Object> IDENTITY = o -> o;

	/** Predicate returning true iff object is not null. */
	ƑP<Object> ISNTØ = Objects::nonNull;

	/** Predicate returning true iff object is null. */
	ƑP<Object> ISØ = Objects::isNull;

	/** Predicate returning true. Matches every object. */
	ƑP<Object> IS = o -> true;

	/** Predicate returning false. Matches no object. */
	ƑP<Object> ISNT = o -> false;

	/** Comparator returning 0. Produces no order change. */
	Comparator<Object> SAME = (a,b) -> 0;

	/**
	 * Returns true if the arguments are equal to each other and false otherwise.
	 * If any argument is null, false is returned.
	 * Otherwise, equality is determined by using the equals method of the first argument.
	 */
	static boolean equalNonNull(Object a, Object b) {
		return a!=null && b!=null && a.equals(b);
	}

	/**
	 * Returns true if the arguments are equal to each other and false otherwise.
	 * If both arguments are null, true is returned and if exactly one argument is null, false is
	 * returned.
	 * Otherwise, equality is determined by using the equals method of the first argument.
	 */
	static boolean equalNull(Object a, Object b) {
		return a == b || (a!=null && a.equals(b));
	}

	static <E> boolean is(E o, E a) {
		return o==a;
	}

	static <E> boolean isAny(E o, E a) {
		return o==a;
	}

	static <E> boolean isAny(E o, E a, E b) {
		return o==a || o==b;
	}

	static <E> boolean isAny(E o, E a, E b, E c) {
		return o==a || o==b || o==c;
	}

	static <E> boolean isAny(E o, E a, E b, E c, E d) {
		return o==a || o==b || o==c || o==d;
	}

	@SafeVarargs
	static <E> boolean isAny(E o, E... es) {
		for (E e : es)
			if (o == e)
				return true;
		return false;
	}

	static <E> boolean isAny(E o, Collection<E> es) {
		for (E e : es)
			if (o == e)
				return true;
		return false;
	}

	static <E> boolean isContainedIn(E o, E a) {
		return o.equals(a);
	}

	static <E> boolean isContainedIn(E o, E a, E b) {
		return o.equals(a) || o.equals(b);
	}

	static <E> boolean isContainedIn(E o, E a, E b, E c) {
		return o.equals(a) || o.equals(b) || o.equals(c);
	}

	static <E> boolean isContainedIn(E o, E a, E b, E c, E d) {
		return o.equals(a) || o.equals(b) || o.equals(c) || o.equals(d);
	}

	@SafeVarargs
	static <E> boolean isContainedIn(E o, E... es) {
		for (E e : es)
			if (o.equals(e))
				return true;
		return false;
	}

	static <E> boolean isContainedIn(E o, Collection<E> es) {
		for (E e : es)
			if (o.equals(e)) return true;
		return false;
	}

	@SafeVarargs
	static <E> boolean isAll(E o, Predicate<E>... ps) {
		boolean b = true;
		for (Predicate<E> p : ps)
			b &= p.test(o);
		return b;
	}

	@SafeVarargs
	static <E> boolean isAny(E o, Predicate<E>... ps) {
		boolean b = false;
		for (Predicate<E> p : ps)
			b |= p.test(o);
		return b;
	}


	static boolean isØ(Object o) {
		return o==null;
	}

	static boolean isAnyØ(Object o) {
		return o==null;
	}

	static boolean isAnyØ(Object a, Object b) {
		return a==null || b==null;
	}

	static boolean isAnyØ(Object a, Object b, Object c) {
		return a==null || b==null || c==null;
	}

	static boolean isAnyØ(Object a, Object b, Object c, Object d) {
		return a==null || b==null || c==null || d==null;
	}

	static boolean isAnyØ(Object... objects) {
		if (objects==null) throw new IllegalArgumentException("Array must ot be null.");
		for (Object o : objects) if (o==null) return true;
		return false;
	}

	static boolean isNoneØ(Object o) {
		return o!=null;
	}

	static boolean isNoneØ(Object a, Object b) {
		return a!=null && b!=null;
	}

	static boolean isNoneØ(Object a, Object b, Object c) {
		return a!=null && b!=null && c!=null;
	}

	static boolean isNoneØ(Object a, Object b, Object c, Object d) {
		return a!=null && b!=null && c!=null && d!=null;
	}

	static boolean isNoneØ(Object... objects) {
		for (Object o : objects) if (o!=null) return false;
		return true;
	}

	/** Repeat action n times. */
	static void repeat(int n, Runnable action) {
		for (int x=0; x<n; x++) action.run();
	}

	/** Repeat action n times. Action takes the index of execution as parameter starting from 0. */
	static void repeat(int n, IntConsumer action) {
		for (int x=0; x<n; x++) action.accept(x);
	}

/* ---------- COLLECTORS -------------------------------------------------------------------------------------------- */

	/** Simple Collector concatenating Strings to coma separated list (CSList)
	 *  by delimiter ", ". */
	Collector<CharSequence,?,String> toCSList = Collectors.joining(", ");

	/** Collector returning the minimum element. */
	static <V,C extends Comparable<? super C>> Collector<V, ?, Optional<V>> minBy(Ƒ1<? super V,C> by) {
		return Collectors.reducing(BinaryOperator.minBy(by(by)));
	}

	/** Collector returning the maximum element. */
	static <V,C extends Comparable<? super C>> Collector<V, ?, Optional<V>> maxBy(Ƒ1<? super V,C> by) {
		return Collectors.reducing(BinaryOperator.maxBy(by(by)));
	}

	/** Collector returning the minimum element. */
	static <V,C extends Comparable<? super C>> Collector<V, ?, V> minBy(V identity, Ƒ1<? super V,C> by) {
		return Collectors.reducing(identity, BinaryOperator.minBy(by(by)));
	}

	/** Collector returning the maximum element. */
	static <V,C extends Comparable<? super C>> Collector<V, ?, V> maxBy(V identity, Ƒ1<? super V,C> by) {
		return Collectors.reducing(identity, BinaryOperator.maxBy(by(by)));
	}

/* ---------- COMPARATORS ------------------------------------------------------------------------------------------- */

	/** Comparator utilizing Comparable.compareTo() of the Comparables. */
	@SuppressWarnings("unchecked")
	Comparator<Comparable> COMPARATOR_DEF = Comparable::compareTo;

	/** String comparator utilizing String.compareTo() of the Strings */
	Comparator<? super String> COMPARATOR_STR = COMPARATOR_DEF;

	/** String comparator utilizing String.compareToIgnoreCase(). */
	Comparator<String> COMPARATOR_STR_CASELESS = String::compareToIgnoreCase;

	/**
	 * Creates comparator comparing E elements by extracted {@link Comparable} value.
	 * <p/>
	 * The returned comparator is serializable if the specified function
	 * and comparator are both serializable.
	 *
	 * @param  <E> the type of element to be compared
	 * @param  <C> the type of the extracted value to compare by
	 * @param  extractor the function used to extract non null value to compare by
	 * @return comparator that compares by an extracted comparable value
	 * @throws NullPointerException if argument is null
	 */
	static <E,C extends Comparable<? super C>> Comparator<E> by(Function<? super E, ? extends C> extractor) {
		noØ(extractor);
		return by(extractor, Comparable::compareTo);
	}

	/**
	 * Creates comparator comparing E elements by their string representation
	 * obtained by provided converter. Utilizes String.compareToIgnoreCase().
	 * <p/>
	 * Easy and concise way to compare objects without code duplication.
	 *
	 * @param extractor the function used to extract non null {@code String} value to compare by
	 * the object.
	 */
	static <E> Comparator<E> byNC(Function<E,String> extractor) {
		return (a,b) -> extractor.apply(a).compareToIgnoreCase(extractor.apply(b));
	}

	/**
	 * Accepts a function that extracts a sort key from a type {@code E}, and
	 * returns a {@code Comparator<E>} that compares by that sort key using
	 * the specified {@link Comparator}.
	 * <p/>
	 * The returned comparator is serializable if the specified function
	 * and comparator are both serializable.
	 *
	 * @apiNote
	 * For example, to obtain a {@code Comparator} that compares {@code Person} objects by their last name
	 * ignoring case differences,
	 *
	 * <pre>{@code
	 *     Comparator<Person> cmp = by(Person::getLastName, String.CASE_INSENSITIVE_ORDER);
	 * }</pre>
	 *
	 * @param  <E> the type of element to be compared
	 * @param  <C> the type of the extracted value to compare by
	 * @param  extractor the function used to extract non null value to compare by
	 * @param  comparator the {@code Comparator} used to compare the sort key
	 * @return comparator that compares by an extracted value using the specified {@code Comparator}
	 * @throws NullPointerException if any argument is null
	 */
	static <E,C> Comparator<E> by(Function<? super E,? extends C> extractor, Comparator<? super C> comparator) {
		noØ(extractor);
		noØ(comparator);
		return (Comparator<E> & Serializable) (a, b) -> comparator.compare(extractor.apply(a),extractor.apply(b));
	}

	/**
	 * Accepts a function that extracts a sort key from a type {@code E}, and
	 * returns a {@code Comparator<E>} that compares by that sort key using
	 * the {@link Comparator} derived from {@link java.util.Comparator#naturalOrder()}.
	 * <p/>
	 * @apiNote convenience method, helpful for cases where the extracted value may be null (and using for example
	 * {@link Comparator#nullsFirst(java.util.Comparator)}).
	 *
	 * @param  <E> the type of element to be compared
	 * @param  <C> the type of the extracted value to compare by
	 * @param  extractor the function used to extract nullable value to compare by
	 * @param  comparatorModifier function that modifies comparing, it is supplied a default
	 *          {@link java.util.Comparator#naturalOrder()} comparator.
	 * @return comparator that compares by an extracted value using the specified {@code Comparator}
	 * @throws NullPointerException if any argument is null
	 */
	static <E,C extends Comparable<? super C>> Comparator<E> by(Function<? super E,? extends C> extractor, Function<Comparator<? super C>, Comparator<? super C>> comparatorModifier) {
		noØ(extractor);
		noØ(comparatorModifier);
		return (Comparator<E> & Serializable) (a, b) -> comparatorModifier.apply(Comparator.naturalOrder()).compare(extractor.apply(a),extractor.apply(b));
	}

	/** @return set of elements of provided collection with no duplicates. Order undefined. */
	static <E> Set<E> noDups(Collection<E> c) {
		return new HashSet<>(c);
	}

	/** @return set of elements of provided collection with no duplicates. Retains order. */
	static <E> Set<E> noDupsStable(Collection<E> c) {
		return new LinkedHashSet<>(c);
	}

/******************************* object -> object *****************************/

	static <IN,OUT> Ƒ1<IN,OUT> mapC(Predicate<? super IN> cond, OUT y, OUT n) {
		return in -> cond.test(in) ? y : n;
	}

	static <OUT> Ƒ1<Boolean,OUT> mapB(OUT y, OUT n) {
		return in -> in ? y : n;
	}

	@SuppressWarnings("unchecked")
	static <NN> Ƒ1<?,NN> mapNulls(NN non_null) {
		noØ(non_null);
		return (Ƒ1) in -> in==null ? non_null : in;
	}


	static <I,O> O mapRef(I value, I i1, O o1) {
		if (value==i1) return o1;
		throw new SwitchException(value);
	}

	static <I,O> O mapRef(I value, I i1, I i2, O o1, O o2) {
		if (value==i1) return o1;
		if (value==i2) return o2;
		throw new SwitchException(value);
	}

	static <I,O> O mapRef(I value, I i1, I i2, I i3, O o1, O o2, O o3) {
		if (value==i1) return o1;
		if (value==i2) return o2;
		if (value==i3) return o3;
		throw new SwitchException(value);
	}

	static <I,O> O mapRef(I value, I i1, I i2, I i3, I i4, O o1, O o2, O o3, O o4) {
		if (value==i1) return o1;
		if (value==i2) return o2;
		if (value==i3) return o3;
		if (value==i4) return o4;
		throw new SwitchException(value);
	}

/* ---------- FUNCTION -> FUNCTION ---------------------------------------------------------------------------------- */

	/** Returns function that never produces null, but returns its input instead. */
	static <I> Ƒ1<I,I> nonNull(Function<I,I> f) {
		return in -> {
			I out = f.apply(in);
			return out==null ? in : out;
		};
	}
	static <I,O> Ƒ1<I,O> nonNull(Function<I,O> f, O or) {
		return in -> {
			O out = f.apply(in);
			return out==null ? or : out;
		};
	}

	/** Faster alternative to {@link #noNull(java.lang.Object...) }. */
	static <O> O noNull(O o1, O o2) {
		return o1!=null ? o1 : o2;
	}

	/** Faster alternative to {@link #noNull(java.lang.Object...) }. */
	static <O> O noNull(O o1, O o2, O o3) {
		return o1!=null ? o1 : o2!=null ? o2 : o3;
	}

	/** Faster alternative to {@link #noNull(java.lang.Object...) }. */
	static <O> O noNull(O o1, O o2, O o3, O o4) {
		return o1!=null ? o1 : o2!=null ? o2 : o3!=null ? o3 : o4;
	}

	/** Returns the first non null object or null if all null. */
	@SafeVarargs
	static <O> O noNull(O... objects) {
		for (O o : objects)
			if (o!=null) return o;
		return null;
	}

	/** Faster alternative to {@link #noNull(java.util.function.Supplier...) }. */
	static <I> I noNull(Supplier<I> supplier1, Supplier<I> supplier2) {
		I i = supplier1.get();
		if (i!=null) return i;
		return supplier2.get();
	}

	/** Faster alternative to {@link #noNull(java.util.function.Supplier...) }. */
	static <I> I noNull(Supplier<I> supplier1, Supplier<I> supplier2, Supplier<I> supplier3) {
		I i = supplier1.get();
		if (i!=null) return i;
		i = supplier2.get();
		if (i!=null) return i;
		return supplier3.get();
	}

	/** Faster alternative to {@link #noNull(java.util.function.Supplier...) }. */
	static <I> I noNull(Supplier<I> supplier1, Supplier<I> supplier2, Supplier<I> supplier3, Supplier<I> supplier4) {
		I i = supplier1.get();
		if (i!=null) return i;
		i = supplier2.get();
		if (i!=null) return i;
		i = supplier3.get();
		if (i!=null) return i;
		return supplier4.get();
	}

	/** Returns the first supplied non null value or null if all null by iterating the suppliers. */
	@SafeVarargs
	static <I> I noNull(Supplier<I>... suppliers) {
		for (Supplier<I> s : suppliers) {
			I i = s.get();
			if (i!=null) return i;
		}
		return null;
	}

	/** Equivalent to {@code noEx(f, null, ecs); }*/
	static <I,O> Ƒ1<I,O> noEx(Function<I,O> f, Class<?>... ecs) {
		return noEx(null, f, ecs);
	}

	/** Equivalent to {@code noEx(f, null, ecs); }*/
	static <I,O> Ƒ1<I,O> noEx(Function<I,O> f, Collection<Class<?>> ecs) {
		return noEx(null, f, ecs);
	}

	/**
	 * Return function functionally equivalent to the one provided, but which
	 * returns null if any of the exception types or subtypes is caught. The
	 * function will never throw any (including runtime) of the specified
	 * exceptions, but will keep throwing other exception types.
	 *
	 * @param f function to wrap
	 * @param or value to return when exception is caught
	 * @param ecs exception types. Any exception that is equal to the type or
	 * subtype of any of the exceptions types will be caught. Using
	 * Exception.class will effectively catch all exception types. Throwable.class
	 * is also an option.
	 */
	static <I,O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Class<?>... ecs) {
		return noEx(or, f, list(ecs));
	}

	/** Equivalent to {@link #noEx(Object, java.util.function.Function, Class[])}. */
	static <I,O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Collection<Class<?>> ecs) {
		return i -> {
			try {
				return f.apply(i);
			} catch(Exception e) {
				for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
				throw e;
			}
		};
	}

	/** Equivalent to {@code noExE(f, null, ecs); }*/
	static <I,O> Ƒ1<I,O> noExE(Ƒ1E<I,O,?> f, Class<?>... ecs) {
		return noExE(null, f, ecs);
	}

	/** Equivalent to {@code noExE(f, null, ecs); }*/
	static <I,O> Ƒ1<I,O> noExE(Ƒ1E<I,O,?> f, Collection<Class<?>> ecs) {
		return noExE(null, f, ecs);
	}

	/**
	 * Return function functionally equivalent to the one provided, but which
	 * returns null if any of the exception types or subtypes is caught. The
	 * function will never throw any (including runtime) of the specified
	 * exceptions, but will keep throwing other exception types.
	 *
	 * @param f function to wrap
	 * @param or value to return when exception is caught
	 * @param ecs exception types. Any exception that is equal to the type or
	 * subtype of any of the exceptions types will be caught. Using
	 * Exception.class will effectively catch all exception types. Throwable.class
	 * is also an option.
	 */
	static <I,O> Ƒ1<I,O> noExE(O or, Ƒ1E<I,O,?> f, Class<?>... ecs) {
		return noExE(or, f, list(ecs));
	}

	/** Equivalent to {@link #noExE(Object, util.functional.Functors.Ƒ1E, Class[])}. */
	static <I,O> Ƒ1<I,O> noExE(O or, Ƒ1E<I,O,?> f, Collection<Class<?>> ecs) {
		return i -> {
			try {
				return f.apply(i);
			} catch(Throwable e) {
				for (Class<?> ec : ecs) if (ec.isAssignableFrom(e.getClass())) return or;
				throw new RuntimeException(e);
			}
		};
	}

/****************************** collection -> list ****************************/

	static <T,E> List<T> flatMapToList(Collection<E> col, Function<E,Collection<T>> mapper) {
		return col.stream().flatMap(e->mapper.apply(e).stream()).collect(Collectors.toList());
	}

	static <T,E> List<T> flatsMapToList(Collection<E> col, Function<E,Stream<T>> mapper) {
		return col.stream().flatMap(mapper).collect(Collectors.toList());
	}

	static <T,E> List<T> flatMapToList(Stream<E> col, Function<E,Collection<T>> mapper) {
		return col.flatMap(e->mapper.apply(e).stream()).collect(Collectors.toList());
	}

	static <T,E> List<T> flatsMapToList(Stream<E> col, Function<E,Stream<T>> mapper) {
		return col.flatMap(mapper).collect(Collectors.toList());
	}

	static <T,E> List<T> flatMapToList(Map<?,E> col, Function<E,Collection<T>> mapper) {
		return col.values().stream().flatMap(e->mapper.apply(e).stream()).collect(Collectors.toList());
	}

	static <T,E> List<T> flatsMapToList(Map<?,E> col, Function<E,Stream<T>> mapper) {
		return col.values().stream().flatMap(mapper).collect(Collectors.toList());
	}

/* ---------- COLLECTION -> OBJECT ------------------------------------------------------------------------------ */

	/**
	 * Converts array to string, joining string representations of the
	 * elements by separator.
	 * @param a array
	 * @param m element to string mapper
	 * @param s delimiter/separator
	 * @return s separated representation of the array
	 */
	static <T> String toS(T[] a, Function<T,String> m, String s) {
		return Stream.of(a).map(m).collect(joining(s));
	}

	/**
	 * Converts array to string, joining string representations of the
	 * elements by ', '.
	 * @param a array
	 * @return comma separated string representation of the array
	 */
	@SafeVarargs
	static <T> String toS(String s, T... a) {
		return stream(a).map(Objects::toString).collect(joining(s));
	}

	/**
	 * Converts collection to string, joining string representations of the
	 * elements by separator
	 *
	 * @param c collection
	 * @param m element to string mapper
	 * @param s delimiter/separator
	 * @return s separated representation of the collection
	 */
	static <T> String toS(Collection<T> c, Function<T,String> m, String s) {
		return c.stream().map(m).collect(joining(s));
	}

	/**
	 * Joins collection of strings to string using delimiter
	 *
	 * @param c collection
	 * @param s delimiter/separator
	 * @return s separated representation of the collection
	 */
	static String toS(Collection<String> c, String s) {
		return c.stream().collect(joining(s));
	}

	/**
	 * Converts collection to string, joining string representations of the
	 * elements by ', '.
	 *
	 * @param c collection
	 * @param m element to string mapper
	 * @return comma separated string representation of the objects in the
	 * collection
	 */
	static <T> String toS(Collection<T> c, Function<T,String> m) {
		return c.stream().map(m).collect(toCSList);
	}

	/**
	 * Equivalent to {@link #toS(java.util.Collection, java.util.function.Function)}.
	 *
	 * @param c collection
	 * @return comma separated string representations of the objects in the
	 * collection
	 */
	static <T> String toS(Collection<T> c) {
		return c.stream().map(Objects::toString).collect(toCSList);
	}

	static <C extends Comparable<? super C>> C min(C a, C b) {
		return a.compareTo(b)<0 ? a : b;
	}

	static <C extends Comparable<? super C>> C max(C a, C b) {
		return a.compareTo(b)>0 ? a : b;
	}

	/**
	 * Specialization of {@link #minBy(java.util.Collection, java.lang.Comparable, util.functional.Functors.Ƒ1)}
	 * with atMost parameter null - maximum value.
	 *
	 * @return optional with the minimum element or empty optional if collection contains no
	 * element smaller than required.
	 */
	static <V,C extends Comparable<? super C>> Optional<V> minBy(Collection<V> c, Ƒ1<? super V,C> by) {
		return minBy(c, null, by);
	}

	/**
	 * Returns minimal element from collection by given criteria. If multiple elements compare
	 * equal, the 1st is considered minimal.
	 *
	 * @param c collection to find minimal element of
	 * @param atMost the maximal value minimal element must have to be considered minimal, null
	 * represents maximul value - any element is smaller than null
	 * @return optional with the minimum element or empty optional if collection contains no
	 * element smaller than required.
	 */
	static <V,C extends Comparable<? super C>> Optional<V> minBy(Collection<V> c, C atMost, Ƒ1<? super V,C> by) {
		V minv = null;
		C minc = atMost;
		for (V v : c) {
			C vc = by.apply(v);
			if (minc==null || vc.compareTo(minc)<0) {
				minv = v;
				minc = vc;
			}
		}
		return Optional.ofNullable(minv);
	}

	/**
	 * Specialization of {@link #maxBy(java.util.Collection, java.lang.Comparable, util.functional.Functors.Ƒ1)}
	 * with atLeast parameter null - maximum value.
	 *
	 * @return optional with the maximal element or empty optional if collection contains no
	 * element bigger than required.
	 */
	static <V,C extends Comparable<? super C>> Optional<V> maxBy(Collection<V> c, Ƒ1<? super V,C> by) {
		return maxBy(c, null, by);
	}

	/**
	 * Returns maximal element from collection by given criteria. If multiple elements compare
	 * equal, the 1st is considered minimal.
	 *
	 * @param c collection to find minimal element of
	 * @param atleast the minimal value maximal element must have to be considered maximal, null
	 * represents minimal value - any element is more than null
	 * @return optional with the maximal element or empty optional if collection contains no
	 * element bigger than required.
	 */
	static <V,C extends Comparable<? super C>> Optional<V> maxBy(Collection<V> c, C atleast, Ƒ1<? super V,C> by) {
		V maxv = null;
		C maxc = atleast;
		for (V v : c) {
			C vc = by.apply(v);
			if (maxc==null || vc.compareTo(maxc)>0) {
				maxv = v;
				maxc = vc;
			}
		}
		return Optional.ofNullable(maxv);
	}

	/**
	 * Returns minimal element from the array using given comparator.
	 * Returns supplied value if it is the smallest, or array is empty.
	 */
	@SafeVarargs
	static <V> V min(V min, Comparator<V> cmp, V... c) {
		return min(Stream.of(c), min, cmp);
	}

	/**
	 * Returns minimal element from the collection using given comparator.
	 * Returns supplied value if it is the smallest, or collection is empty.
	 */
	static <V> V min(Collection<V> c, V min, Comparator<V> cmp) {
		return min(c.stream(), min, cmp);
	}

	/**
	 * Returns minimal element from the stream using {@link Comparable#compareTo(java.lang.Object)}.
	 * Returns supplied value if it is the smallest, or stream is empty.
	 */
	static <V extends Comparable<? super V>> V min(Stream<V> c, V min) {
		return min(c, min, Comparable::compareTo);
	}

	/**
	 * Returns minimal element from the stream using given comparator.
	 * Returns supplied value if it is the smallest, or stream is empty.
	 */
	static <V> V min(Stream<V> c, V min, Comparator<V> cmp) {
		return c.reduce(min, (t,u) -> cmp.compare(t, u)<0 ? t : u);
	}

	/**
	 * Returns maximal element from the array using given comparator.
	 * Returns supplied value if it is the smallest, or array is empty.
	 */
	@SafeVarargs
	static <V> V max(V max, Comparator<V> cmp, V... c) {
		return max(Stream.of(c), max, cmp);
	}

	/**
	 * Returns maximal element from the collection using given comparator.
	 * Returns supplied value if it is the smallest, or collection is empty.
	 */
	static <V> V max(Collection<V> c, V max, Comparator<V> cmp) {
		return max(c.stream(), max, cmp);
	}

	/**
	 * Returns maximal element from the stream using given comparator.
	 * Returns supplied value if it is the smallest, or stream is empty.
	 */
	static <V extends Comparable<? super V>> V max(Stream<V> c, V max) {
		return max(c, max, Comparable::compareTo);
	}

	/**
	 * Returns maximal element from the stream using given comparator.
	 * Returns supplied value if it is the smallest, or stream is empty.
	 */
	static <V> V max(Stream<V> c, V max, Comparator<V> cmp) {
		return c.reduce(max, (t,u) -> cmp.compare(t, u)>0 ? t : u);
	}

	/**
	 * Checks whether all elements of the list are equal by some property
	 * obtained using specified transformation.
	 * <p/>
	 * For example checking whether all lists have the same size:
	 * <pre>{@code equalBy(lists,List::size) }</pre>
	 *
	 * @return true if transformation of each element in the list produces equal
	 * result.
	 */
	static <V,R> boolean equalBy(List<V> o, Function<V,R> by) {
		if (o.size()<2) return true;
		R r = by.apply(o.get(0));
		for (int i=1; i<o.size(); i++)
			if (!r.equals(by.apply(o.get(i)))) return false;
		return true;
	}

	/**
	 * Assuming a map of lists, i-slice is a list i-th elements in every list.
	 * @return i-th slice of the map m
	 */
	static <K,T> Map<K,T> mapSlice(Map<K,List<T>> m, int i) {
		Map<K,T> o = new HashMap<>();
		m.entrySet().forEach(e -> o.put(e.getKey(), e.getValue().get(i)));
		return o;
	}

	/**
	 * Creates map which remaps all elements to different key, using key mapper
	 * function.
	 * @return new map containing all elements mapped to transformed keys
	 */
	static <K1,K2,V> Map<K2,V> mapKeys(Map<K1,V> m, Function<K1,K2>f) {
		Map<K2,V> o = new HashMap<>();
		m.forEach((key,val) -> o.put(f.apply(key), val));
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

	/**
	 * Functional equivalent of a for loop. After each (last included) cycle the thread sleeps for
	 * given period. If thread interrupts, the cycle ends immediately.
	 * <p/>
	 * The cycle will also end if action in any cycle throws {@link InterruptedException}. Use it
	 * instead of the {@code break} expression.
	 *
	 * @param period time for thread to wait after each loop. If negative or 0, no waiting occurs.
	 * @param items items to execute action for, the order of execution will be the same as if using
	 * normal for loop
	 * @param action action that executes once per item. It can throw exception to signal loop break
	 */
	static <T> void forEachAfter(long period, Collection<T> items, ƑEC<T,InterruptedException> action) {
		for (T item : items) {
			try {
				action.apply(item);
				if (period>0) Thread.sleep(period);
			} catch (InterruptedException ex) {
				break;
			}
		}
	}

	/** Loops over both lists simultaneously. Must be of the same size. */
	static <A,B> void forEachBoth(List<A> a, List<B> b, BiConsumer<A,B> action) {
		throwIfNot(a.size()==b.size());
		for (int i=0; i<a.size(); i++)
			action.accept(a.get(i), b.get(i));
	}

	/** Loops over both arrays simultaneously. Must be of the same size. */
	static <A,B> void forEachBoth(A[] a, B[] b, BiConsumer<A,B> action) {
		throwIfNot(a.length==b.length);
		for (int i=0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	static void forEachBoth(int[] a, int[] b, IntBiConsumer action) {
		throwIfNot(a.length==b.length);
		for (int i=0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	static void forEachBoth(long[] a, long[] b, LongBiConsumer action) {
		throwIfNot(a.length==b.length);
		for (int i=0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	static void forEachBoth(double[] a, double[] b, DoubleBiConsumer action) {
		throwIfNot(a.length==b.length);
		for (int i=0; i<a.length; i++)
			action.accept(a[i], b[i]);
	}

	/** Loops over list zipping index with each item. Index starts at 0. */
	static <T> void forEachWithI(Collection<T> c, BiConsumer<Integer,T> action) {
		int i=0;
		for (T item : c) {
			action.accept(i, item);
			i++;
		}
	}

	/** Loops over array zipping index with each item. Index starts at 0. */
	static <T> void forEachWithI(T[] c, BiConsumer<Integer,T> action) {
		int i=0;
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
	static <E> void forEachPair(Collection<E> c, BiConsumer<? super E, ? super E> action) {
		forEachPair(c, c, action);
	}

	/** Loops over cartesian product C x C of a collection C, ignoring symmetric elements (i,j) (j;i). */
	static <E> void forEachCartesianHalf(Collection<E> c, BiConsumer<? super E, ? super E> action) {
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
				if (i>0)i--;
				if (i==0) action.accept(e,t);
			}
			j++;
		}
	}

	/**
	 * Loops over cartesian product C x C of a collection C, ignoring symmetric elements (i,j)(j;i) and
	 * self elements (i,i).
	 */
	static <E> void forEachCartesianHalfNoSelf(Collection<E> c, BiConsumer<? super E, ? super E> action) {
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
				if (i==0) action.accept(e,t);
				if (i>0)i--;
			}
			j++;
		}
	}

	/** Loops over cartesian product C1 x C2 of collections C1, C2.
	 * @param action 1st parameter is element from 1st collection, 2nd parameter is el. from 2nd
	 */
	static <E,T> void forEachPair(Collection<E> c1, Collection<T> c2, BiConsumer<? super E, ? super T> action) {
		for (E e : c1) for (T t : c2) action.accept(e,t);
	}

	/** Loops over list zipping each item with a companion derived from it. */
	static <T,W> void forEachWith(Collection<T> c, Function<T,W> toCompanion, BiConsumer<? super T, ? super W> action) {
		for (T t : c)
			action.accept(t, toCompanion.apply(t));
	}

	/**
	 * Returns stream of elements mapped by the mapper from index-element pairs
	 * of specified collection. Indexes start at 0.
	 * <p/>
	 * Functionally equivalent to: List.stream().mapB(item->new Pair(item,list.indexOf(item))).mapB(pair->mapper.mapB(p))
	 * but avoiding the notion of a Pair or Touple, and without any collection
	 * traversal to get indexes.
	 *
	 * @param <T> element type
	 * @param <R> result type
	 * @param c collection to iterate
	 * @param mapper mapper of the element (zipped with its index as 1st parameter) to a transformed one
	 * @return stream of mapped elements
	 */
	static <T,R> Stream<R> forEachIStream(Collection<T> c, BiFunction<Integer,T,R> mapper) {
		int i=0;
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
	static <T,R> Stream<R> forEachIRStream(List<T> c, BiFunction<Integer,T,R> mapper) {
		int size = c.size();
		Stream.Builder<R> b = Stream.builder();
		for (int i = 1; i<=size; i++)
			b.accept(mapper.apply(i-1, c.get(size-i)));
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
	 * @param initial_val  for example: 0
	 * @param operation for example: number -> number++
	 * @param mapper maps the key-object pair into another object
	 *
	 * @return stream of mapped values by a mapper out of key-element pairs
	 */
	static <I,T,R> Stream<R> forEachIStream(Collection<T> c, I initial_val, Function<I,I> operation, BiFunction<I,T,R> mapper) {
		I i = initial_val;
		Stream.Builder<R> b = Stream.builder();
		for (T item : c) {
			b.accept(mapper.apply(i, item));
			i = operation.apply(i);
		}
		return b.build();
	}

	static <R> Stream<R> forEachInLine(double fromX, double fromY, double toX, double toY, long count, BiFunction<Double,Double,R> mapper) {
		throwIf(count<0);
		return forEachInLineBy(fromX, fromY, count<=1 ? 0 : (toX-fromX)/(count-1), count<=1 ? 0 : (toY-fromY)/(count-1), count, mapper);
	}

	static <R> Stream<R> forEachInLineBy(double x, double y, double byX, double byY, long count, BiFunction<Double,Double,R> mapper) {
		throwIf(count<0);
		return IntStream.iterate(0, i -> i<=count, i -> i+1)
				.mapToObj(i -> mapper.apply(x+i*byX, y+i*byY));
	}

	static <R> StreamEx<R> forEachOnCircle(long count, TriFunction<Double,Double,Double,R> mapper) {
		throwIf(count<0);
		return forEachOnCircle(0, 0, 1, count, mapper);
	}

	static <R> StreamEx<R> forEachOnCircleBy(double x, double y, double by, long count, TriFunction<Double,Double,Double,R> mapper) {
		throwIf(count<0);
		double circumference = by*count;
		double radius = circumference/(2*PI);
		return forEachOnCircle(x, y, radius, count, mapper);
	}

	static <R> StreamEx<R> forEachOnCircle(double x, double y, double radius, long count, TriFunction<Double,Double,Double,R> mapper) {
		throwIf(count<0);
		return DoubleStreamEx.iterate(0, a-> a+2*PI/count)
				.limit(count)
				.mapToObj(a -> mapper.apply(x+radius*cos(a), y+radius*sin(a), a));
	}

/****************************** () -> collection ******************************/

	static <T> Stream<Tuple2<Integer,T>> toIndexedStream(Collection<T> c) {
		int i=0;
		Stream.Builder<Tuple2<Integer,T>> b = Stream.builder();
		for (T item : c) {
			b.accept(tuple(i, item));
			i++;
		}
		return b.build();
	}

	/** Creates an array filled with provided elements. The array's length will equal element count. */
	@SafeVarargs
	static <T> T[] array(T... elements) {
		return elements;
	}

	/**
	 * Equivalent, but optimized version of {@link #setRO(Object[])}.
	 */
	@SuppressWarnings("unchecked")
	static <T> Set<T> setRO() {
		return EMPTY_SET;
	}

	/**
	 * Equivalent, but optimized version of {@link #setRO(Object[])}.
	 */
	static <T> Set<T> setRO(T t) {
		return singleton(t);
	}

	/**
	 * Returns unmodifiable set containing specified elements.
	 * Optimized:
	 * <ul>
	 * <li> if zero parameters - {@link Collections#EMPTY_SET}
	 * <li> if 1 parameters - {@link Collections#singleton(java.lang.Object)}
	 * <li> else parameters - {@link Collections#unmodifiableSet(java.util.Set)} using a new {@link java.util.HashSet}
	 * </ul>
	 */
	@SuppressWarnings("unchecked")
	static <T> Set<T> setRO(T... ts) {
		int size = ts.length;
		if (size==0) return EMPTY_SET;
		if (size==1) return singleton(ts[0]);
		else return stream(ts).toSetAndThen(Collections::unmodifiableSet);
	}

	@SafeVarargs
	static <T> Set<T> set(T... ts) {
		return stream(ts).toCollection(() -> new HashSet<>(ts.length));
	}

	/** Returns modifiable list containing specified elements. */
	@SafeVarargs
	static <T> List<T> list(T... ts) {
		return stream(ts).toCollection(() -> new ArrayList<>(ts.length));
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

	/** Returns modifiable list containing elements in both specified collection and array. */
	@SafeVarargs
	static <T> List<T> list(Collection<T> a, T... ts) {
		return stream(ts).append(ts).toCollection(() -> new ArrayList<>(ts.length));
	}

	/** Returns modifiable list containing specified element i times. */
	static <T> List<T> list(int i, T a) {
		List<T> l = new ArrayList<>(i);
		for (int j=0; j<i; j++) l.add(a);
		return l;
	}

	/** Returns modifiable list containing element supplied by specified supplier i times. */
	static <T> List<T> list(int i, Supplier<T> factory) {
		List<T> l = new ArrayList<>(i);
		for (int j=0; j<i; j++) l.add(factory.get());
		return l;
	}

	/**
	 * Returns modifiable list containing element supplied by specified supplier i times. Integer
	 * params range from 1 to i;
	 */
	static <T> List<T> listF(int i, Function<Integer,T> factory) {
		List<T> l = new ArrayList<>(i);
		for (int j=0; j<i; j++) l.add(factory.apply(j));
		return l;
	}


	static <T> StreamEx<T> stream() {
		return StreamEx.empty();
	}

	static <T> StreamEx<T> stream(T t) {
		return StreamEx.of(t);
	}

	@SafeVarargs
	static <T> StreamEx<T> stream(T... t) {
		return t.length==0 ? StreamEx.empty() : StreamEx.of(t);
	}

	static <K,V> EntryStream<K,V> stream(Map<K,V> map) {
		return EntryStream.of(map);
	}

	static <T> StreamEx<T> stream(Stream<? extends T> s1, Stream<? extends T> s2) {
		return StreamEx.of(Stream.concat(s1,s2));
	}

	static <T> StreamEx<T> stream(T o, Stream<T> t) {
		return StreamEx.of(o).append(t);
	}

	static <T> StreamEx<T> stream(T o, Collection<T> t) {
		return StreamEx.of(o).append(t);
	}

	static <T> StreamEx<T> stream(Collection<T> t) {
		return StreamEx.of(t);
	}

	static <T> StreamEx<T> stream(Iterator<T>  t) {
		return StreamEx.of(t);
	}
	static <T> StreamEx<T> stream(Enumeration<T> t) {
		return StreamEx.of(t);
	}

	static <A,B,R> Stream<R> streamBi(A[] a, B[] b, Ƒ2<A,B,R> zipper) {
		throwIfNot(a.length==b.length);
		Stream.Builder<R> builder = Stream.builder();
		for (int i=0; i<a.length; i++)
			builder.accept(zipper.apply(a[i], b[i]));
		return builder.build();
	}

	/** @return stream equivalent to a for loop */
	static <T> Stream<T> stream(T seed, Predicate<T> cond, UnaryOperator<T> op) {
		Stream.Builder<T> b = Stream.builder();
		for (T t = seed; cond.test(t); t=op.apply(t)) b.accept(t);
		return b.build();
	}

	/** Creates stream of {@link Integer} in range from-to inclusive. */
	static Stream<Integer> range(int fromInclusive, int toInclusive) {
		Stream.Builder<Integer> b = Stream.builder();
		for (int i=fromInclusive; i<=toInclusive; i++) b.accept(i);
		return b.build();
	}

	static Stream<Double> range(double fromInclusive, double toInclusive) {
		Stream.Builder<Double> b = Stream.builder();
		for (double i=fromInclusive; i<=toInclusive; i++) b.accept(i);
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
	static <T,R> List<R> map(T[] a, Function<T,R> m) {
		return Stream.of(a).map(m).collect(toList());
	}

	/** Maps collection. Returns list. Source remains unchanged. */
	static <T,R> List<R> map(Collection<? extends T> c, Function<? super T,? extends R> m) {
		return c.stream().map(m).collect(toList());
	}

	/** Filters and then maps array. Returns list. Source remains unchanged. */
	@SafeVarargs
	static <T,R> List<R> filterMap(Predicate<? super T> f, Function<? super T,? extends R> m, T... a ) {
		return Stream.of(a).filter(f).map(m).collect(toList());
	}

	/** Filters and then maps collection. Returns list. Source remains unchanged. */
	static <T,R> List<R> filterMap(Collection<T> c, Predicate<? super T> f, Function<? super T,? extends R> m) {
		return c.stream().filter(f).map(m).collect(toList());
	}

	/** Filters and then maps stream. Returns list. */
	static <T,R> List<R> filterMap(Stream<T> c, Predicate<? super T> f, Function<? super T,? extends R> m) {
		return c.filter(f).map(m).collect(toList());
	}

	/**
	 * Alternative to {@link Stream#collect(java.util.stream.Collector)} and
	 * {@link Collectors#groupingBy(java.util.function.Function) }, doesn't support
	 * null keys (even for maps that allow them, and even if it has been specifically supplied for
	 * the collector to use)!
	 */
	static <E,K> Map<K,List<E>> groupBy(Stream<E> s, Function<E,K> key_extractor) {
		Map<K,List<E>> m = new HashMap<>();
		s.forEach(e -> m.computeIfAbsent(key_extractor.apply(e), key -> new ArrayList<>()).add(e));
		return m;
	}

	/** Convenience method. */
	@SafeVarargs
	static <K,V,E> Map<K,E> toMap(Function<V,K> key_extractor, Function<V,E> val_extractor, V... c) {
		return Stream.of(c).collect(Collectors.toMap(key_extractor, val_extractor));
	}

	/** Convenience method. */
	static <K,V,E> Map<K,E> toMap(Collection<V> c, Function<V,K> key_extractor, Function<V,E> val_extractor) {
		return c.stream().collect(Collectors.toMap(key_extractor, val_extractor));
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
	static List<String> splitToNonemptyOrNull(String txt, String regex, int i) {
		if (txt.isEmpty()) return EMPTY_LIST;
		String[] a = txt.split(regex, i);
		if (a.length<i) return null;
		for (String s : a)
			if (s==null || s.isEmpty())
				return null;
		return Stream.of(a).collect(toList());
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
	static int findFirstEmptyKey(Map<Integer, ?> map, int from) {
		return IntStreamEx.iterate(from, i -> i+1).findFirst(i -> !map.containsKey(i)).getAsInt();
	}

	static int findFirstInt(int from, IntPredicate condition) {
		return IntStream.iterate(from, i -> i+1).filter(condition).findFirst().getAsInt();
	}

}