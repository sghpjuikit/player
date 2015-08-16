/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util;

import java.util.Objects;

import util.collections.map.ClassMap;
import util.functional.Functors.Ƒ1;

/**
 *
 * @author Plutonium_
 */
public class InstanceName {

    private static final Ƒ1<? extends Object,String> def = Objects::toString;
    private final ClassMap<Ƒ1<?,String>> m = new ClassMap<>();


    /**
     * Registers name function for specified class and all its subclasses that 
     * dont have any name registered.
     *
     * @param <T>
     * @param c
     * @param parser 
     */
    public <T> void add(Class<T> c, Ƒ1<? super T,String> parser) {
        m.put(c, parser);
    }

    /**
     * Returns name/string representation of the object instance. If none is
     * provided, {@link Objects#toString(java.lang.Object)} is used.
     * 
     * @param instance Object to get name of. Can be null, in which case its
     * treated as of type {@link Void}.
     * @return computed name of the object instance. Never null.
     */
    public String get(Object instance) {
        // we handle null as void so user can register his own function
        Class c = instance==null ? Void.class : instance.getClass();
        Ƒ1<?,String> f = m.getElementOfSuper(c);
        // fall back to general implementation
        // note that it must be able to handle null (which it does)
        if(f==null) f = def;

        return ((Ƒ1<Object,String>) f).apply(instance);
    }

}