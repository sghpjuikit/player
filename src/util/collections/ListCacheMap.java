/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import util.functional.functor.FunctionC;

/**
More specific cache map using {@link ArrayList} as a cache bucket/accumulation
container.
<p>
Cache factory returns new list and accumulator adds element to the list, key
mapper remains unimplemented.
<p>
Defines a 

 @author Plutonium_
 */
public class ListCacheMap<E,K> extends CacheMap<E,K,List<E>> {

    public ListCacheMap(FunctionC<E,K> keyMapper) {
        super(keyMapper, () -> new ArrayList(), (e,cache) -> cache.add(e));
    }
    
    
    /** Multi key get returning the combined content of the cache buckets.
    @return list containing all elements of all cache buckets / accumulation
    containers assigned to keys in the given collection. */
    public List<E> getElementsOf(Collection<K> keys) {
        List<E> out = new ArrayList();
        for(K k : keys) {
            List<E> c = get(k);
            if(c!=null) out.addAll(c);
        }
        return out;
    }
    /** Array version of {@link #getElementsOf(java.util.Collection)}. */
    public List<E> getElementsOf(K... keys) {
        List<E> out = new ArrayList();
        for(K k : keys) {
            List<E> c = get(k);
            if(c!=null) out.addAll(c);
        }
        return out;
    }
    
    /** Stream version of {@link #getElementsOf(java.util.Collection)}. 
    Use to avoid creating intermediary collection of keys (parameter) and reduce memory.*/
    public List<E> getElementsOf(Stream<K> keys) {
        return keys.map(this::get).filter(c -> c!=null)
                   .flatMap(c->c.stream()).collect(toList());
    }
}
