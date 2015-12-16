/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.collections.mapset;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import util.dev.TODO;

import static util.dev.TODO.Purpose.UNIMPLEMENTED;
import static util.dev.TODO.Severity.SEVERE;

/**
 * {@link Set} backed by {@link Map} using provided key mapper function for
 * identity check instead of equals() and hashCode().
 * <p>
 * The underlying map hashing the elements by key is {@link HashMap} by default,
 * but this is not forced. If desired, pass an arbitrary map into a constructor.
 * <p>
 * Similarly to other sets, this can be used to easily filter duplicates from
 * collection, but leveraging arbitrary element identity.
 * Use like this: {@code new MapSet<K,E>(e -> e.identity(), elements) }
 *
 * @see #keyMapper
 *
 * @author Plutonium_
 */
public class MapSet<K,E> implements Set<E> {

/****************************************** FACTORIES *********************************************/

    public static <E> MapSet<Integer,E> mapHashSet() {
        return new MapSet<>(Object::hashCode);
    }

    public static <E> MapSet<Integer,E> mapHashSet(Map<Integer,E> backing_map) {
        return new MapSet<>(backing_map, Object::hashCode);
    }

    public static <E> MapSet<Integer,E> mapHashSet(Map<Integer,E> backing_map, Collection<E> c) {
        return new MapSet<>(backing_map, Object::hashCode, c);
    }

    public static <E> MapSet<Integer,E> mapHashSet(Map<Integer,E> backing_map, E... c) {
        return new MapSet<>(backing_map, Object::hashCode, c);
    }

/**************************************************************************************************/

    /**
     * Function transforming element to its key. Used for all collection
     * operations, e.g add(), addAll(), remove(), etc. Two elements are mapped
     * to the same key if this function produces the same key for them.
     * <p>
     * Note, that if the function returned a constant, this set would become
     * singleton set. Using hashCode() would result in same mapping strategy
     * as default maps and collections use.
     */
    public final Function<E,K> keyMapper;
    private final Map<K,E> m;

    public MapSet(Function<E,K> keyMapper) {
        this(new HashMap<>(),keyMapper);
    }

    public MapSet(Function<E,K> keyMapper, Collection<E> c) {
        this(new HashMap<>(),keyMapper,c);
    }

    public MapSet(Function<E,K> keyMapper, E... c) {
        this(new HashMap<>(),keyMapper,c);
    }

    public MapSet(Map<K,E> backing_map, Function<E,K> keyMapper) {
        this.m = backing_map;
        this.keyMapper = keyMapper;
    }

    public MapSet(Map<K,E> backing_map, Function<E,K> keyMapper, Collection<E> c) {
        this.m = backing_map;
        this.keyMapper = keyMapper;
        addAll(c);
    }

    public MapSet(Map<K,E> backing_map, Function<E,K> keyMapper, E... c) {
        this.m = backing_map;
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

    public boolean containsKey(K k) {
        return m.containsKey(k);
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

    public E getOr(K key, E e) {
        return m.containsKey(key) ? m.get(key) : e;
    }

    public E getOrSupply(K key, Supplier<E> supplier) {
        return m.containsKey(key) ? m.get(key) : supplier.get();
    }

    public E getOrOther(K key, K key2) {
        return m.containsKey(key) ? m.get(key) : get(key2);
    }

    @Override
    public boolean remove(Object o) {
        return removeKey(keyMapper.apply((E)o));
    }

    public boolean removeValue(E o) {
        return remove(o);
    }

    public boolean removeKey(K key) {
        E e = m.get(key);
        if(e==null) return false;
        m.remove(key);
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

    @TODO(purpose = UNIMPLEMENTED, severity = SEVERE)
    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @TODO(purpose = UNIMPLEMENTED, severity = SEVERE)
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

    public void ifHasK(K k, Consumer<E> action) {
        E e = m.get(k);
        if(e != null)
            action.accept(e);
    }

    public void ifHasE(E element, Consumer<E> action) {
        E e = m.get(keyMapper.apply(element));
        if(e != null)
            action.accept(e);
    }

    public E computeIfAbsent(K key, Function<? super K, ? extends E> mappingFunction) {
        E v = m.get(key);
        if (v == null) {
            E nv = mappingFunction.apply(key);
            if (nv!= null) {
                m.put(key, nv);
                return nv;
            }
        }
        return v;
    }

    public E computeIfAbsent(K key, Supplier<? extends E> supplier) {
        E v = m.get(key);
        if (v == null) {
            E nv = supplier.get();
            if (nv!= null) {
                m.put(key, nv);
                return nv;
            }
        }
        return v;
    }

}