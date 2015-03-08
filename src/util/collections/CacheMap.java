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
 <p>
 @author Plutonium_
 */
public class CacheMap<E,K,C> extends HashMap<K,C> {
    public FunctionC<E,K> keyMapper;
    public Supplier<C> cacheFactory;
    public BiConsumer<E,C> cacheAccumulator;

    public CacheMap(FunctionC<E,K> keyMapper, Supplier<C> cacheFactory, BiConsumer<E,C> cacheAccumulator) {
        this.keyMapper = keyMapper;
        this.cacheFactory = cacheFactory;
        this.cacheAccumulator = cacheAccumulator;
    }
    
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
