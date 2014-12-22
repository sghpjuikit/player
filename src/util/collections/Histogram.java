/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import static util.functional.FunctUtil.mapStoList;

/**
 * Histogram of H for elements E by their property P (key).
 * <p>
 * For example histogram of numbers
 * 
 * @param <K> key
 * @param <E> element. Typically same as type of collection element.
 * @param <H> histogram element. Denotes the statistics for element. Mostly
 * a value object - like Pair or Tuple of certain arity.
 * 
 * @author Plutonium_
 */
public final class Histogram<K,E,H> extends HashMap<K,H>{
    private static final long serialVersionUID = 190L;
    
    /** Creates new empty histogram element. 
      * Used when histogram element for given element does not exist yet. */
    public Supplier<H> histogramFactory;
    /** Updates histogram element by accumulating the element. 
      * Used during accumulation phase, for each element. */
    public BiConsumer<H,E> elementAccumulator;
    /** Maps elements into histogram element key. 
      * Decides for element which histogram element will be accumulated. */
    public Function<E,K> keyMapper;    
    
    /** Accumulates given element into this histogram. 
      * a histogram for the element as if a singleton collection. */
    public void accumulate(E e) {
        // obtain histogram
        K k = keyMapper.apply(e);
        H h = get(k);
        // supply new histogram if not yet available
        if(h==null) {
            h = histogramFactory.get();
            put(k,h);
        }
        elementAccumulator.accept(h, e);
    }
    
    /** Accumulates all elements in the collection into this histogram. Creates
      * a histogram for the collection. */
    public void accumulate(Collection<E> c) {
        c.forEach(this::accumulate);
    }
    
    /** Maps the histogram into list of results, using the provided mapper. */
    public<R> List<R> getResult(BiFunction<K,H,R> resultMapper) {
        return mapStoList(entrySet().stream(), e -> resultMapper.apply(e.getKey(), e.getValue()));
    }
}
