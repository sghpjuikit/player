/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import util.TODO;
import static util.TODO.Purpose.API;
import static util.TODO.Severity.LOW;

/**
 * {@link HashMap} with key mapper, which transforms elements into keys. The map
 * provides additional methods to act more like a list or collection.
 *
 * @author Plutonium_
 */
@TODO(note = "implement Set", purpose = API, severity = LOW)
public class KeyMap<K,E> extends HashMap<K,E> {
    private static final long serialVersionUID = 192L;
    public final Function<E,K> keyMapper;
    
    public KeyMap(Function<E,K> keyMapper) {
        this.keyMapper = keyMapper;
    }
    
    public void addE(E e) {
        put(keyMapper.apply(e), e);
    }
    
    public void addAllE(E... a) {
        for(E e : a) addE(e);
    }
    
    public void addAllE(Collection<E> c) {
        c.forEach(this::addE);
    }
    
    public void removeE(E e) {
        remove(keyMapper.apply(e));
    }
    
    public void removeEif(Predicate<E> p) {
        values().forEach(e->{
            if(p.test(e))
                removeE(e);
        });
    }
    
    public void containsE(E e) {
        containsKey(keyMapper.apply(e));
    }
    
    public Stream<E> stream() {
        return values().stream();
    }
    
    public Stream<Entry<K,E>> streamK() {
        return entrySet().stream();
    }
}
