/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.util.Callback;

import util.collections.Tuple2;
import util.functional.Functors.Ƒ1;
import util.functional.Functors.Ƒ1E;
import util.functional.Functors.Ƒ2;
import util.functional.Functors.ƑEC;
import util.functional.Functors.ƑP;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static util.collections.Tuples.tuple;
import static util.dev.Util.noØ;
import static util.dev.Util.yes;

/**
 *
 * @author Plutonium_
 */
public class Util {

    /** Function returning the inputs. */
    public static final Ƒ1<? super Object,Object> IDENTITY = o -> o;

    /** Predicate returning true iff object is not null. */
    public static final ƑP<? super Object> ISNTØ = Objects::nonNull;

    /** Predicate returning true iff object is null. */
    public static final ƑP<? super Object> ISØ = Objects::isNull;

    /** Predicate returning true. Matches every object. */
    public static final ƑP<? super Object> IS = o -> true;

    /** Predicate returning false. Matches no object. */
    public static final ƑP<? super Object> ISNT = o -> false;

    /** Comparator returning 0. Produces no order change. */
    public static Comparator SAME = (a,b) -> 0;

    /**
     * Returns true if the arguments are equal to each other and false otherwise.
     * If any argument is null, false is returned.
     * Otherwise, equality is determined by using the equals method of the first argument.
     */
    public static boolean equalNonNull(Object a, Object b) {
        return a!=null && b!=null && a.equals(b);
    }

    /**
     * Returns true if the arguments are equal to each other and false otherwise.
     * If both arguments are null, true is returned and if exactly one argument is null, false is
     * returned.
     * Otherwise, equality is determined by using the equals method of the first argument.
     */
    public static boolean equalNull(Object a, Object b) {
        return a == b || (a!=null && a.equals(b));
    }

    /**  */
    public static <E> boolean isIn(E o, E... es) {
        for(E e : es)
            if(o.equals(e))
                return true;
        return false;
    }

    public static <E> boolean isIn(E o, Collection<E> es) {
        for(E e : es)
            if(o.equals(e))
                return true;
        return false;
    }

    public static <E> boolean isInR(E o, E... es) {
        for(E e : es)
            if(o == e)
                return true;
        return false;
    }

    public static <E> boolean isInR(E o, Collection<E> es) {
        for(E e : es)
            if(o == e)
                return true;
        return false;
    }

    public static <E> boolean isInR(E o, Collection<E> es, E... ess) {
        return isInR(o, es) || isInR(o, ess);
    }

    public static <E> boolean isAll(E o, Predicate<E>... ps) {
        boolean b = true;
        for(Predicate<E> p : ps)
            b &= p.test(o);
        return b;
    }
    public static <E> boolean isAny(E o, Predicate<E>... ps) {
        boolean b = false;
        for(Predicate<E> p : ps)
            b |= p.test(o);
        return b;
    }
    public static <E> boolean isNone(E o, Predicate<E>... ps) {
        return !isAll(o, ps);
    }

    /** Repeat action n times. */
    public static void repeat(int n, Runnable action) {
        for(int x=0; x<n; x++) action.run();
    }

    /** Repeat action n times. Action takes the index of execution as parameter starting from 0. */
    public static void repeat(int n, IntConsumer action) {
        for(int x=0; x<n; x++) action.accept(x);
    }


    /** Runnable that does nothing.  () -> {}; */
    public static final Runnable do_NOTHING = () -> {};

    /**
     * Function transforming object into its string representation by invoking
     * its toString() method or "null" if null.
     */
    public static final Function<Object,String> toString = Objects::toString;

    /** Simple Collector concatenating Strings to coma separated list (CSList)
     *  by delimiter ", ". */
    public static final Collector<CharSequence,?,String> toCSList = Collectors.joining(", ");

    /** Comparator utilizing Comparable.compareTo() of the Comparables. */
    public static final Comparator<Comparable> COMPARATOR_DEF = (a,b) -> a.compareTo(b);

    /** String comparator utilizing String.compareTo() of the Strings */
    public static final Comparator<String> COMPARATOR_STR = (a,b) -> a.compareTo(b);

    /** String comparator utilizing String.compareToIgnoreCase(). */
    public static final Comparator<String> COMPARATOR_STR_CASELESS = (a,b) -> a.compareToIgnoreCase(b);

