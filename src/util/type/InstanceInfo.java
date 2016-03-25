
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;

import static java.util.Collections.EMPTY_MAP;

/**
 *
 * @author Plutonium_
 */
public class InstanceInfo {

    private static final Ƒ1<? extends Object,Map<String,String>> DEF = o -> EMPTY_MAP;
    private final ClassMap<Ƒ1<?,Map<String,String>>> names = new ClassMap<>();


    /**
     * Registers name function for specified class and all its subclasses that 
     * dont have any name registered.
     * <p>
     * Use {@link Void} class to handle null (since only null can be an 'instance' of Void).
     *
     * @param <T>
     * @param c
     * @param extractor
     */
    public <T> void add(Class<T> c, Ƒ1<? super T,Map<String,String>> extractor) {
        names.put(c, extractor);
    }

    /**
     * Convenience method. Alternative to {@link #add(Class, util.functional.Functors.Ƒ1)} which passes already
     * created map to the extractor.
     *
     * @param c
     * @param extractor
     * @param <T>
     */
    public <T> void add(Class<T> c, BiConsumer<? super T,Map<String,String>> extractor) {
        names.put(c, (T o) -> {
            Map<String,String> m = new HashMap<>();
            extractor.accept(o, m);
            return m;
        });
    }

    /**
     * Returns name/string representation of the object instance. If none is
     * provided, {@link Objects#toString(java.lang.Object)} is used.
     * 
     * @param instance Object to get name of. Can be null, in which case its
     * treated as of type {@link Void}.
     * @return computed name of the object instance. Never null.
     */
    public Map<String,String> get(Object instance) {
        // we handle null as void so user can register his own function
        Class c = instance==null ? Void.class : instance.getClass();
        Ƒ1<?,Map<String,String>> f = names.getElementOfSuper(c);
        // fall back to general implementation (must be able to handle null)
        if(f==null) f = DEF;

        return ((Ƒ1<Object,Map<String,String>>) f).apply(instance);
    }

}