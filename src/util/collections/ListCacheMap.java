/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
    
    
    /** Multi key get with elements.
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
}
