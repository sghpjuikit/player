/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;
import util.dev.TODO;
import static util.dev.TODO.Purpose.UNIMPLEMENTED;
import static util.dev.TODO.Severity.SEVERE;

/**
 * {@link Set} backed by {@link HashMap} using provided key mapper for identity 
 * check instead of equals() and hashCode().
 * <p>
 * Similarly to other sets, this can be used to easily filter doubles from
 * collection, but leveraging arbitrary element identity.
 * Use like this: {@code new MapSet<K,E>(e->e.identity(), elements) }
 *
 * @see #keyMapper
 *
 * @author Plutonium_
 */
@TODO(purpose = UNIMPLEMENTED, severity = SEVERE)
public class MapSet<K,E> implements Set<E> {

    /** Function transforming element to its identity. Used for all collection
    operations, e.g add(), addAll(), remove(), etc. 
    <p>
    Note, that just as with bad equals()/hashCode() implementation where two
    objects with different state are considered equal, two 'different' objects
    can map to same identity here as well. If the function returned a constant,
    this set would become singleton set. Using hashCode() would result in
    standard HashSet implementation.
    */
    public final Function<E,K> keyMapper;
    private final Map<K,E> m = new HashMap();

    public MapSet(Function<E,K> keyMapper) {
        this.keyMapper = keyMapper;
    }
    public MapSet(Function<E,K> keyMapper, Collection<E> c) {
        this.keyMapper = keyMapper;
        addAll(c);
    }
    public MapSet(Function<E,K> keyMapper, E... c) {
        this.keyMapper = keyMapper;
        addAll(c);
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

    /** Adds item to this keymap if not yet contained in this mapset.
    Element identity is obtained using {@link #keyMapper} */
    @Override
    public boolean add(E e) {
        K k = keyMapper.apply(e);
        if(m.containsKey(k)) return false;
        m.put(k, e);
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
    
    /** Adds all not yet contained items to this mapset.
    Element identity is obtained using {@link #keyMapper} */
    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }
    
    /** Array version of {@link #addAll(java.util.Collection)}*/
    public boolean addAll(E... c) {
        boolean modified = false;
        for (E e : c)
            if (add(e))
                modified = true;
        return modified;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void clear() {
        m.clear();
    }
    
    /** @return this.entrySet().stream() */
    public Stream<Entry<K,E>> streamE() {
        return m.entrySet().stream();
    }
    /** @return this.keySet().stream() */
    public Stream<K> streamK() {
        return m.keySet().stream();
    }
    /** @return this.values().stream() */
    public Stream<E> streamV() {
        return m.values().stream();
    }
    
}