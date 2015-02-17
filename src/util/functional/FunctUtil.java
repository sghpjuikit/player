/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional;

import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.iterate;
import java.util.stream.Stream;
import javafx.util.Callback;
import util.collections.Tuple2;
import static util.collections.Tuples.tuple;

/**
 *
 * @author Plutonium_
 */
public class FunctUtil {
    
    /** Predicate returning true if object is not null. */
    public static final Predicate<Object> isNotNULL = Objects::nonNull;
    
    /** Predicate returning true if object is null. */
    public static final Predicate<Object> isNULL = Objects::isNull;
    
    /** Predicate returning true.*/
    public static final Predicate isTRUE = o -> true;
    
    /** Predicate returning false.*/
    public static final Predicate isFALSE = o -> false;
    
    /** Runnable that does nothing.  () -> {}; */
    public static final Runnable do_NOTHING = () -> {};
    
    /** Function transforming object into its string representation by invoking its toString() method*/
    public static final Function<?,String> toString = Object::toString;
    
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
     * @param toComparableConverter E to Comparable mapper, derives Comparable from E.
     */
    public static<E> Comparator<E> cmpareBy(Callback<E,Comparable> toComparableConverter) {
        return (a,b) -> toComparableConverter.call(a).compareTo(toComparableConverter.call(b));
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
    public static<E> Comparator<E> cmpareNoCase(Callback<E,String> toStringConverter) {
        return (a,b) -> toStringConverter.call(a).compareToIgnoreCase(toStringConverter.call(b));
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
    
    public static <IN,OUT> Function<IN,OUT> mapC(Predicate<? super IN> cond, OUT y, OUT n) {
        return in -> cond.test(in) ? y : n;
    }
    public static <OUT> Function<Boolean,OUT> mapB(OUT y, OUT n) {
        return in -> in ? y : n;
    }
    public static <OUT> Function<? extends Object,OUT> mapNulls(OUT y) {
        return (Function) in -> in==null ? y : in;
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
     * @param c collection
     * @param m element to string mapper
     * @param s delimiter/separator
     * @return s separated representation of the collection
     */
    public static<T> String toS(Collection<T> c, Function<T,String> m, String s) {
        return c.stream().map(m).collect(joining(s));
    }
    
    /**
     * Converts collection to string, joining string representations of the
     * elements by ', '.
     * @param c collection
     * @param m element to string mapper
     * @return comma separated string representation of the collection
     */
    public static<T> String toS(Collection<T> c, Function<T,String> m) {
        return c.stream().map(m).collect(toCSList);
    }
    
    /**
     * Equivalent to {@code toString(c, Object::toString)}.
     * @param c collection
     * @return comma separated string representation of the collection
     */
    public static<T> String toS(Collection<T> c) {
        return toS(c, Object::toString);
    }
    
    public static<E> E findOrDie(Collection<E> c, Predicate<E> filter) {
        for(E i : c) if(filter.test(i)) return i;
        throw new RuntimeException("Collection does not have the element.");
    }
    public static<E> Optional<E> find(Collection<E> c, Predicate<E> filter) {
        for(E i : c) if(filter.test(i)) return Optional.of(i);
        return Optional.empty();
    }
    
/****************************** indexed forEach *******************************/
    
    /**
     * Functional alternative to for cycle for collections.
     * <p>
     * Equivalent to Collection.forEach(), with additional parameter - index of
     * the element in the collection.
     * <p>
     * Maps all elements of the collection into index-element pairs and executes
     * the action for each. Indexes start at 0.
     * 
     * @param <T> element type
     * @param c
     * @param action 
     */
    public static<T> void forEachIndexed(Collection<T> c, BiConsumer<Integer,T> action) {
        int i=0;
        for(T item : c) {
            action.accept(i, item);
            i++;
        }
    }
    
    /**
     * Returns stream of elements mapped by the mapper from index-element pairs 
     * of specified collection. Indexes start at 0.
     * <p>
 Functionally equivalent to: List.stream().mapB(item->new Pair(item,list.indexOf(item))).mapB(pair->mapper.mapB(p))
 but avoiding the notion of a Pair or Touple, and without any collection
 traversal to get indexes.
     * 
     * @param <T> element type
     * @param <R> result type
     * @param c
     * @param mapper
     * @return 
     */
    public static<T,R> Stream<R> forEachIndexedStream(Collection<T> c, BiFunction<Integer,T,R> mapper) {
        int i=0;
        Stream.Builder<R> b = Stream.builder();
        for(T item : c) {
            b.accept(mapper.apply(i, item));
            i++;
        }
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
    public static<I,T,R> Stream<R> forEachIndexedStream(Collection<T> c, I initial_val, Function<I,I> operation, BiFunction<I,T,R> mapper) {
        I i = initial_val;
        Stream.Builder<R> b = Stream.builder();
        for(T item : c) {
            b.accept(mapper.apply(i, item));
            i = operation.apply(i);
        }
        return b.build();
    }
    
/****************************** -> collection *********************************/
    
    public static<T> Stream<Tuple2<Integer,T>> toIndexedStream(Collection<T> c) {
        int i=0;
        Stream.Builder<Tuple2<Integer,T>> b = Stream.builder();
        for(T item : c) {
            b.accept(tuple(i, item));
            i++;
        }
        return b.build();
    }
    
    public static<T> List<T> list(T... a) {
        return Arrays.asList(a);
    }
    
    
    public static<T> List<T> list(T[] a, Predicate<T> f) {
        return Stream.of(a).filter(f).collect(toList());
    }
    
    public static<T> List<T> list(Collection<T> c, Predicate<T> f) {
        return c.stream().filter(f).collect(toList());
    }
    
    public static<T,R> List<R> listM(T[] a, Function<T,R> m) {
        return Stream.of(a).map(m).collect(toList());
    }
    
    public static<T,R> List<R> listM(Collection<T> c, Function<T,R> m) {
        return c.stream().map(m).collect(toList());
    }
    
    
    public static<T,R> List<R> list(T[] a, Predicate<T> f, Function<T,R> m) {
        return Stream.of(a).filter(f).map(m).collect(toList());
    }
    
    public static<T,R> List<R> list(Collection<T> c, Predicate<T> f, Function<T,R> m) {
        return c.stream().filter(f).map(m).collect(toList());
    }
    
    
    public static<T> List<T> split(String txt, String regex, int i, Function<String,T> factory) {
        if(txt.isEmpty()) return EMPTY_LIST;
        return Stream.of(txt.split(regex, i)).map(factory).collect(toList());
    }
    
    public static<T> List<T> split(String txt, String regex, Function<String,T> factory) {
        if(txt.isEmpty()) return EMPTY_LIST;
        return Stream.of(txt.split(regex, -1)).map(factory).collect(toList());
    }
    
    
    
    public static int findFirstEmpty(Map<Integer, ?> m, int from) {
        return iterate(from, i -> i+1).filter(i->m.get(i)==null).findFirst().getAsInt();
    }
}
