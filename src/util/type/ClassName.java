/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.type;

import util.collections.map.ClassMap;

/**
 *
 * @author Martin Polakovic
 */
public class ClassName {

    private final ClassMap<String> names = new ClassMap<>();
    private final ClassMap<String> cache = new ClassMap<>();

    /**
     * Registers name for specified class and all its subclasses that dont
     * have any name registered.
     */
    public void add(Class<?> c, String name) {
        names.put(c, name);
    }

    /**
     * Returns name of the class. If there is no registered name, names of
     * superclasses are looked up. If this fails, the name is derived from the
     * class.
     * <p/>
     * Name computation is in order:
     * <ul>
     * <li> registered name of class
     * <li> first registered name of superclass in inheritance order
     * <li> first registered name of interface (no order)
     * <li> {@link Class#getSimpleName()}
     * <li> {@link Class#toString()}
     * </ul>
     * This is computed lazily (when requested) and cached (every name
     * will be computed only once), with access O(1).
     *
     * @return computed name of the class. Never null or empty string.
     */
    public String get(Class<?> c) {
        cache.computeIfAbsent(c, names::getElementOfSuperV);
        return cache.computeIfAbsent(c, ClassName::of);
    }

    public String getOf(Object instance) {
        return get(instance==null ? Void.class : instance.getClass());
    }


    public static String of(Class<?> c) {
        String n = c.getSimpleName();
        return n.isEmpty() ? c.toString() : n;
    }
}
