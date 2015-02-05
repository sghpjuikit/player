/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import util.dev.TODO;
import static util.dev.TODO.Purpose.UNIMPLEMENTED;
import static util.dev.TODO.Severity.SEVERE;

/**
 * {@link Set} backed by {@link HashMap} using provided key mapper for identity 
 * check instead of equals() and hashCode().
 *
 * @author Plutonium_
 */
@TODO(purpose = UNIMPLEMENTED, severity = SEVERE, note = "Also, implement Map instead?")
public class MapSet<K,E> implements Set<E> {

    public final Function<E,K> keyMapper;
    private final Map<K,E> m = new HashMap();

    public MapSet(Function<E,K> keyMapper) {
        this.keyMapper = keyMapper;
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return m.containsKey(keyMapper.apply((E)o));
    }

    @Override
    public Iterator<E> iterator() {
        return m.values().iterator();
    }

    @Override
    public Object[] toArray() {
        return m.values().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return m.values().toArray(a);   // not sure...
    }

    @Override
    public boolean add(E e) {
        if(m.containsKey(keyMapper.apply(e))) return false;
        m.put(keyMapper.apply(e), e);
        return true;
    }
    
    public E get(K key) {
        return m.get(key);
    }

    @Override
    public boolean remove(Object o) {
        K k = keyMapper.apply((E)o);
        E e = m.get(k);
        if(e==null) return false;
        m.remove(k);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object e : c)
            if (!contains(e))
                return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void clear() {
        m.clear();
    }
    
    public Stream<Entry<K,E>> streamK() {
        return m.entrySet().stream();
    }
    
}