    /**
     * Creates comparator comparing E elements by derived {@link Comparable}, for
     * example a Comparable field, obtained by the converter.
     * Utilizes Comparable.compareTo().
     * <p>
     * Easy and concise way to compare objects without code duplication
     * <p>
     * This method is generic Comparator factory producing comparators comparing
     * the obtained result of the comparable supplier.
     *
     * @param toStr E to Comparable mapper, derives Comparable from E.
     */
    public static<E,C extends Comparable<? super C>> Comparator<E> by(Callback<E,C> toStr) {
        return (a,b) -> toStr.call(a).compareTo(toStr.call(b));
    }

    /**
     * Creates comparator comparing E elements by their string representation
     * obtained by provided converter. Utilizes String.compareToIgnoreCase().
     * <p>
     * Easy and concise way to compare objects without code duplication.
     *
     * @param cmpGetter E to String mapper, derives String from E.
     * the object.
     */
    public static<E> Comparator<E> byNC(Callback<E,String> toStr) {
        return (a,b) -> toStr.call(a).compareToIgnoreCase(toStr.call(b));
    }

    /** @return set of elements of provided collection with no duplicates. Order undefined. */
    public static<E> Set<E> noDups(Collection<E> c) {
        return new HashSet(c);
    }

    /** @return set of elements of provided collection with no duplicates. Retains order. */
    public static<E> Set<E> noDupsStable(Collection<E> c) {
        return new LinkedHashSet(c);
    }



/******************************* object -> object *****************************/

    public static <IN,OUT> Ƒ1<IN,OUT> mapC(Predicate<? super IN> cond, OUT y, OUT n) {
        return in -> cond.test(in) ? y : n;
    }
    public static <OUT> Ƒ1<Boolean,OUT> mapB(OUT y, OUT n) {
        return in -> in ? y : n;
    }
    public static <NN> Ƒ1<? extends Object,NN> mapNulls(NN non_null) {
        noØ(non_null);
        return (Ƒ1) in -> in==null ? non_null : in;
    }

/***************************** function -> function ***************************/

    /** Returns function that never produces null, but returns its input instead. */
    public static <I> Ƒ1<I,I> nonNull(Function<I,I> f) {
        return in -> {
            I out = f.apply(in);
            return out==null ? in : out;
        };
    }
    public static <I,O> Ƒ1<I,O> nonNull(Function<I,O> f, O or) {
        return in -> {
            O out = f.apply(in);
            return out==null ? or : out;
        };
    }

    /** Faster alternative to {@link #noNull(java.lang.Object...) }. */
    public static <O> O noNull(O o1, O o2) {
        return o1!=null ? o1 : o2;
    }

    /** Faster alternative to {@link #noNull(java.lang.Object...) }. */
    public static <O> O noNull(O o1, O o2, O o3) {
        return o1!=null ? o1 : o2!=null ? o2 : o3;
    }

    /** Faster alternative to {@link #noNull(java.lang.Object...) }. */
    public static <O> O noNull(O o1, O o2, O o3, O o4) {
        return o1!=null ? o1 : o2!=null ? o2 : o3!=null ? o3 : o4;
    }

    /** Returns the first non null object or null if all null. */
    public static <O> O noNull(O... objects) {
        for(O o : objects)
            if(o!=null) return o;
        return null;
    }

    /** Faster alternative to {@link #noNull(java.util.function.Supplier...) }. */
    public static <I> I noNull(Supplier<I> supplier1, Supplier<I> supplier2) {
        I i = supplier1.get();
        if(i!=null) return i;
        return supplier2.get();
    }

    /** Faster alternative to {@link #noNull(java.util.function.Supplier...) }. */
    public static <I> I noNull(Supplier<I> supplier1, Supplier<I> supplier2, Supplier<I> supplier3) {
        I i = supplier1.get();
        if(i!=null) return i;
        i = supplier2.get();
        if(i!=null) return i;
        return supplier3.get();
    }

    /** Faster alternative to {@link #noNull(java.util.function.Supplier...) }. */
    public static <I> I noNull(Supplier<I> supplier1, Supplier<I> supplier2, Supplier<I> supplier3, Supplier<I> supplier4) {
        I i = supplier1.get();
        if(i!=null) return i;
        i = supplier2.get();
        if(i!=null) return i;
        i = supplier3.get();
        if(i!=null) return i;
        return supplier4.get();
    }

    /** Returns the first supplied non null value or null if all null by iterating the suppliers. */
    public static <I> I noNull(Supplier<I>... suppliers) {
        for(Supplier<I> s : suppliers) {
            I i = s.get();
            if(i!=null) return i;
        }
        return null;
    }

