/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static util.functional.Util.map;


/**
 * Histogram of H for elements E by their property P (key).
 * <p/>
 * For example histogram of numbers
 *
 * @param <K> key
 * @param <E> element. Typically a collection element.
 * @param <H> histogram element. Denotes the statistics for element. Mostly
 * a value object - like Pair or Tuple of certain arity.
 */
public final class Histogram<K,E,H> extends HashMap<K,H> {
    private static final long serialVersionUID = 190L;
    public static final Object ALL = new All();

    /** Creates new empty histogram element.
      * Used when histogram element for given element does not exist yet. */
    public Supplier<H> histogramFactory;
    /** Updates histogram element by accumulating the element.
      * Used during accumulation phase, for each element. */
    public BiConsumer<H,E> elementAccumulator;
    /** Maps elements into histogram element key.
      * Decides for element which histogram element will be accumulated. */
    public Function<E,K> keyMapper;

    private H h_all;

    /** Accumulates given element into this histogram.
      * a histogram for the element as if a singleton collection. */
    public void accumulate(E e) {
        K k = keyMapper.apply(e);
        H h = computeIfAbsent(k, key -> histogramFactory.get());
        elementAccumulator.accept(h, e);
        if(h_all==null) h_all = histogramFactory.get();
        elementAccumulator.accept(h_all, e);
    }

    /** Accumulates all elements in the collection into this histogram. Creates
      * a histogram for the collection. */
    public void accumulate(Collection<E> c) {
        c.forEach(this::accumulate);
    }

    @Override
    public void clear() {
        super.clear();
        h_all = null;
    }

    /** Maps the histogram into list of results, using the provided mapper. */
    public<R> List<R> toList(BiFunction<K,H,R> resultMapper) {
        return map(entrySet(), e -> resultMapper.apply(e.getKey(), e.getValue()));
    }

    /**
     * Same as {@link #toList(java.util.function.BiFunction)}, but there is additional record
     * inserted in the beginning of the list - accumulation of all elements (while each record
     * represents accumulation of the elements mapped to single key).
     * <p/>
     * This item can be recognized as the function will receive {@link #ALL} as a key input
     * instead of an actual key. Because this special key is of different type, the function's
     * input is broadened to Object.
     */
    public<R> List<R> toListAll(BiFunction<Object,H,R> resultMapper) {
        List<R> l = new ArrayList<>(size()+1);
        l.add(resultMapper.apply(ALL,h_all));
        entrySet().forEach(e -> l.add(resultMapper.apply(e.getKey(),e.getValue())));
        return l;
    }


    public static class All implements Comparable<Object> {

        @Override
        public int compareTo(Object o) {
            return -1;
        }

    }
}