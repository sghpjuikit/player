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
import java.util.function.Supplier;
import util.functional.functor.FunctionC;

/**
 Map for caching and splitting collections. It splits elements E to cache buckets 
 C based on key K derived from elements E

 @param <E> element that will be split/accumulated
 @param <K> key extracted from element E to hash cache buckets on.
 @param <C> cache bucket/accumulation container. A collection such as List, but 
 can be a single object
 even element E itself. For example a sum. Depends on accumulation strategy.
 <p>
 @author Plutonium_
 */
public class CacheMap<E,K,C> extends HashMap<K,C> {
    /** Extracts keys from elements. Determines the splitting parts of the caching 
    strategy, e.g. using a predicate would split the original collection on
    two parts - elements that test true, and elements that test false.*/
    public FunctionC<E,K> keyMapper;
    /** Builds cache bucket/accumulation container when there is none for the
    given key during accumulation.*/
    public Supplier<C> cacheFactory;
    /** Defines how the elements will be accumulated into the cache bucket.
    If the bucket is a collection, you probably wish to use 
    {@code (element, collection) -> collection.add(element);} but different 
    reducing strategies can be used, for example {@code (element,sum) -> sum+number; }*/
    public BiConsumer<E,C> cacheAccumulator;

    public CacheMap(FunctionC<E,K> keyMapper, Supplier<C> cacheFactory, BiConsumer<E,C> cacheAccumulator) {
        this.keyMapper = keyMapper;
        this.cacheFactory = cacheFactory;
        this.cacheAccumulator = cacheAccumulator;
    }
    
    /** Accumulates given collection into this cache map. The collection remains
    ineffected. */
    public void accumulate(Collection<E> es) {
        for(E e : es) {
            // get key
            K k = keyMapper.apply(e);
            // get cache storage with key & build new if not yet built 
            C c = get(k);
            if(c==null) {
                c = cacheFactory.get();
                put(k, c);
            }
            // cache element
            cacheAccumulator.accept(e, c);
        }
    }
    
    /** Multi key get.
    @return list containing of all cache buckets / accumulation
    containers assigned to keys in the given collection. */
    public List<C> getCacheOf(Collection<K> keys) {
        List<C> out = new ArrayList();
        for(K k : keys) {
            C c = get(k);
            if(c!=null) out.add(c);
        }
        return out;
    }

    @Override
    public void clear() {
        super.clear();
    }
}