    /** Equivalent to {@code noEx(f, null, ecs); }*/
    public static <I,O> Ƒ1<I,O> noEx(Function<I,O> f, Class<?>... ecs) {
        return noEx(null, f, ecs);
    }
    /** Equivalent to {@code noEx(f, null, ecs); }*/
    public static <I,O> Ƒ1<I,O> noEx(Function<I,O> f, Collection<Class<?>> ecs) {
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
    public static <I,O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Class<?>... ecs) {
        return noEx(or, f, list(ecs));
    }

    /** Equivalent to {@link #noEx(java.util.function.Function, java.lang.Object, java.lang.Class...)}*/
    public static <I,O> Ƒ1<I,O> noEx(O or, Function<I,O> f, Collection<Class<?>> ecs) {
        return i -> {
            try {
                return f.apply(i);
            } catch(Exception e) {
                for(Class<?> ec : ecs) if(ec.isAssignableFrom(e.getClass())) return or;
                throw e;
            }
        };
    }

    /** Equivalent to {@code noExE(f, null, ecs); }*/
    public static <I,O> Ƒ1<I,O> noExE(Ƒ1E<I,O,?> f, Class<?>... ecs) {
        return noExE(null, f, ecs);
    }
    /** Equivalent to {@code noExE(f, null, ecs); }*/
    public static <I,O> Ƒ1<I,O> noExE(Ƒ1E<I,O,?> f, Collection<Class<?>> ecs) {
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
    public static <I,O> Ƒ1<I,O> noExE(O or, Ƒ1E<I,O,?> f, Class<?>... ecs) {
        return noExE(or, f, list(ecs));
    }

    /** Equivalent to {@link #noExE(java.util.function.Function, java.lang.Object, java.lang.Class...)}*/
    public static <I,O> Ƒ1<I,O> noExE(O or, Ƒ1E<I,O,?> f, Collection<Class<?>> ecs) {
        return i -> {
            try {
                return f.apply(i);
            } catch(Exception e) {
                for(Class<?> ec : ecs) if(ec.isAssignableFrom(e.getClass())) return or;
                throw new RuntimeException(e);
            }
        };
    }

/****************************** collection -> list ****************************/

    public static<T,E> List<T> flatMapToList(Collection<E> col, Function<E,Collection<T>> mapper) {
        return col.stream().flatMap(e->mapper.apply(e).stream()).collect(Collectors.toList());
    }

    public static<T,E> List<T> flatsMapToList(Collection<E> col, Function<E,Stream<T>> mapper) {
        return col.stream().flatMap(mapper).collect(Collectors.toList());
    }

    public static<T,E> List<T> flatMapToList(Stream<E> col, Function<E,Collection<T>> mapper) {
        return col.flatMap(e->mapper.apply(e).stream()).collect(Collectors.toList());
    }

    public static<T,E> List<T> flatsMapToList(Stream<E> col, Function<E,Stream<T>> mapper) {
        return col.flatMap(mapper).collect(Collectors.toList());
    }

    public static<T,E> List<T> flatMapToList(Map<?,E> col, Function<E,Collection<T>> mapper) {
        return col.values().stream().flatMap(e->mapper.apply(e).stream()).collect(Collectors.toList());
    }

    public static<T,E> List<T> flatsMapToList(Map<?,E> col, Function<E,Stream<T>> mapper) {
        return col.values().stream().flatMap(mapper).collect(Collectors.toList());
    }

/***************************** collection -> object ***************************/

    /**
     * Converts array to string, joining string representations of the
     * elements by separator.
     * @param a array
     * @param m element to string mapper
     * @param s delimiter/separator
     * @return s separated representation of the array
     */
    public static<T> String toS(T[] a, Function<T,String> m, String s) {
        return Stream.of(a).map(m).collect(joining(s));
    }

    /**
     * Converts array to string, joining string representations of the
     * elements by ', '.
     * @param a array
     * @return comma separated string representation of the array
     */
    public static<T> String toS(String s, T... a) {
        return Stream.of(a).map(Object::toString).collect(joining(s));
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
    public static<T> String toS(Collection<T> c, Function<T,String> m, String s) {
        return c.stream().map(m).collect(joining(s));
    }

    /**
     * Joins collection of strings to string using delimiter
     *
     * @param c collection
     * @param s delimiter/separator
     * @return s separated representation of the collection
     */
    public static String toS(Collection<String> c, String s) {
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
    public static<T> String toS(Collection<T> c, Function<T,String> m) {
        return c.stream().map(m).collect(toCSList);
    }

    /**
     * Equivalent to {@link #toS(java.util.Collection, java.util.function.Function)}.
     *
     * @param c collection
     * @return comma separated string representations of the objects in the
     * collection
     */
    public static<T> String toS(Collection<T> c) {
        return c.stream().map(toString).collect(toCSList);
    }

    public static<E> E findOrDie(Collection<E> c, Predicate<E> filter) {
        for(E i : c) if(filter.test(i)) return i;
        throw new RuntimeException("Collection does not have the element.");
    }
    public static<E> Optional<E> find(Collection<E> c, Predicate<E> filter) {
        for(E i : c) if(filter.test(i)) return Optional.of(i);
        return Optional.empty();
    }



    public static <C extends Comparable> C min(C a, C b) {
        return a.compareTo(b)<0 ? a : b;
    }

    public static <C extends Comparable> C max(C a, C b) {
        return a.compareTo(b)>0 ? a : b;
    }

    /** Collector returning the minimum element. */
    public static <V,C extends Comparable<C>> Collector<V, ?, Optional<V>> minBy(Ƒ1<? super V,C> by) {
        return Collectors.reducing(BinaryOperator.minBy(by(by)));
    }

    /** Collector returning the maximum element. */
    public static <V,C extends Comparable<C>> Collector<V, ?, Optional<V>> maxBy(Ƒ1<? super V,C> by) {
        return Collectors.reducing(BinaryOperator.maxBy(by(by)));
    }

    /** Collector returning the minimum element. */
    public static <V,C extends Comparable<C>> Collector<V, ?, V> minBy(V identity, Ƒ1<? super V,C> by) {
        return Collectors.reducing(identity, BinaryOperator.minBy(by(by)));
    }

    /** Collector returning the maximum element. */
    public static <V,C extends Comparable<C>> Collector<V, ?, V> maxBy(V identity, Ƒ1<? super V,C> by) {
        return Collectors.reducing(identity, BinaryOperator.maxBy(by(by)));
    }


    /**
     * Specialization of {@link #minBy(java.util.Collection, java.lang.Comparable, util.functional.Functors.F1)}
     * with atMost parameter null - maximum value.
     *
     * @return optional with the minimum element or empty optional if collection contains no
     * element smaller than required.
     */
    public static <V,C extends Comparable<C>> Optional<V> minBy(Collection<V> c, Ƒ1<? super V,C> by) {
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
    public static <V,C extends Comparable<C>> Optional<V> minBy(Collection<V> c, C atMost, Ƒ1<? super V,C> by) {
        V minv = null;
        C minc = atMost;
        for(V v : c) {
            C vc = by.apply(v);
            if(minc==null || vc.compareTo(minc)<0) {
                minv = v;
                minc = vc;
            }
        }
        return Optional.ofNullable(minv);
    }
    /**
     * Specialization of {@link #maxBy(java.util.Collection, java.lang.Comparable, util.functional.Functors.F1)}
     * with atLeast parameter null - maximum value.
     *
     * @return optional with the maximal element or empty optional if collection contains no
     * element bigger than required.
     */
    public static <V,C extends Comparable<C>> Optional<V> maxBy(Collection<V> c, Ƒ1<? super V,C> by) {
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
    public static <V,C extends Comparable<C>> Optional<V> maxBy(Collection<V> c, C atleast, Ƒ1<? super V,C> by) {
        V maxv = null;
        C maxc = atleast;
        for(V v : c) {
            C vc = by.apply(v);
            if(maxc==null || vc.compareTo(maxc)>0) {
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
    public static <V> V min(V min, Comparator<V> cmp, V... c) {
        return min(Stream.of(c), min, cmp);
    }

    /**
     * Returns minimal element from the collection using given comparator.
     * Returns supplied value if it is the smallest, or collection is empty.
     */
    public static <V> V min(Collection<V> c, V min, Comparator<V> cmp) {
        return min(c.stream(), min, cmp);
    }

    /**
     * Returns minimal element from the stream using {@link Comparable#compareTo(java.lang.Object)}.
     * Returns supplied value if it is the smallest, or stream is empty.
     */
    public static <V extends Comparable<V>> V min(Stream<V> c, V min) {
        return max(c, min, Comparable::compareTo);
    }

    /**
     * Returns minimal element from the stream using given comparator.
     * Returns supplied value if it is the smallest, or stream is empty.
     */
    public static <V> V min(Stream<V> c, V min, Comparator<V> cmp) {
        return c.reduce(min, (t,u) -> cmp.compare(t, u)<0 ? t : u);
    }

    /**
     * Returns maximal element from the array using given comparator.
     * Returns supplied value if it is the smallest, or array is empty.
     */
    public static <V> V max(V max, Comparator<V> cmp, V... c) {
        return max(Stream.of(c), max, cmp);
    }

    /**
     * Returns maximal element from the collection using given comparator.
     * Returns supplied value if it is the smallest, or collection is empty.
     */
    public static <V> V max(Collection<V> c, V max, Comparator<V> cmp) {
        return max(c.stream(), max, cmp);
    }

    /**
     * Returns maximal element from the stream using given comparator.
     * Returns supplied value if it is the smallest, or stream is empty.
     */
    public static <V extends Comparable<V>> V max(Stream<V> c, V max) {
        return max(c, max, Comparable::compareTo);
    }

    /**
     * Returns maximal element from the stream using given comparator.
     * Returns supplied value if it is the smallest, or stream is empty.
     */
    public static <V> V max(Stream<V> c, V max, Comparator<V> cmp) {
        return c.reduce(max, (t,u) -> cmp.compare(t, u)>0 ? t : u);
    }

    public static <V> V get(Collection<V> c, Predicate<V> p) {
        return c.stream().filter(p).findFirst().get();
    }

    /**
     * Checks whether all elements of the list are equal by some property
     * obtained using specified transformation.
     * <p>
     * For example checking whether all lists have the same size:
     * <pre>{@code equalBy(lists,List::size) }</pre>
     *
     * @return true if transformation of each element in the list produces equal
     * result.
     */
    public static <V,R> boolean equalBy(List<V> o, Function<V,R> by) {
        if(o.size()<2) return true;
        R r = by.apply(o.get(0));
        for(int i=1; i<o.size(); i++)
            if(!r.equals(by.apply(o.get(i)))) return false;
        return true;
    }

    /**
     * Assuming a map of lists, i-slice is a list i-th elements in every list.
     * @return i-th slice of the map m
     */
    public static <K,T> Map<K,T> mapSlice(Map<K,List<T>> m, int i) {
        Map<K,T> o = new HashMap();
        m.entrySet().forEach(e -> o.put(e.getKey(), e.getValue().get(i)));
        return o;
    }

    /**
     * Creates map which remaps all elements to different key, using key mapper
     * function.
     * @return new map containing all elements mapped to transformed keys
     */
    public static <K1,K2,V> Map<K2,V> mapKeys(Map<K1,V> m, Function<K1,K2>f) {
        Map<K2,V> o = new HashMap();
        m.forEach((key,val) -> o.put(f.apply(key), val));
        return o;
    }

/************************************ for *************************************/

    /** Functional equivalent of a for loop. */
    public static<I> void forEach(List<I> items, Consumer<I> action) {
        for(I item : items)
            action.accept(item);
    }

    /**
     * Functional equivalent of a for loop. After each (last included) cycle the thread sleeps for
     * given period. If thread interrupts, the cycle ends immediately.
     * <p>
     * The cycle will also end if action in any cycle throws {@link InterruptedException}. Use it
     * instead of the {@code break} expression.
     *
     * @param period time for thread to wait after each loop. If negative or 0, no waiting occurs.
     * @param items items to execute action for, the order of execution will be the same as if using
     * normal for loop
     * @param action action that executes once per item. It can throw exception to signal loop break
     */
    public static <T> void forEachAfter(long period, Collection<T> items, ƑEC<T,InterruptedException> action) {
        for(T item : items) {
            try {
                action.apply(item);
                if(period>0) Thread.sleep(period);
            } catch (InterruptedException ex) {
                break;
            }
        }
    }

    /** Loops over both lists simultaneously. Must be of the same size. */
    public static<A,B> void forEachBoth(List<A> a, List<B> b, BiConsumer<A,B> action) {
        yes(a.size()==b.size());
        for(int i=0; i<a.size(); i++)
            action.accept(a.get(i), b.get(i));
    }

    /** Loops over both arrays simultaneously. Must be of the same size. */
    public static<A,B> void forEachBoth(A[] a, B[] b, BiConsumer<A,B> action) {
        yes(a.length==b.length);
        for(int i=0; i<a.length; i++)
            action.accept(a[i], b[i]);
    }

    /** Loops over list zipping index with each item. Index starts at 0. */
    public static<T> void forEachWithI(Collection<T> c, BiConsumer<Integer,T> action) {
        int i=0;
        for(T item : c) {
            action.accept(i, item);
            i++;
        }
    }

    /** Loops over array zipping index with each item. Index starts at 0. */
    public static<T> void forEachWithI(T[] c, BiConsumer<Integer,T> action) {
        int i=0;
        for(T item : c) {
            action.accept(i, item);
            i++;
        }
    }

    /** Loops over cartesian product C x C of a collection C. */
    public static <E> void forEachCartesian(Collection<E> c, BiConsumer<? super E, ? super E> action) {
        forEachCartesian(c, c, action);
    }

    /** Loops over cartesian product C x C of a collection C, ignoring symmetric elements (i,j) (j;i). */
    public static <E> void forEachCartesianHalf(Collection<E> c, BiConsumer<? super E, ? super E> action) {
//        for(int i=0; i<c.size(); i++)
//            for(int j=i; j<c.size(); j++)
//                action.accept(c.get(i), c.get(j));

//        int j = 0;
//        for(E e : c) {
//            c.stream().skip(j).forEach(t -> action.accept(e,t));
//            j++;
//        }

        int j = 1;
        for(E e : c) {
            int i = j;
            for(E t : c) {
                if(i>0)i--;
                if(i==0) action.accept(e,t);
            }
            j++;
        }
    }

    /**
     * Loops over cartesian product C x C of a collection C, ignoring symmetric elements (i,j)(j;i) and
     * self elements (i,i).
     */
    public static <E> void forEachCartesianHalfNoSelf(Collection<E> c, BiConsumer<? super E, ? super E> action) {
//        for(int i=0; i<c.size(); i++)
//            for(int j=i+1; j<c.size(); j++)
//                action.accept(c.get(i), c.get(j));

//        int j = 1;
//        for(E e : c) {
//            c.stream().skip(j).forEach(t -> action.accept(e,t));
//            j++;
//        }

        int j = 1;
        for(E e : c) {
            int i = j;
            for(E t : c) {
                if(i==0) action.accept(e,t);
                if(i>0)i--;
            }
            j++;
        }
    }

    /** Loops over cartesian product C1 x C2 of collections C1, C2.
     * @param action 1st parameter is element from 1st collection, 2nd parameter is el. from 2nd
     */
    public static <E,T> void forEachCartesian(Collection<E> c1, Collection<T> c2, BiConsumer<? super E, ? super T> action) {
        for(E e : c1) for(T t : c2) action.accept(e,t);
    }

    /** Loops over list zipping each item with a companion derived from it. */
    public static<T,W> void forEachWith(Collection<T> c, Function<T,W> toCompanion, BiConsumer<? super T, ? super W> action) {
        for(T t : c)
            action.accept(t, toCompanion.apply(t));
    }

    /**
     * Returns stream of elements mapped by the mapper from index-element pairs
     * of specified collection. Indexes start at 0.
     * <p>
     * Functionally equivalent to: List.stream().mapB(item->new Pair(item,list.indexOf(item))).mapB(pair->mapper.mapB(p))
     * but avoiding the notion of a Pair or Touple, and without any collection
     * traversal to get indexes.
     *
     * @param <T> element type
     * @param <R> result type
     * @param c
     * @param mapper
     * @return
     */
    public static<T,R> Stream<R> forEachIStream(Collection<T> c, BiFunction<Integer,T,R> mapper) {
        int i=0;
        Stream.Builder<R> b = Stream.builder();
        for(T item : c) {
            b.accept(mapper.apply(i, item));
            i++;
        }
        return b.build();
    }
    /** *  Equivalent to {@link #forEachIStream(java.util.Collection, java.util.function.BiFunction)}
    but iterates backwards (the index values are also reversed so last item
    has index value of 0).*/
    public static<T,R> Stream<R> forEachIRStream(List<T> c, BiFunction<Integer,T,R> mapper) {
        int size = c.size();
        Stream.Builder<R> b = Stream.builder();
        for(int i = 1; i<=size; i++)
            b.accept(mapper.apply(i-1, c.get(size-i)));
        return b.build();
    }

    /**
     * More general version of {@link #forEachIndexed(java.util.Collection, utilities.functional.functor.BiCallback)}.
     * The index can now be of any type and how it changes is defined by a parameter.
     * @param <I> key type
     * @param <T> element type
     * @param <R> result type
     * @param c
     * @param initial_val  for example: 0
     * @param operation for example: number -> number++
     * @param mapper maps the key-object pair into another object
     *
     * @return stream of mapped values by a mapper out of key-element pairs
     */
    public static<I,T,R> Stream<R> forEachIStream(Collection<T> c, I initial_val, Function<I,I> operation, BiFunction<I,T,R> mapper) {
        I i = initial_val;
        Stream.Builder<R> b = Stream.builder();
        for(T item : c) {
            b.accept(mapper.apply(i, item));
            i = operation.apply(i);
        }
        return b.build();
    }

/****************************** () -> collection ******************************/

    public static<T> Stream<Tuple2<Integer,T>> toIndexedStream(Collection<T> c) {
        int i=0;
        Stream.Builder<Tuple2<Integer,T>> b = Stream.builder();
        for(T item : c) {
            b.accept(tuple(i, item));
            i++;
        }
        return b.build();
    }

    public static<T> Set<T> set(T... ts) {
        Set<T> l = new HashSet<>(ts.length);
        for(T t : ts) l.add(t);
        return l;
    }

    /** Creates an array filled with provided elements. The array's length will equal element count. */
    public static <T> T[] array(T... elements) {
        return elements;
    }

    /** Returns modifiable list containing specified elements. */
    public static<T> List<T> list(T... ts) {
        List<T> l = new ArrayList<>(ts.length);
        for(T t : ts) l.add(t);
        return l;
    }
    /**
     * Returns unmodifiable list containing specified elements. Optimized:
     * <ul>
     * <li> if zero parameters - {@link Collections#EMPTY_LIST}
     * <li> if 1 parameters - {@link Collections#singletonList(java.lang.Object)}
     * <li> else parameters - {@link Arrays#asList(java.lang.Object...)}
     * </ul>
     */
    public static<T> List<T> listRO(T... ts) {
        int l = ts.length;
        if(l==0) return EMPTY_LIST;
        if(l==1) return singletonList(ts[0]);
        else return Arrays.asList(ts);
    }
    /** Returns modifiable list containing elements in the specified collection. */
    public static<T> List<T> list(Collection<T> a) {
        return new ArrayList<>(a);
    }
    /** Returns modifiable list containing elements in both specified collection and array. */
    public static<T> List<T> list(Collection<T> a, T... ts) {
        List<T> out = new ArrayList<>(a);
        for(int i=0; i<ts.length; i++) out.add(ts[i]);
        return out;
    }

    /** Returns modifiable list containing specified element i times. */
    public static<T> List<T> list(int i, T a) {
        List<T> l = new ArrayList<>(i);
        for(int j=0; j<i; j++) l.add(a);
        return l;
    }

    /** Returns modifiable list containing element supplied by specified supplier i times. */
    public static<T> List<T> list(int i, Supplier<T> factory) {
        List<T> l = new ArrayList<>(i);
        for(int j=0; j<i; j++) l.add(factory.get());
        return l;
    }

    /**
     * Returns modifiable list containing element supplied by specified supplier i times. Integer
     * params range from 1 to i;
     */
    public static<T> List<T> listF(int i, Function<Integer,T> factory) {
        List<T> l = new ArrayList<>(i);
        for(int j=0; j<i; j++) l.add(factory.apply(j));
        return l;
    }

    public static <T> Stream<T> stream(T... t) {
        return Stream.of(t);
    }

    public static <T,A extends T,B extends T> Stream<T> stream(Stream<A> s1, Stream<B> s2) {
        return Stream.concat(s1,s2);
    }

    public static <T> Stream<T> stream(T o, Stream<T> t) {
        return Stream.concat(Stream.of(o), t);
    }

    public static <T> Stream<T> stream(Collection<T> t) {
        return t.stream();
    }

    /** @return stream equivalent to a for loop */
    public static <T> Stream<T> stream(T seed, Predicate<T> cond, UnaryOperator<T> op) {
        Stream.Builder<T> b = Stream.builder();
        for(T t = seed; cond.test(t); t=op.apply(t)) b.accept(t);
        return b.build();
    }

    /** Creates stream of {@link Integer} in range from-to inclusive. */
    public static final Stream<Integer> range(int fromInclusive, int toInclusive) {
        Stream.Builder<Integer> b = Stream.builder();
        for(int i=fromInclusive; i<=toInclusive; i++) b.accept(i);
        return b.build();
    }

    public static final Stream<Double> range(double fromInclusive, double toInclusive) {
        Stream.Builder<Double> b = Stream.builder();
        for(double i=fromInclusive; i<=toInclusive; i++) b.accept(i);
        return b.build();
    }

    public static <A,B,R> Stream<R> streamBi(A[] a, B[] b, Ƒ2<A,B,R> zipper) {
        yes(a.length==b.length);
        Stream.Builder<R> builder = Stream.builder();
        for(int i=0; i<a.length; i++)
            builder.accept(zipper.apply(a[i], b[i]));
        return builder.build();
    }

/************************* collection -> collection ***************************/

    /** Filters array. Returns list. Source remains unchanged. */
    public static<T> List<T> filter(T[] a, Predicate<T> f) {
        return Stream.of(a).filter(f).collect(toList());
    }

    /** Filters collection. Returns list. Source remains unchanged. */
    public static<T> List<T> filter(Collection<T> c, Predicate<T> f) {
        return c.stream().filter(f).collect(toList());
    }

    /** Maps array. Returns list. Source remains unchanged. */
    public static<T,R> List<R> map(T[] a, Function<T,R> m) {
        return Stream.of(a).map(m).collect(toList());
    }

    /** Maps collection. Returns list. Source remains unchanged. */
    public static<T,R> List<R> map(Collection<T> c, Function<T,R> m) {
        return c.stream().map(m).collect(toList());
    }

    /** Filters and then maps array. Returns list. Source remains unchanged. */
    public static<T,R> List<R> filterMap(Predicate<T> f, Function<T,R> m, T... a ) {
        return Stream.of(a).filter(f).map(m).collect(toList());
    }

    /** Filters and then maps collection. Returns list. Source remains unchanged. */
    public static<T,R> List<R> filterMap(Collection<T> c, Predicate<T> f, Function<T,R> m) {
        return c.stream().filter(f).map(m).collect(toList());
    }

    /** Filters and then maps stream. Returns list. */
    public static<T,R> List<R> filterMap(Stream<T> c, Predicate<T> f, Function<T,R> m) {
        return c.filter(f).map(m).collect(toList());
    }

    /**
     * Alternative to {@link Stream#collect(java.util.stream.Collector)} and
     * {@link Collectors#groupingBy(java.util.function.Function) }, doesn't support
     * null keys (even for maps that allow them, and even if it has been specifically supplied for
     * the collector to use)!
     */
    public static <E,K> Map<K,List<E>> groupBy(Stream<E> s, Function<E,K> key_extractor) {
        Map<K,List<E>> m = new HashMap<>();
        s.forEach(e -> m.computeIfAbsent(key_extractor.apply(e), key -> new ArrayList<>()).add(e));
        return m;
    }

    /** Convenience method. */
    public static <K,V,E> Map<K,E> toMap(Function<V,K> key_extractor, Function<V,E> val_extractor, V... c) {
        return Stream.of(c).collect(Collectors.toMap(key_extractor, val_extractor));
    }

    /** Convenience method. */
    public static <K,V,E> Map<K,E> toMap(Collection<V> c, Function<V,K> key_extractor, Function<V,E> val_extractor) {
        return c.stream().collect(Collectors.toMap(key_extractor, val_extractor));
    }


    public static<T> List<T> split(String txt, String regex, int i, Function<String,T> m) {
        if(txt.isEmpty()) return EMPTY_LIST;
        return Stream.of(txt.split(regex, i)).map(m).collect(toList());
    }

    public static<T> List<T> split(String txt, String regex, Function<String,T> m) {
        if(txt.isEmpty()) return EMPTY_LIST;
        return Stream.of(txt.split(regex, -1)).map(m).collect(toList());
    }
    public static List<String> split(String txt, String regex, int i) {
        if(txt.isEmpty()) return EMPTY_LIST;
        return Stream.of(txt.split(regex, i)).collect(toList());
    }

    public static List<String> split(String txt, String regex) {
        if(txt.isEmpty()) return EMPTY_LIST;
        return Stream.of(txt.split(regex, -1)).collect(toList());
    }



    public static int findFirst(Map<Integer, ?> m, int from) {
        return IntStream.iterate(from, i -> i+1).filter(i->m.get(i)==null).findFirst().getAsInt();
    }

    public static int findFirst(IntPredicate condition, int from) {
        return IntStream.iterate(from, i -> i+1).filter(condition).findFirst().getAsInt();
    }
}
