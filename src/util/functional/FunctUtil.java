/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.functional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.util.Callback;

/**
 *
 * @author Plutonium_
 */
public class FunctUtil {
    
    /** Predicate returning true if object is null. */
    public static final Predicate<Object> isNotNULL = Objects::isNull;
    
    /** Predicate returning true if object is not null. */
    public static final Predicate<Object> isNULL = Objects::nonNull;
    
    /** Predicate returning true.*/
    public static final Predicate<Object> isTRUE = o -> true;
    
    /** Predicate returning false.*/
    public static final Predicate<Object> isFALSE = o -> false;
    
    /** Runnable that does nothing.  () -> {}; */
    public static final Runnable do_NOTHING = () -> {};
    
    /** Function transforming object into its string representation by invoking its toString() method*/
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
    
    
    /** Transforms Runnable into Runnable that executes on FxApplication Thread.
     *  Declared as r -> () -> Platform.runLater(r); */
    public static final Function<Runnable,Runnable> fxRunnableMapper = r -> () -> Platform.runLater(r);
    /** Function transforming executor into function transforming runnable into 
     * runnable that executes the runnable with provided executor */
    public static final Function<Consumer<Runnable>,Function<Runnable,Runnable>> executionWrapper = executor -> r -> () -> executor.accept(r);
    
    
    
    public static<T,E> List<T> mapToList(Collection<E> col, Function<E,T> mapper) {
        return col.stream().map(mapper).collect(Collectors.toList());
    }
    
    public static<T,E> List<T> mapToList(Stream<E> s, Function<E,T> mapper) {
        return s.map(mapper).collect(Collectors.toList());
    }
    
    public static<T,E> Set<T> mapToSet(Collection<E> col, Function<E,T> mapper) {
        return col.stream().map(mapper).collect(Collectors.toSet());
    }
    
    public static<T,E> Set<T> mapToSet(Stream<E> s, Function<E,T> mapper) {
        return s.map(mapper).collect(Collectors.toSet());
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
    
/************************** collection -> list<String> ************************/
    
    /**
     * Converts collection to string, joining string representations of the
     * elements by ', '
     * @param <T>
     * @param c collection
     * @param toStringConverter to map each element to string
     * @return String representation of the collection
     */
    public static<T> String toString(Collection<T> c, Function<T,String> toStringConverter) {
        return c.stream().map(toStringConverter).collect(toCSList);
    }
    
    /**
     * Equivalent to {@code toString(c, Object::toString)}.
     * @param <T>
     * @param c collection
     * @return String representation of the collection
     */
    public static<T> String toString(Collection<T> c) {
        return toString(c, Object::toString);
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
     * Functionally equivalent to: List.stream().map(item->new Pair(item,list.indexOf(item))).map(pair->mapper.map(p))
     * but avoiding the notion of a Pair or Touple, and without any collection
     * traversal to get indexes.
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
}
