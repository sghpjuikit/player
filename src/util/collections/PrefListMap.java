/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.function.Function;

/**
 *
 * @author Plutonium_
 */
public class PrefListMap<E,K> extends ListMap<E,K> {

    public PrefListMap(Function<E,K> keyMapper) {
        super(PrefList::new,keyMapper);
    }
    
    public void accumulate(E e, boolean pref) {
        // get key
        K k = keyMapper.apply(e);
        // get cache storage with key & build new if not yet built 
        PrefList c = (PrefList) get(k);
        if(c==null) {
            c = (PrefList)cacheFactory.get();
            put(k, c);
        }
        c.addPreferred(e, pref);
    }

//    @Override
//    public PrefList<E> get(Object key) {
//        List<E> l = super.get(key);
//        return l==null ? new PrefList() : (PrefList) super.get(key);
//    }
    
}
